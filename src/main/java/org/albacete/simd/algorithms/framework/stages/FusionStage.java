package org.albacete.simd.algorithms.framework.stages;

import edu.cmu.tetrad.graph.Dag;
import org.albacete.simd.algorithms.framework.stages.Stage;

public abstract class FusionStage extends Stage {

    @Override
    public Dag run() {
        return null;
    }

    public abstract Dag fusion();
}
