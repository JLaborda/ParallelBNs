package org.albacete.simd.experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author pablo
 */
public class RunExperimentsPablo {

    public static final String NET_FOLDER = "./res/networks/";
    public static final String BBDD_FOLDER = "./res/networks/BBDD/";
    public static final String TEST_FOLDER = "./res/networks/BBDD/tests/";

    public static final String[] NET_NAMES
            = {"alarm", "andes", "barley", "cancer", "child", "earthquake", "hailfinder", "hepar2",
                "insurance", "link", "mildew", "munin", "pigs", "water", "win95pts"}; //15
    // barley da problemas (OutOfMemoryError)

    // Redes pequeñas - medianas (20-50 nodos)
    //{"alarm", "child", "insurance", "water", "hailfinder", "hepar2", "win95pts", "link", "pigs", "mildew"};
    public static ArrayList<Integer> SKIP = new ArrayList<>();

    public static final int[] NET_PARAMETERS
            = {509, 1157, 114005, 10, 230, 10, 2656, 1453, 984, 14211, 540150, 80592, 5618, 10083, 574};

    public static final String EXPERIMENTS_FOLDER = "/parallelbns/experiments/";
    //public static final String EXPERIMENTS_FOLDER = "./experiments/";

    public static String createNetworkPath(String net_name) {
        // Adding net paths
        return NET_FOLDER + net_name + ".xbif";
    }

    public static String createTestPath(String net_name) {
        return TEST_FOLDER + net_name + "_test.csv";
    }

    public static void main(String[] args) throws FileNotFoundException {
        try {
            String savePath = EXPERIMENTS_FOLDER + "experiments.csv";

            String savePath2 = EXPERIMENTS_FOLDER + "experiments_total.csv";

            String savePath3 = EXPERIMENTS_FOLDER + "experiments_SD.csv";

            // Get the hyperparameters according with the index
            int index = Integer.parseInt(args[0]);

            int base = Integer.parseInt(args[1]);

            System.out.println("Numero de hilos: " + Runtime.getRuntime().availableProcessors());
            int thr = Runtime.getRuntime().availableProcessors();

            String netName = NET_NAMES[base];

            // Initial variables
            //int[] threads = {1, 2, 4, 6, 8};
            int maxIterations = 100;
            int[] interleavings = {5, 10, 15};
            int[] ratios = {1, 2, 4, 8};
            int[] seeds = {2, 3, 5, 7, 11};//, 13, 17, 19, 23, 29};

            /*if (file.length() == 0) {
                String header = "Base, Nodos, Arcos, Params, I, T, BDeu, Iteraciones, Tiempo, NodosN, Enlaces\n";
                csvWriter.append(header);
            }*/
            Experiment experiment = null;

            String bbddPath = BBDD_FOLDER + netName + ".xbif50001_.csv";

            // Creating Experiments
            String netPath = createNetworkPath(netName);
            String testPath = createTestPath(netName);

            // Obteniendo interleaving y ratio de index
            int inter = index / interleavings.length;
            int ratio = index % ratios.length;
            System.out.println("Index: " + index + ", inter: " + inter + 
                    ", ratio: " + ratio + ", thr: " + thr);
            
            System.out.println("Ejecutamos PGESv2");
            
            ArrayList<Experiment> results = new ArrayList<>();
            for (Integer seed : seeds) {
                //PGES
                try {
                    experiment = new ExperimentPGES(netPath, bbddPath, testPath, thr, maxIterations, interleavings[index], seed, ratio);
                    experiment.runExperiment();
                    experiment.printResults();
                    results.add(experiment);

                    File file2 = new File(savePath2);
                    FileWriter csvWriter2 = new FileWriter(file2, true);

                    // Guardamos todas las ejecuciones
                    csvWriter2.append(netName + ","
                            + experiment.getNnodes() + ","
                            + experiment.getNarcs() + ","
                            + NET_PARAMETERS[base] + ","
                            + thr + ","
                            + interleavings[inter] + ","
                            + ratios[ratio] + ","
                            + experiment.getScore() + ","
                            + experiment.getnIterations() + ","
                            + (experiment.getElapsedTimeMiliseconds() / (double) 1000) + ","
                            + experiment.getNewNnodes() + ","
                            + experiment.getNewNarcs() + ","
                            + experiment.getShd() + ","
                            + experiment.getLLScore() + ","
                            + experiment.getDfmm()[0] + ","
                            + experiment.getDfmm()[1] + ","
                            + experiment.getDfmm()[2] + "\n");
                    csvWriter2.flush();
                    csvWriter2.close();

                } catch (OutOfMemoryError | Exception ex) {
                    System.out.println("Out Of Memory Error | Exception: " + ex);
                }
            }

            // Media de seed
            double bdeu = 0, ite = 0, time = 0, nodes = 0, arcs = 0, shd = 0,
                    llscore = 0, dfmm0 = 0, dfmm1 = 0, dfmm2 = 0;
            for (Experiment result : results) {
                bdeu += result.getScore();
                ite += result.getnIterations();
                time += (result.getElapsedTimeMiliseconds() / (double) 1000);
                nodes += result.getNewNnodes();
                arcs += result.getNewNarcs();
                shd += result.getShd();
                llscore += result.getLLScore();
                dfmm0 += result.getDfmm()[0];
                dfmm1 += result.getDfmm()[1];
                dfmm2 += result.getDfmm()[2];
            }

            double[][] resultsArray = new double[10][seeds.length];
            for (int i = 0; i < seeds.length; i++) {
                resultsArray[0][i] = results.get(i).getScore();
                resultsArray[1][i] = results.get(i).getnIterations();
                resultsArray[2][i] = (results.get(i).getElapsedTimeMiliseconds() / (double) 1000);
                resultsArray[3][i] = results.get(i).getNewNnodes();
                resultsArray[4][i] = results.get(i).getNewNarcs();
                resultsArray[5][i] = results.get(i).getShd();
                resultsArray[6][i] = results.get(i).getLLScore();
                resultsArray[7][i] = results.get(i).getDfmm()[0];
                resultsArray[8][i] = results.get(i).getDfmm()[1];
                resultsArray[9][i] = results.get(i).getDfmm()[2];
            }

            ArrayList resultsSD = new ArrayList();
            for (double[] resultsArray1 : resultsArray) {
                resultsSD.add(calculateSD(resultsArray1));
            }

            File file = new File(savePath);
            FileWriter csvWriter = new FileWriter(file, true);

            // Guardamos la media de las ejecuciones de las distintas semillas
            csvWriter.append(netName + ","
                    + experiment.getNnodes() + ","
                    + experiment.getNarcs() + ","
                    + NET_PARAMETERS[base] + ","
                    + thr + ","
                    + interleavings[inter] + ","
                    + ratios[ratio] + ","
                    + (bdeu / results.size()) + ","
                    + (ite / results.size()) + ","
                    + (time / results.size()) + ","
                    + (nodes / results.size()) + ","
                    + (arcs / results.size()) + ","
                    + (shd / results.size()) + ","
                    + (llscore / results.size()) + ","
                    + (dfmm0 / results.size()) + ","
                    + (dfmm1 / results.size()) + ","
                    + (dfmm2 / results.size()) + "\n");
            csvWriter.flush();
            csvWriter.close();

            File file3 = new File(savePath3);
            FileWriter csvWriter3 = new FileWriter(file3, true);

            // Guardamos la media de las SD de las ejecuciones
            csvWriter3.append(netName + ","
                    + experiment.getNnodes() + ","
                    + experiment.getNarcs() + ","
                    + NET_PARAMETERS[base] + ","
                    + thr + ","
                    + interleavings[inter] + ","
                    + ratios[ratio] + ","
                    + resultsSD.get(0) + ","
                    + resultsSD.get(1) + ","
                    + resultsSD.get(2) + ","
                    + resultsSD.get(3) + ","
                    + resultsSD.get(4) + ","
                    + resultsSD.get(5) + ","
                    + resultsSD.get(6) + ","
                    + resultsSD.get(7) + ","
                    + resultsSD.get(8) + ","
                    + resultsSD.get(9) + "\n");
            csvWriter3.flush();
            csvWriter3.close();

        } catch (IOException ex) {
        }
    }

    public static double calculateSD(double numArray[]) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }
}
