package com.springscapital.utils;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Twitter Snowflake ID Generator<br>
 * ID is a long number, with time sequentially and the following structure:<br>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
 * 1 bit for +/- in long<br>
 * 41 bits for time offset from current to the start in millisecond<br>
 * 10 bits for code of machine, 5 bits as datacenterId and 5 bits as workerId<br>
 * 12 bits for sequence with 1 millisecond (<=4096)<br>
 */
public class SnowflakeIdGenerator implements IdGenerator {

    /**
     * Start Time(2012-09-10)
     */
    private static final long TWEPOCH = 1347206400000L;

    /**
     * Worker ID bits length
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * Data center ID bits length
     */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /**
     * The max available workerId
     */
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);

    /**
     * The max available datacenterId
     */
    private static final long MAX_DATA_CENTER_ID = -1L ^ (-1L << DATA_CENTER_ID_BITS);

    /**
     * Sequence bits length
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * workId shift bits length
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * dataCenterId shift bits length
     */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * timestamp offset shift bits length
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /**
     * final sequence mask: 4095 (0b111111111111=0xfff=4095)
     */
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    /**
     * (0~31)
     */
    private final long workerId;

    /**
     * (0~31)
     */
    private final long datacenterId;

    /**
     * (0~4095)
     */
    private long sequence = 0L;

    /**
     * timestamp used for last ID
     */
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        long nodeId = this.generateNodeIdWithMacAddress();
        this.workerId = nodeId & MAX_WORKER_ID;
        this.datacenterId = nodeId >> WORKER_ID_BITS & MAX_DATA_CENTER_ID;
    }

    /**
     * @param workerId (0~31)
     * @param datacenterId (0~31)
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATA_CENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * Generate an ID
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            // concurrent call within 1 millisecond
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // all 4095 IDs used up
                // Wait for next millisecond
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // generate the ID with  predefined bits offset
        return ((timestamp - TWEPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * @param lastTimestamp timestamp used to generate last ID
     * @return nextTimestamp to be used
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private long generateNodeIdWithMacAddress() {
        long nodeId;
        try {
            StringBuilder sb = new StringBuilder("SnowflakeIdGenerator");
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for(byte macPort: mac) {
                        sb.append(String.format("%02X", macPort));
                    }
                }
            }
            nodeId = sb.toString().hashCode();
        } catch (Exception ex) {
            nodeId = (new SecureRandom().nextInt());
        }
        return nodeId;
    }

    @Override
    public String toString() {
        return "SnowflakeIdGenerator [workerId=" + workerId + ", datacenterId=" + datacenterId + "]";
    }

}
