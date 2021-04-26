package com.damon.cqrs.utils;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
/**
 * 
 * @author xianping_lu
 *
 */
public class InternerUtils {

    private static Interner<Long> interner = Interners.<Long>newWeakInterner();

    public static Long intern(long id) {
        return interner.intern(id);
    }

}
