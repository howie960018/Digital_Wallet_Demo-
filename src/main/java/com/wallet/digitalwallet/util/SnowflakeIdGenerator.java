package com.wallet.digitalwallet.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    // 起始時間戳（2025-01-01 00:00:00 UTC）
    private final long epoch = 1735689600000L;

    // 各部分的位元數
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long sequenceBits = 12L;

    // 最大值
    private final long maxWorkerId = ~(-1L << workerIdBits);         // 31
    private final long maxDatacenterId = ~(-1L << datacenterIdBits); // 31

    // 位移量
    private final long workerIdShift = sequenceBits;                          // 12
    private final long datacenterIdShift = sequenceBits + workerIdBits;       // 17
    private final long timestampShift = sequenceBits + workerIdBits + datacenterIdBits; // 22

    // 序列號遮罩（4095）
    private final long sequenceMask = ~(-1L << sequenceBits);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID 超出範圍");
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID 超出範圍");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 時鐘回撥檢查
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("時鐘回撥，拒絕生成 ID");
        }

        // 同一毫秒內，序列號遞增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 序列號溢出（同一毫秒超過 4096 個），等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列號歸零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}