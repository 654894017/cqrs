package com.damon.cqrs.utils;

import java.time.ZonedDateTime;

public class DateUtils {

    public static long getSecond(ZonedDateTime first, ZonedDateTime second) {
        long secondMinutes = second.toInstant().getEpochSecond();
        long firstMinutes = first.toInstant().getEpochSecond();
        return secondMinutes - firstMinutes;
    }
}
