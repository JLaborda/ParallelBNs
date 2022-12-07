package org.albacete.simd.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.Edge;

public class EdgeSearch implements Comparable<EdgeSearch> {
        public double score;
        public SubSet hSubset;
        public Edge edge;

        public EdgeSearch (double score, SubSet hSubSet, Edge edge) {
            this.score = score;
            this.hSubset = hSubSet;
            this.edge = edge;
        }

        @Override
        public int compareTo(EdgeSearch o){
            return Double.compare(this.score, (o).score);
        }

        public double getScore(){
            return this.score;
        }

        public SubSet gethSubset(){
            return this.hSubset;
        }

        public Edge getEdge(){
            return this.edge;
        }
    }