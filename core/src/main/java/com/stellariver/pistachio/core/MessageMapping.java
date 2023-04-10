package com.stellariver.pistachio.core;

public interface MessageMapping<T, ID>{

    ID map(T t);

    class PlaceHolder implements MessageMapping<Message<?>, String> {

        @Override
        public String map(Message<?> message) {
            throw new RuntimeException("this class is only a place holder, would not be used");
        }
    }

}
