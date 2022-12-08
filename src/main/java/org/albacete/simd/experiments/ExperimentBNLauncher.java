package org.albacete.simd.experiments;

import org.albacete.simd.algorithms.bnbuilders.GES_BNBuilder;
import org.albacete.simd.algorithms.bnbuilders.PGESwithStages;
import org.albacete.simd.clustering.Clustering;
import org.albacete.simd.clustering.HierarchicalClustering;
import org.albacete.simd.clustering.RandomClustering;
import org.albacete.simd.framework.BNBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ExperimentBNLauncher {

    public static final int MAXITERATIONS = 100;

    private static final String EXPERIMENTS_FOLDER = "/home/jorlabs/projects/ParallelBNs/results/galgo/clustering/ ";
    private int index;
    private String paramsFileName;
    private int threads;
    private ExperimentBNBuilder experiment;
    private String savepath;

    public ExperimentBNLauncher(int index, String paramsFileName, int threads){
        this.index = index;
        this.paramsFileName = paramsFileName;
        this.threads = threads;
    }

    public static void main(String[] args) throws Exception {
        ExperimentBNLauncher experimentBNLauncher = getExperimentBNLauncherFromCommandLineArguments(args);
        String[] parameters = experimentBNLauncher.readParameters();

        experimentBNLauncher.createExperiment(parameters);
        
        if (!experimentBNLauncher.checkExistentFile()){
            experimentBNLauncher.runExperiment();
            experimentBNLauncher.saveExperiment();
        }
    }

    private static ExperimentBNLauncher getExperimentBNLauncherFromCommandLineArguments(String[] args) {
        int index = Integer.parseInt(args[0]);
        String paramsFileName = args[1];
        int threads = Integer.parseInt(args[2]);
        return new ExperimentBNLauncher(index, paramsFileName, threads);
    }

    public String[] readParameters() throws Exception {
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

    private void createExperiment(String[] parameters){
        try {
            experiment = new ExperimentBNBuilder(parameters, threads);
        } catch (Exception e) {
            System.out.println("Exception when creating the experiment");
            e.printStackTrace();
        }
    }
    
    private void runExperiment(){
        experiment.runExperiment();
    }

    private boolean checkExistentFile() throws IOException{
        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + experiment.netName + "_" + experiment.algName + "_" + 
                experiment.databaseName + "_t" + experiment.numberOfThreads + "_PGESt" + experiment.numberOfPGESThreads +
                "_i" + experiment.interleaving + "_s" + experiment.seed + ".csv";
        
        return experiment.checkExistentFile(savePath);
    }

    private void saveExperiment() {
        String results = experiment.getResults();

        String savePath = EXPERIMENTS_FOLDER  + "experiment_results_" + experiment.netName + "_" + experiment.algName + "_" + 
                experiment.databaseName + "_t" + experiment.numberOfThreads + "_PGESt" + experiment.numberOfPGESThreads +
                "_i" + experiment.interleaving + "_s" + experiment.seed + ".csv";
        try {
            Experiment.saveExperiment(savePath, results);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error saving results at: " + savePath);
        }
    }
}
