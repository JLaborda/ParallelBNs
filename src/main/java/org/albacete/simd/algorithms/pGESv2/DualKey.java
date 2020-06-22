package org.albacete.simd.algorithms.pGESv2;

import java.util.Objects;

public class DualKey<K1,K2> {
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
