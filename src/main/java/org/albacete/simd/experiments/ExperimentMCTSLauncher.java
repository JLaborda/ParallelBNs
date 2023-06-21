package org.albacete.simd.experiments;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Node;
import org.albacete.simd.mctsbn.HillClimbingEvaluator;
import org.albacete.simd.mctsbn.MCTSBN;
import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentMCTSLauncher {
    public static void main(String[] args) throws Exception {
        int index = Integer.parseInt(args[0]);
        String paramsFileName = args[1];
        int threads = Integer.parseInt(args[2]);

        String[] parameters = readParameters(paramsFileName, index);

        String algName = parameters[0];
        String netName = parameters[1];
        String netPath = parameters[2];
        String databasePath = parameters[3];
        String databaseName = getDatabaseNameFromPattern(databasePath);
        int iterationLimit = Integer.parseInt(parameters[4]);
        double exploitConstant = Double.parseDouble(parameters[5]);
        double numberSwaps = Double.parseDouble(parameters[6]);
        double probabilitySwap = Double.parseDouble(parameters[7]);

        String savePath = "results/experiment_" + netName + "_" + algName + "_" +
                databaseName + "_t" + threads + "_it" + iterationLimit + "_ex" + exploitConstant
                + "_ps" + numberSwaps + "_ns" + probabilitySwap + ".csv";
        File file = new File(savePath);

        // Si no existe el fichero
        if(file.length() == 0) {
            Problem problem = new Problem(databasePath);

            String alg;
            switch (algName) {
                default:
                case "mcts":
                case "mcts-pGES":
                    alg = "pGES";
                    break;
                case "mcts-fGES":
                    alg = "fGES";
                    break;
                case "mcts-PC":
                    alg = "PC";
                    break;
                case "mcts-CPC":
                    alg = "CPC";
                    break;
                case "mcts-PC-Max":
                    alg = "PC-Max";
                    break;
            }
            MCTSBN mctsbn = new MCTSBN(problem, iterationLimit, netName, databaseName, threads, exploitConstant, numberSwaps, probabilitySwap, alg);
            mctsbn.EXPLOITATION_CONSTANT = exploitConstant;
            mctsbn.NUMBER_SWAPS = numberSwaps;
            mctsbn.PROBABILITY_SWAP = probabilitySwap;

            double init = System.currentTimeMillis();
            Dag_n result = mctsbn.search();
            double time = (System.currentTimeMillis() - init)/1000.0;


            // Calculate scores
            MlBayesIm controlBayesianNetwork;
            try {
                controlBayesianNetwork = readOriginalBayesianNetwork(netPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            HillClimbingEvaluator hc = mctsbn.hc;

            Dag_n dagOriginal = new Dag_n(controlBayesianNetwork.getDag());
            ArrayList<Node> ordenOriginal = dagOriginal.getTopologicalOrder();
            ArrayList<Integer> ordenNuevosNodos = new ArrayList<>(ordenOriginal.size());
            for (Node node : ordenOriginal) {
                for (Node node2 : problem.getVariables()) {
                    if (node.getName().equals(node2.getName())) {
                        ordenNuevosNodos.add(problem.getHashIndices().get(node2));
                    }
                }
            }
            hc.setOrder(ordenNuevosNodos);
            hc.search();
            Dag_n hcDag = new Dag_n(hc.getGraph());
            double bdeuHCPerfect = GESThread.scoreGraph(hcDag, problem);
            double shdHCPerfect = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), hcDag);
            System.out.println("\n Best HC: \n    BDeu: " + bdeuHCPerfect + "\n    SHD: " + shdHCPerfect);

            double bdeuOriginal = GESThread.scoreGraph(dagOriginal, problem);
            double shdOriginal = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), dagOriginal);
            System.out.println("\n Original: \n    BDeu: " + bdeuOriginal + "\n    SHD: " + shdOriginal);

            Dag_n PGESdag = new Dag_n(mctsbn.getPGESDag());
            double bdeuPGES = GESThread.scoreGraph(PGESdag, problem);
            double shdPGES = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), PGESdag);
            System.out.println("\n PGES: \n    BDeu: " + bdeuPGES + "\n    SHD: " + shdPGES);

            double bdeuMCTS = GESThread.scoreGraph(result, problem);
            double shdMCTS = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), result);
            System.out.println("\n MCTS: \n    BDeu: " + bdeuMCTS + "\n    SHD: " + shdMCTS);

            file = new File(savePath);
            if(file.length() == 0) {
                BufferedWriter csvWriter = new BufferedWriter(new FileWriter(savePath, true));
                String header = "algorithm,network,bbdd,threads,itLimit,exploitConst,numSwaps,probSwap,bdeuMCTS,shdMCTS,bdeuPGES,shdPGES,bdeuOrig,shdOrig,bdeuPerfect,shdPerfect,timePGES,time\n";
                csvWriter.append(header);

                String results = (algName + ","
                        + netName + ","
                        + databaseName + ","
                        + threads + ","
                        + iterationLimit + ","
                        + exploitConstant + ","
                        + numberSwaps + ","
                        + probabilitySwap + ","
                        + bdeuMCTS + ","
                        + shdMCTS + ","
                        + bdeuPGES + ","
                        + shdPGES + ","
                        + bdeuOriginal + ","
                        + shdOriginal + ","
                        + bdeuHCPerfect + ","
                        + shdHCPerfect + ","
                        + (double) mctsbn.PGESTime + ","
                        + (double) time + "\n");
                csvWriter.append(results);
                csvWriter.flush();
                csvWriter.close();
            }
        }
        else {
            System.out.println("Experimento:  " + savePath + "    ya existente.");
        }
    }

    private static String getDatabaseNameFromPattern(String databasePath){
        // Matching the end of the csv file to get the name of the database
        Pattern pattern = Pattern.compile(".*/(.*).csv");
        Matcher matcher = pattern.matcher(databasePath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String[] readParameters(String paramsFileName, int index) throws Exception {
        String[] parameterStrings = null;
        try (BufferedReader br = new BufferedReader(new FileReader(paramsFileName))) {
            String line;
            for (int i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();
            parameterStrings = line.split(" ");
        }
        catch(FileNotFoundException e){
            System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parameterStrings;
    }

    private static MlBayesIm readOriginalBayesianNetwork(String netPath) throws Exception {
        BIFReader bayesianReader = new BIFReader();
        bayesianReader.processFile(netPath);
        BayesNet bayesianNet = bayesianReader;
        System.out.println("Numero de variables: " + bayesianNet.getNrOfNodes());

        //Transforming the BayesNet into a BayesPm
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(bayesianNet);
        MlBayesIm bn2 = new MlBayesIm(bayesPm);

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        return bn2;
    }

}