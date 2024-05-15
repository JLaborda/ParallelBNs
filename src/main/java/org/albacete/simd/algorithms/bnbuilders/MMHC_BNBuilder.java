package org.albacete.simd.algorithms.bnbuilders;

import org.albacete.simd.framework.BNBuilder;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.IndTestChiSquare;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.mb.Mmhc;

public class MMHC_BNBuilder extends BNBuilder{

    Mmhc algorithm;

    public MMHC_BNBuilder(String path ) {
        super(path, 1, -1, -1);
        IndependenceTest test = new IndTestChiSquare(super.problem.getData(), 0.05);
        algorithm = new Mmhc(test, super.problem.getData());
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
    public Graph search(){
        this.currentGraph = algorithm.search();
        return this.currentGraph;
    }

    
    
}
