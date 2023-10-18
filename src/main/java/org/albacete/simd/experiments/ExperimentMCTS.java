package org.albacete.simd.experiments;

import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Dag_n;
import org.albacete.simd.mctsbn.MCTSBN;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentMCTS {

    private String algName;
    private String netName;
    private String netPath;
    private String databasePath;
    private String databaseName;
    private int iterationLimit;
    private double exploitConstant;
    private double numberSwaps;
    private double probabilitySwaps;
    private int selectionConstant;
    private String savePath;

    private File saveFile;

    private Problem problem;
    private MCTSBN algorithm;

    private double totalTime;

    private Dag_n resultDag;


    public ExperimentMCTS(String[] parameters, String savePath){
        this.algName = parameters[0];
        this.netName = parameters[1];
        this.netPath = parameters[2];
        this.databasePath = parameters[3];
        this.databaseName = Utils.getDatabaseNameFromPattern(databasePath);
        this.problem = new Problem(databasePath);
        this.iterationLimit = Integer.parseInt(parameters[4]);
        this.exploitConstant = Double.parseDouble(parameters[5]);
        this.numberSwaps = Double.parseDouble(parameters[6]);
        this.probabilitySwaps = Double.parseDouble(parameters[7]);
        this.selectionConstant = Integer.parseInt(parameters[8]);
        //this.algorithm = new MCTSBN(problem, iterationLimit);
        this.savePath = savePath;
        this.algorithm = new MCTSBN(problem, iterationLimit, netName, databaseName, Runtime.getRuntime().availableProcessors(), exploitConstant, numberSwaps, probabilitySwaps, selectionConstant);
    }


    private void setUpExperiment() {
        MCTSBN.EXPLOITATION_CONSTANT = exploitConstant;
        MCTSBN.NUMBER_SWAPS = numberSwaps;
        MCTSBN.PROBABILITY_SWAP = probabilitySwaps;
    }

    public Dag_n runExperiment(){
        saveFile = new File(savePath);
        this.setUpExperiment();
        double start = System.currentTimeMillis();
        resultDag = this.algorithm.search();
        this.totalTime = (System.currentTimeMillis() - start) / 1000.0;
        return resultDag;
    }

    public Problem getProblem(){
        return problem;
    }

    public Dag_n getPGESDag(){
        return this.algorithm.getPGESDag();
    }

    public double getPGESTime(){
        return this.algorithm.pgesTime;
    }

    public double getTotalTime(){
        return totalTime;
    }

    public String getSavePath(){
        return this.savePath;
    }

    public double getTimeSpentSavingRoundsSeconds(){
        return this.algorithm.getTimeSpentSavingRoundsSeconds();
    }



}
