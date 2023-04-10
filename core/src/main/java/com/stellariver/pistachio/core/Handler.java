package com.stellariver.pistachio.core;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Method;
import java.util.function.BiPredicate;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Handler {

    boolean async;

    Class<? extends DomainObject<?>> domainObjectClass;

    Class<? extends Message<?>> messageClass;

    Method method;

    MessageMapping<? extends Message<?>, String> messageMapping;

    public void handle(DomainObject<?> domainObject, Message<?> message) {

    }

}
