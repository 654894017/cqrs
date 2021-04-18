package com.damon.cqrs.utils;

public class ReflectUtis {

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> classes) {
        try {
            return (T) classes.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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
