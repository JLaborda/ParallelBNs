package org.albacete.simd.algorithms.pGESv2;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;


public class DualKeyTest {

    @Test
    public void constructorAndGetterTest(){
        DualKey<String, String> d1 = new DualKey<>("Hello", "World");
        DualKey<Integer, String> d2 = new DualKey<>(1, "Ipsum");

        assertEquals("Hello", d1.getKey1());
        assertEquals("World", d1.getKey2());

        assertEquals(1, d2.getKey1().intValue());
        assertEquals("Ipsum", d2.getKey2());
    }

    @Test
    public void equalsTest(){
        DualKey<String, String> d1 = new DualKey<>("Hello", "World");
        DualKey<String, String> d2 = new DualKey<>("Hello", "World");
        DualKey<String, String> d3 = new DualKey<>("Hello", "World2");
        DualKey<String, String> d4 = new DualKey<>("Hello2", "World");
        DualKey<String, String> d5 = new DualKey<>("Hello2", "World2");
        DualKey<Integer, String> d6 = new DualKey<>(1, "World");
        Object d7 = "Hello World";

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, d4);
        assertNotEquals(d1, d5);
        assertNotEquals(d1, d6);
        assertNotEquals(d1, d7);
    }

    @Test
    public void toStringTest(){
        DualKey<String, String> d1 = new DualKey<>("Hello", "World");
        assertEquals("(Hello, World)", d1.toString());
    }

    @Test
    public void hashTest(){

        Set<Integer> s1 = new HashSet<>();
        Set<Integer> s2 = new HashSet<>();
        Set<Integer> s3 = new HashSet<>();

        s1.add(1);
        s2.add(1);
        s3.add(2);


        DualKey<Integer, Set<Integer>> k1 = new DualKey<>(1, s1);
        DualKey<Integer, Set<Integer>> k2 = new DualKey<>(1, s2);
        DualKey<Integer, Set<Integer>> k3 = new DualKey<>(1, s3);
        // Checking that k1 and k2 are equal and give back the same hashcode
        assertEquals(k1,k2);
        assertEquals(k1.hashCode(), k2.hashCode());
        // Checking that k1, k3 and k2, k3 are not equal and don't return the same hashcode
        // (although this is not fully necessary)
        assertNotEquals(k1, k3);
        assertNotEquals(k2, k3);
        assertNotEquals(k1.hashCode(), k3.hashCode());
        assertNotEquals(k2.hashCode(), k3.hashCode());
    }

    @Test
    public void concurrentDualKeyHashMapTest() throws InterruptedException {
        ConcurrentHashMap<DualKey<Integer, Set<Integer>>, Double> map = new ConcurrentHashMap<>();
        DualKey<Integer, Set<Integer>> key1 = new DualKey<>(1, new HashSet<>());
        map.put(key1, 1.0);

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                DualKey<Integer, Set<Integer>> key1 = new DualKey<>(1, new HashSet<>());
                double value = map.get(key1);
                System.out.println("Value: " + value);
                map.put(key1, value + 1);
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                DualKey<Integer, Set<Integer>> key1 = new DualKey<>(1, new HashSet<>());
                double value = map.get(key1);
                System.out.println("Value: " + value);
                map.put(key1, value + 1);
            }
        });

        thread1.start();
        thread1.join();
        thread2.start();
        thread2.join();

        System.out.println("Map: " + map);
        assertEquals(3.0, map.get(key1), 0.000001);

        /*ExecutorService executorService =
                Executors.newFixedThreadPool(4);
        for (int j = 0; j < 10; j++) {
            executorService.execute(() -> {
                for (int k = 0; k < 10; k++){
                    map.put(new DualKey<Integer, Set<Integer>>(k, new HashSet<>()), 3.2 * k);
                }

            });
         */

    }


}
