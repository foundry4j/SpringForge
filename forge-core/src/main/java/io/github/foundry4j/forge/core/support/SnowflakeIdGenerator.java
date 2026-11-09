package io.github.foundry4j.forge.core.support;

import java.time.Instant;

/// Thread safe Snowflake identifier generator.
///
/// The 64 bit value is composed of a 41 bit millisecond timestamp, a 10 bit worker
/// identifier and a 12 bit sequence, which allows 4096 identifiers per worker per
/// millisecond:
///
/// ```text
/// 0 | 41 bit timestamp | 10 bit worker | 12 bit sequence
/// ```
///
/// Deployments with more than one instance must give every instance its own worker
/// identifier, usually from the `forge.data.worker-id` property.
public final class SnowflakeIdGenerator {

    /// Default epoch, 2025-01-01T00:00:00Z, which is usable for roughly 69 years.
    public static final long DEFAULT_EPOCH = 1735689600000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private final long epoch;

    private long sequence;
    private long lastTimestamp = -1L;

    /// Creates a generator with the default epoch.
    public SnowflakeIdGenerator(long workerId) {
        this(workerId, DEFAULT_EPOCH);
    }

    /// Creates a generator with an explicit epoch.
    public SnowflakeIdGenerator(long workerId, long epoch) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId must be in [0, %d], got %d".formatted(MAX_WORKER_ID, workerId));
        }
        this.workerId = workerId;
        this.epoch = epoch;
    }

    /// Generates the next identifier.
    ///
    /// @throws IllegalStateException when the system clock moved backwards
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards, refusing to generate an id");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - epoch) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    /// Extracts the timestamp encoded in an identifier.
    public Instant timestampOf(long id) {
        return Instant.ofEpochMilli((id >>> TIMESTAMP_SHIFT) + epoch);
    }

    /// Extracts the worker identifier encoded in an identifier.
    public long workerIdOf(long id) {
        return (id >>> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    private static long waitNextMillis(long last) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= last) {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
