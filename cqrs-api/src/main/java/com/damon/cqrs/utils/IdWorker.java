package com.damon.cqrs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

enum SystemClock {

    // ====

    INSTANCE(1);

    private final long period;
    private final AtomicLong nowTime;
    private boolean started = false;
    private ScheduledExecutorService executorService;

    SystemClock(long period) {
        this.period = period;
        this.nowTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * The initialize scheduled executor service
     */
    public void initialize() {
        if (started) {
            return;
        }

        this.executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "system-clock");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> nowTime.set(System.currentTimeMillis()),
                this.period, this.period, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
        started = true;
    }

    /**
     * The get current time milliseconds
     *
     * @return long time
     */
    public long currentTimeMillis() {
        return started ? nowTime.get() : System.currentTimeMillis();
    }

    /**
     * The get string current time
     *
     * @return string time
     */
    public String currentTime() {
        return new Timestamp(currentTimeMillis()).toString();
    }

    /**
     * The destroy of executor service
     */
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}

/**
 * <p>
 * ??????GUID????????????(sequence),??????Snowflake??????64?????????ID????????? <br>
 * ?????????????????? http://git.oschina.net/yu120/sequence
 * </p>
 *
 * @author hubin
 * @Date 2016-08-01
 */
public class IdWorker {

    /**
     * ???????????????????????????
     */
    private static final Sequence worker = new Sequence();

    public static long getId() {
        return worker.nextId();
    }

    /**
     * <p>
     * ????????????"-" UUID
     * </p>
     */
    public static synchronized String get32UUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}

class Sequence {

    private static final Logger log = LoggerFactory.getLogger(Sequence.class);
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static volatile InetAddress LOCAL_ADDRESS = null;
    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private final long twepoch = 1519740777809L;
    /**
     * 5????????????id
     */
    private final long datacenterIdBits = 5L;
    protected final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    /**
     * 5????????????id
     */
    private final long workerIdBits = 5L;
    protected final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    /**
     * ?????????????????????id???: 2???12?????????
     */
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /**
     * ?????????????????????
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    /**
     * ????????????id
     */
    private final long datacenterId;
    /**
     * ????????????id
     */
    private final long workerId;
    /**
     * ??????????????????
     */
    private long sequence = 0L;
    /**
     * ???????????? ID ?????????
     */
    private long lastTimestamp = -1L;

    public Sequence() {
        this.datacenterId = getDatacenterId();
        this.workerId = getMaxWorkerId(datacenterId);
    }

    /**
     * ???????????????
     *
     * @param workerId     ???????????? ID
     * @param datacenterId ?????????
     */
    public Sequence(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * Find first valid IP from local network card
     *
     * @return first valid local IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }

        LOCAL_ADDRESS = getLocalAddress0();
        return LOCAL_ADDRESS;
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    return address;
                                }
                            } catch (Throwable e) {
                                log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }

        log.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }

        String name = address.getHostAddress();
        return (name != null && !"0.0.0.0".equals(name) && !"127.0.0.1".equals(name) && IP_PATTERN.matcher(name).matches());
    }

    /**
     * ????????????MAC????????????????????????????????????
     * <p>
     * ???????????????
     */
    protected long getDatacenterId() {
        long id = 0L;
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(getLocalAddress());
            if (null == network) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            log.warn(" getDatacenterId: " + e.getMessage());
        }

        return id;
    }

    /**
     * ?????? MAC + PID ??? hashcode ??????16?????????
     * <p>
     * ???????????????
     */
    protected long getMaxWorkerId(long datacenterId) {
        StringBuilder mpId = new StringBuilder();
        mpId.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && name.length() > 0) {
            // GET jvmPid
            mpId.append(name.split("@")[0]);
        }

        // MAC + PID ??? hashcode ??????16?????????
        return (mpId.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * ??????????????? ID
     *
     * @return next id
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        // ??????
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    // ????????????????????????????????????????????????
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            // ?????????????????????????????????
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // ??????????????????????????????????????????
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // ????????????????????????????????? 1 - 3 ?????????
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        // ??????????????? | ?????????????????? | ?????????????????? | ???????????????
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }

        return timestamp;
    }

    protected long timeGen() {
        return SystemClock.INSTANCE.currentTimeMillis();
    }

}