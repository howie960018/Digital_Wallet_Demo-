package com.wallet.digitalwallet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SnowflakeIdGenerator 單元測試")
class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("生成 ID：應回傳正數的 Long 值")
    void nextId_returnsPositiveLong() {
        // Arrange
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

        // Act
        long id = generator.nextId();

        // Assert
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("生成 ID：連續生成 100 個 ID 應全部唯一")
    void nextId_generates100UniqueIds() {
        // Arrange
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();

        // Act
        for (int i = 0; i < 100; i++) {
            ids.add(generator.nextId());
        }

        // Assert
        assertThat(ids).hasSize(100);
    }

    @Test
    @DisplayName("生成 ID：連續生成的 ID 應為遞增趨勢")
    void nextId_isMonotonicallyIncreasing() {
        // Arrange
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

        // Act
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        // Assert
        assertThat(id2).isGreaterThanOrEqualTo(id1);
        assertThat(id3).isGreaterThanOrEqualTo(id2);
    }

    @Test
    @DisplayName("建構子：workerId 超出範圍應拋出 IllegalArgumentException")
    void constructor_invalidWorkerId_throwsException() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Worker ID 超出範圍");
    }

    @Test
    @DisplayName("建構子：datacenterId 超出範圍應拋出 IllegalArgumentException")
    void constructor_invalidDatacenterId_throwsException() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, 32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Datacenter ID 超出範圍");
    }

    @Test
    @DisplayName("建構子：負數 workerId 應拋出 IllegalArgumentException")
    void constructor_negativeWorkerId_throwsException() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Worker ID 超出範圍");
    }

    @Test
    @DisplayName("多執行緒：並發生成 1000 個 ID 應全部唯一")
    void nextId_concurrent_generatesUniqueIds() throws InterruptedException {
        // Arrange
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        int threadCount = 10;
        int idsPerThread = 100;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    ids.add(generator.nextId());
                }
            });
        }

        // Act
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // Assert
        assertThat(ids).hasSize(threadCount * idsPerThread);
    }
}

