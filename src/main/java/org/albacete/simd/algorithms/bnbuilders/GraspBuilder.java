package org.albacete.simd.algorithms.bnbuilders;

import org.albacete.simd.framework.BNBuilder;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.Grasp;

public class GraspBuilder extends BNBuilder{

    edu.cmu.tetrad.search.Grasp grasp;

    public GraspBuilder(String path, int nThreads, int maxIterations, int nItInterleaving) {
        super(path, nThreads, maxIterations, nItInterleaving);
        //TODO Auto-generated constructor stub
    }

    @Override
    protected boolean convergence() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'convergence'");
    }

    @Override
    protected void initialConfig() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'initialConfig'");
    }

    @Override
    protected void repartition() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repartition'");
    }

    @Override
    protected void forwardStage() throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'forwardStage'");
    }

    @Override
    protected void forwardFusion() throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'forwardFusion'");
    }

    @Override
    protected void backwardStage() throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'backwardStage'");
    }

    @Override
    protected void backwardFusion() throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'backwardFusion'");
    }

    @Override
    public Graph search() {
        // TODO Auto-generated method stub
        grasp = new Grasp(problem.getBDeu());
        return null;
    }
    
}
