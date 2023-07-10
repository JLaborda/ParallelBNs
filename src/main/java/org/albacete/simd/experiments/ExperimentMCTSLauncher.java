package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.mctsbn.HillClimbingEvaluator;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.jetbrains.annotations.NotNull;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.*;
import java.util.ArrayList;

public class ExperimentMCTSLauncher {
    public static void main(String[] args) throws Exception {
        // Reading parameters
        int index = Integer.parseInt(args[0]);
        String paramsFileName = args[1];
        //int threads = Integer.parseInt(args[2]);
        String [] parameters = Utils.readParameters(paramsFileName, index);

        // Creating experiment
        String savePath = createSavePath(parameters);
        ExperimentMCTS experimentMCTS = new ExperimentMCTS(parameters, savePath);

        // Defining time variables
        long startTime, endTime;


        // Running the experiment
        Dag_n resultMCTS = experimentMCTS.runExperiment();
        if(resultMCTS == null){
            System.out.println("Experiment has already been done before");
            System.exit(0);
        }

        // Creating Original Dag
        String netPath = parameters[2];
        Dag controlDag = Utils.createOriginalDAG(netPath);
        Dag_n dagOriginal = new Dag_n(controlDag);

        // Setting up HC Control
        Problem problem = experimentMCTS.getProblem();
        HillClimbingEvaluator hc = setupHC(dagOriginal, problem);

        // Running HC
        startTime = System.currentTimeMillis();
        hc.search();
        endTime = System.currentTimeMillis();
        Dag_n hcDag = new Dag_n(hc.getGraph());

        // Computing results from experiments and contorl
        // HC with perfect order
        double bdeuHCPerfect = GESThread.scoreGraph(hcDag, problem);
        double shdHCPerfect = Utils.SHD(Utils.removeInconsistencies(controlDag), hcDag);
        double timeHCPerfect = (double) (endTime - startTime) / 1000;
        System.out.println("\n Best HC: \n    BDeu: " + bdeuHCPerfect + "\n    SHD: " + shdHCPerfect + "\n\tTime(s): " + timeHCPerfect);

        // GroundTruth DAG
        double bdeuOriginal = GESThread.scoreGraph(dagOriginal, problem);
        double shdOriginal = Utils.SHD(Utils.removeInconsistencies(controlDag), dagOriginal);
        System.out.println("\n Original: \n    BDeu: " + bdeuOriginal + "\n    SHD: " + shdOriginal + "\n\tTime(s): " + 0.0);

        // Results from initial PGES in MCTS
        Dag_n PGESdag = new Dag_n(experimentMCTS.getPGESDag());
        double bdeuPGES = GESThread.scoreGraph(PGESdag, problem);
        double shdPGES = Utils.SHD(Utils.removeInconsistencies(controlDag), PGESdag);
        double timePGES = experimentMCTS.getPGESTime();
        System.out.println("\n PGES: \n    BDeu: " + bdeuPGES + "\n    SHD: " + shdPGES + "\n\tTime(s): " + timePGES);

        // Results from MCTSBN
        double bdeuMCTS = GESThread.scoreGraph(resultMCTS, problem);
        double shdMCTS = Utils.SHD(Utils.removeInconsistencies(controlDag), resultMCTS);
        double mctsbnTimeSeconds = experimentMCTS.getTotalTime();
        System.out.println("\n MCTS: \n\tBDeu: " + bdeuMCTS + "\n\tSHD: " + shdMCTS + "\n\tTime(s): " + mctsbnTimeSeconds);

        saveExperiment(parameters, experimentMCTS, bdeuHCPerfect, shdHCPerfect, timeHCPerfect, bdeuOriginal, shdOriginal, bdeuPGES, shdPGES, bdeuMCTS, shdMCTS);
    }

    private static String createSavePath(String[] parameters){
        String resultsFolder = "results/";
        String expHeader = "experiment_";
        String algName = parameters[0];
        String netName = parameters[1];
        String databasePath = parameters[3];
        String databaseName = Utils.getDatabaseNameFromPattern(databasePath);
        int iterationLimit = Integer.parseInt(parameters[4]);
        double exploitConstant = Double.parseDouble(parameters[5]);
        double numberSwaps = Double.parseDouble(parameters[6]);
        double probabilitySwaps = Double.parseDouble(parameters[7]);


        return resultsFolder + expHeader + netName + "_" + algName + "_" +
                databaseName + "_it" + iterationLimit + "_ex" + exploitConstant
                + "_ps" + numberSwaps + "_ns" + probabilitySwaps + ".csv";

    }

    private static void saveExperiment(String[] parameters, ExperimentMCTS experimentMCTS, double bdeuHCPerfect, double shdHCPerfect, double timeHCPerfect, double bdeuOriginal, double shdOriginal, double bdeuPGES, double shdPGES, double bdeuMCTS, double shdMCTS) throws IOException {

        // Saving results
        String savePath = experimentMCTS.getSavePath();
        String algName = parameters[0];
        String netName = parameters[1];
        String databasePath = parameters[3];
        String databaseName = Utils.getDatabaseNameFromPattern(databasePath);
        int iterationLimit = Integer.parseInt(parameters[4]);
        double exploitConstant = Double.parseDouble(parameters[5]);
        double numberSwaps = Double.parseDouble(parameters[6]);
        double probabilitySwaps = Double.parseDouble(parameters[7]);

        double timeMCTS = experimentMCTS.getTotalTime();
        double timePGES = experimentMCTS.getPGESTime();

        File file = new File(savePath);
        if(file.length() == 0) {
            BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
            String header = "algorithm,network,bbdd,threads,itLimit,exploitConst,numSwaps,probSwap,bdeuMCTS,shdMCTS,timeMCTS,bdeuPGES,shdPGES,timePGES,bdeuOrig,shdOrig,bdeuPerfect,shdPerfect,timePerfect\n";
            csvWriter.append(header);

            String results = (algName + ","
                    + netName + ","
                    + databaseName + ","
                    + Runtime.getRuntime().availableProcessors() + ","
                    + iterationLimit + ","
                    + exploitConstant + ","
                    + numberSwaps + ","
                    + probabilitySwaps + ","
                    + bdeuMCTS + ","
                    + shdMCTS + ","
                    + timeMCTS + ","
                    + bdeuPGES + ","
                    + shdPGES + ","
                    + timePGES + ","
                    + bdeuOriginal + ","
                    + shdOriginal + ","
                    + bdeuHCPerfect + ","
                    + shdHCPerfect + ","
                    + timeHCPerfect + "\n");
            csvWriter.append(results);
            csvWriter.flush();
            csvWriter.close();
        }
    }

    @NotNull
    private static ArrayList<Integer> parseOriginalNodesToIntegers(Dag_n dagOriginal, Problem problem) {
        ArrayList<Node> ordenOriginal = dagOriginal.getTopologicalOrder();
        ArrayList<Integer> ordenNuevosNodos = new ArrayList<>(ordenOriginal.size());
        for (Node node : ordenOriginal) {
            for (Node node2 : problem.getVariables()) {
                if (node.getName().equals(node2.getName())) {
                    ordenNuevosNodos.add(problem.getHashIndices().get(node2));
                }
            }
        }
        return ordenNuevosNodos;
    }



    private static HillClimbingEvaluator setupHC(Dag_n dagOriginal, Problem problem) {

        HillClimbingEvaluator hc = new HillClimbingEvaluator(problem);
        ArrayList<Integer> ordenNuevosNodos = parseOriginalNodesToIntegers(dagOriginal, problem);
        hc.setOrder(ordenNuevosNodos);
        return hc;
    }




}