package com.damon.cqrs.utils;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * @author xianping_lu
 */
public class InternerUtils {

    private static final Interner<Long> interner = Interners.newWeakInterner();

    public static Long intern(long id) {
        return interner.intern(id);
    }

}
