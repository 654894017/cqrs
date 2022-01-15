package com.damon.cqrs.utils;

import org.springframework.cglib.beans.BeanCopier;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BeanCopierUtils {
    private static final ConcurrentMap<String, BeanCopier> bcCache = new ConcurrentHashMap<>();
    private static final String keySplitor = "->";

    public static <S, D> void copy(S srcObject, D destObject) {
        BeanCopier bc = getBeanCopier(srcObject.getClass(), destObject.getClass());
        bc.copy(srcObject, destObject, null);
    }

    private static BeanCopier getBeanCopier(Class<?> srcClass, Class<?> destClass) {
        String key = generateKey(srcClass, destClass);
        BeanCopier bc = bcCache.computeIfAbsent(key, k -> {
            return BeanCopier.create(srcClass, destClass, false);
        });
        return bc;
    }

    private static String generateKey(Class<?> srcClass, Class<?> destClass) {
        StringBuilder sb = new StringBuilder();
        String srcClassName = srcClass.getName();
        String destClassName = destClass.getName();
        sb.append(srcClassName).append(keySplitor).append(destClassName);
        return sb.toString();
    }
}