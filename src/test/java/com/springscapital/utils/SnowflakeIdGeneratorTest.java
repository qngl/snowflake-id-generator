package com.springscapital.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;

public class SnowflakeIdGeneratorTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSnowflakeIdGenerator() {
        SnowflakeIdGenerator generator1 = new SnowflakeIdGenerator();
        SnowflakeIdGenerator generator2 = new SnowflakeIdGenerator();
        assertEquals(generator1.toString(), generator2.toString());
        System.out.println(generator1.toString());
    }

    @Test
    public void testSnowflakeIdGeneratorLongLong() {
        SnowflakeIdGenerator generator1 = new SnowflakeIdGenerator(1L, 1L);
        SnowflakeIdGenerator generator2 = new SnowflakeIdGenerator(2L, 1L);
        assertNotEquals(generator1.toString(), generator2.toString());
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < 10000; i++) {
            ids.add(generator1.nextId());
        }
        for (int i = 0; i < 10000; i++) {
            ids.add(generator2.nextId());
        }
        assertEquals(20000, ids.size());
    }

    @Test
    public void testNextId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        assertNotEquals(id1, id2);
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < 10000; i++) {
            ids.add(generator.nextId());
        }
        assertEquals(10000, ids.size());
    }

    @Test
    public void testConcurrentNextId() throws InterruptedException {
        final ConcurrentMap<Long, Long> ids = new ConcurrentHashMap<Long, Long>();
        final SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        final CheckConcurrentGenerating check = new CheckConcurrentGenerating();
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    long id = generator.nextId();
                    ids.put(id, id);
                }
                check.check(ids);
            }
        });
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    long id = generator.nextId();
                    ids.put(id, id);
                }
                check.check(ids);
            }
        });
        t2.start();
        t1.start();
    }

    static class CheckConcurrentGenerating {
        private int count = 0;

        void check(ConcurrentMap<Long, Long> ids) {
            if (++count == 2) {
                assertEquals(20000, ids.size());
            }
        }
    }
}
