package com.stellariver.pistachio.core;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handle {

    boolean async() default true;

    Class<? extends MessageMapping<? extends Message, String>> messageMapping() default MessageMapping.PlaceHolder.class;

    Class<? extends DomainObjectMapping<?, ?>> domainObjectMapping() default DomainObjectMapping.PlaceHolder.class;

}
