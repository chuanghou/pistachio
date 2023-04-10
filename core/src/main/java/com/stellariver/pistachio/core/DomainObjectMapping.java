package com.stellariver.pistachio.core;

public interface DomainObjectMapping<T, ID> {

    ID map(T t);

    class PlaceHolder implements DomainObjectMapping<Object, String> {

        @Override
        public String map(Object o) {
            throw new RuntimeException("this class is only a place holder, would not be used");
        }

    }

}
