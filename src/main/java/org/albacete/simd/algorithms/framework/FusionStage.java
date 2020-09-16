package org.albacete.simd.algorithms.framework;

import edu.cmu.tetrad.graph.Dag;

public abstract class FusionStage extends Stage{

    @Override
    public Dag run() {
        return null;
    }

    public abstract Dag fusion();
}
