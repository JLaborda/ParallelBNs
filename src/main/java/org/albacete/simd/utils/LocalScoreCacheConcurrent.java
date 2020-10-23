package org.albacete.simd.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalScoreCacheConcurrent {
    private ConcurrentHashMap<DualKey<Integer, Set<Integer>>, Double> map = new ConcurrentHashMap<>();

    public LocalScoreCacheConcurrent() {
    }

    public void add(int variable, int[] parents, double score) {
        Set<Integer> _parents = new HashSet(parents.length);
        int[] var9 = parents;
        int var8 = parents.length;

        for(int var7 = 0; var7 < var8; ++var7) {
            int parent = var9[var7];
            _parents.add(parent);
        }

        DualKey<Integer, Set<Integer>> key = new DualKey<>(variable, _parents);

        this.map.put(key, score);
    }

    public double get(int variable, int[] parents) {
        Set<Integer> _parents = new HashSet(parents.length);
        int[] var7 = parents;
        int var6 = parents.length;

        for(int var5 = 0; var5 < var6; ++var5) {
            int parent = var7[var5];
            _parents.add(parent);
        }
        DualKey<Integer, Set<Integer>> key = new DualKey<>(variable, _parents);


        Double _score = this.map.get(key);
        return _score == null ? 0.0D / 0.0 : _score;
    }

    public void clear() {
        this.map.clear();
    }

    @Override
    public String toString() {
        return "LocalScoreCacheConcurrent{" +
                "map=" + map +
                '}';
    }
}
