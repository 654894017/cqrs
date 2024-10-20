package com.damon.cqrs.utils;

import java.lang.reflect.InvocationTargetException;

public class ReflectUtils {

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> classes, Long aggregateId) {
        try {
            return (T) classes.getDeclaredConstructor(Long.class).newInstance(aggregateId);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }

    }
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(String classTypeName) {
        try {
            return (Class<T>) Class.forName(classTypeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
