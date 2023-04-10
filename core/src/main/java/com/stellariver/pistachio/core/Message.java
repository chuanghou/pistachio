package com.stellariver.pistachio.core;

import lombok.Data;

@Data
public abstract class Message<T> implements MessageMapping<T, String>{

    String id;

    @Override
    public String map(T t) {
        return id;
    }

}
