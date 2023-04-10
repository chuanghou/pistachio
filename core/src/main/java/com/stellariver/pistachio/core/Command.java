package com.stellariver.pistachio.core;

public abstract class Command<T> extends Message<T>{

    String domainObjectId;

    @Override
    public String map(T t) {
        return domainObjectId;
    }

}
