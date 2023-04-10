package com.stellariver.pistachio.core;


import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.stellariver.milky.common.tool.exception.SysEx;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.stellariver.milky.common.tool.exception.ErrorEnumsBase.CONFIG_ERROR;

public class MessageBus {

    public static Map<Class<?>, Object> singletonMap = new HashMap<>();

    private static final Predicate<Method> HANDLER_FORMAT =
            method -> Modifier.isPublic(method.getModifiers())
                    && (!Modifier.isStatic(method.getModifiers()))
                    && method.getParameterTypes().length == 1
                    && Message.class.isAssignableFrom(method.getParameterTypes()[0]);

    private final Multimap<Class<DomainObject<?>>, Handler> domainObjectClassHandlers;
    private final Multimap<Class<? extends Message<?>>, Transformation> subscriptions = MultimapBuilder.hashKeys().arrayListValues().build();

    private final Map<Class<? extends DomainObject<?>>, ConcurrentHashMap<String, ? extends DomainObject<?>>> container = new HashMap<>();
    private final Map<Handler, ConcurrentHashMap<String, String>> handlerMappings = new HashMap<>();

    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static private class Transformation {
        MessageMapping<Message<?>, String> mapping;
        List<Handler> handlers;
    }

    @SneakyThrows
    @SuppressWarnings("rawtypes, unchecked")
    public MessageBus(String[] packages) {

        ConfigurationBuilder configuration = new ConfigurationBuilder().forPackages(packages).addScanners(new SubTypesScanner());
        Reflections reflections = new Reflections(configuration);
        Set<Class<? extends DomainObject>> domainObjectClasses = reflections.getSubTypesOf(DomainObject.class);

        List<Method> methods = domainObjectClasses.stream().flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(Handle.class))
                .peek(m -> SysEx.falseThrow(HANDLER_FORMAT.test(m), CONFIG_ERROR.message(m.toGenericString() + " signature not valid!")))
                .collect(Collectors.toList());

        List<Handler> handlers = new ArrayList<>();

        for (Method method : methods) {
            Class<? extends DomainObject<?>> domainObjectClass = (Class<? extends DomainObject<?>>) method.getDeclaringClass();
            Handle annotation = method.getAnnotation(Handle.class);
            Class<? extends Message<?>> messageClass = (Class<? extends Message<?>>) method.getParameterTypes()[0];

            Class<? extends MessageMapping<? extends Message, String>> messageMappingClazz = annotation.messageMapping();
            messageMappingClazz = messageMappingClazz == MessageMapping.PlaceHolder.class ?
                    (Class<? extends MessageMapping<? extends Message, String>>) messageClass.getDeclaringClass() : messageMappingClazz;

            Class<? extends Message<?>> messageClazz = (Class<? extends Message<?>>) Arrays.stream(messageMappingClazz.getGenericInterfaces())
                    .map(i -> (ParameterizedType) i).filter(t -> Objects.equals(t.getRawType(), MessageMapping.class))
                    .map(ParameterizedType::getActualTypeArguments).findFirst().orElseThrow(() -> new SysEx(CONFIG_ERROR))[0];

            SysEx.trueThrow(messageClass != messageClazz, CONFIG_ERROR);

            MessageMapping<? extends Message<?>, String> messageMapping = (MessageMapping<? extends Message<?>, String>) singletonMap.get(messageMappingClazz);
            if (messageMapping == null) {
                messageMapping = (MessageMapping<? extends Message<?>, String>) messageMappingClazz.newInstance();
                singletonMap.put(messageMappingClazz, messageMapping);
            }

            Handler handler = Handler.builder().async(annotation.async())
                    .domainObjectClass(domainObjectClass)
                    .messageClass(messageClass)
                    .method(method)
                    .messageMapping(messageMapping)
                    .build();

            handlers.add(handler);

        }

        domainObjectClassHandlers = handlers.stream().collect(Collect.multiMap(h -> (Class<DomainObject<?>>) h.getDomainObjectClass(), true));

        handlers.stream().collect(Collectors.groupingBy(Handler::getMessageClass)).forEach((messageClass, groupHandlers) -> {
            List<Transformation> transformations = groupHandlers.stream()
                    .collect(Collectors.groupingBy(Handler::getMessageMapping)).entrySet().stream()
                    .map(e -> new Transformation((MessageMapping<Message<?>, String>) e.getKey(), e.getValue())).collect(Collectors.toList());
            subscriptions.putAll(messageClass, transformations);
        });
    }


    static private final ConcurrentHashMap<String, ? extends DomainObject<?>> emptyMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        subscriptions.get((Class<? extends Message<?>>) event.getClass()).forEach(t -> {
            String mappedMessageId = t.mapping.map(event);

            for (Handler handler : t.handlers) {
                Map<String, String> idMapping = handlerMappings.get(handler);
                if (idMapping == null) {
                    continue;
                }
                Object domainObjectId = idMapping.get(mappedMessageId);
                Map<String, ? extends DomainObject<?>> subMap = container.getOrDefault(handler.getDomainObjectClass(), emptyMap);
                DomainObject<?> domainObject = subMap.get(domainObjectId);
                SysEx.nullThrow(domainObject);
                handler.handle(domainObject, event);
            }
        });
    }

    @SneakyThrows
    public static void createDomainObject(Class<? extends DomainObject<?>> domainObjectClass) {
        DomainObject<?> domainObject = domainObjectClass.newInstance();
        domainObject.init();
    }

}




