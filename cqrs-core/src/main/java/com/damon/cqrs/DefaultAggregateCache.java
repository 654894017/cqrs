package com.damon.cqrs;

import java.util.concurrent.TimeUnit;

import com.domain.cqrs.domain.Aggregate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class DefaultAggregateCache implements IAggregateCache {

    private final Cache<Long, Aggregate> aggregateCache;
    /**
     * 
     * @param cacheMaximumSize 最多能够缓存多少聚合个数
     * @param expireTime  有效时间（分钟）
     */
    public DefaultAggregateCache(int cacheMaximumSize, int expireTime) {
        /**
         * 聚合缓存
         */
        aggregateCache = CacheBuilder.newBuilder().maximumSize(cacheMaximumSize).expireAfterAccess(expireTime, TimeUnit.MINUTES).build();
    }

    @Override
    public void updateAggregateCache(long id, Aggregate aggregate) {
        aggregateCache.put(id, aggregate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Aggregate> T get(long id) {
        return (T) aggregateCache.getIfPresent(id);
    }

}
