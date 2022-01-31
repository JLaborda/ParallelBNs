package org.albacete.simd.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;


public class LocalScoreCacheConcurrent {
    private ConcurrentHashMap<DualKey<Integer, Set<Integer>>, Double> map = new ConcurrentHashMap<>();

    public LocalScoreCacheConcurrent() {
    }

    public void add(int variable, int[] parents, double score) {
        Set<Integer> _parents = new HashSet<>(parents.length);
        //int[] parents_aux = parents;

        for(int i = 0; i < parents.length; ++i) {
            int parent = parents[i];
            _parents.add(parent);
        }

        DualKey<Integer, Set<Integer>> key = new DualKey<>(variable, _parents);

        this.map.put(key, score);
    }

    public double get(int variable, int[] parents) {
        Set<Integer> _parents = new HashSet<Integer>(parents.length);
        int[] var7 = parents;
        int var6 = parents.length;

        for(int i = 0; i < parents.length; ++i) {
            int parent = parents[i];
            _parents.add(parent);
        }
        DualKey<Integer, Set<Integer>> key = new DualKey<>(variable, _parents);


        Double _score = this.map.get(key);
        return _score == null ? Double.NaN : _score;
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

    private class DualKey<K1,K2> {
        private final K1 key1;
        private final K2 key2;
    
        public DualKey(K1 key1, K2 key2){
            this.key1 = key1;
            this.key2 = key2;
        }
    
        public K1 getKey1(){
            return key1;
        }
    
        public K2 getKey2(){
            return key2;
        }
    
        @Override
        public boolean equals(Object other){
            if (other instanceof DualKey<?,?>){
                DualKey<?,?> obj = (DualKey<?,?>)other;
                if ( obj.getKey1().equals(this.key1) && obj.getKey2().equals(this.key2)){
                    return true;
                }
            }
            return false;
        }
    
        @Override
        public String toString() {
            return "(" + key1.toString() + ", " + key2.toString() + ")";
        }
    
        @Override
        public int hashCode() {
            //return super.hashCode();
            return Objects.hash(key1, key2);
        }
    }
}
