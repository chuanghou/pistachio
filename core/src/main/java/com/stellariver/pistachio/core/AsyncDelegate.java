package com.stellariver.pistachio.core;

import com.lmax.disruptor.dsl.Disruptor;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AsyncDelegate<T extends DomainObject<T>> {

}


