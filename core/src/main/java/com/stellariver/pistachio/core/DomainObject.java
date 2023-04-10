package com.stellariver.pistachio.core;


import lombok.Getter;

public abstract class DomainObject<T> implements DomainObjectMapping<T, String>{

    @Getter
    private String id;


    /**
     * When domain object is handled asynchronously, domain object has group id
     * the domain objects sharing same groupId will be handled in the same disruptor and thread
     * default: groupId equals to domain object id, then every group has only one member
     * @return the group id of this domain object belongs
     */
    protected String groupId() {
        return id;
    }


    protected void init() {}

    protected void terminate() {}

    protected PersistenceConfig persistenceConfig() {
        return null;
    }

}
