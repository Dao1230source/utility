package org.source.utility.exceptions;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ExceptionConstructorCache {

    @SuppressWarnings("rawtypes")
    static final Map<Class<? extends EnumProcessor>, EnumProcessor.ExceptionConstructor<?>> CONSTRUCTOR_MAP = HashMap.newHashMap(8);
}
