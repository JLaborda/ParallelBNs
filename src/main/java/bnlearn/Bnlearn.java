package bnlearn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.albacete.simd.threads.GESThread;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;
import org.apache.commons.collections4.map.LinkedMap;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.graph.Dag_n;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;
import weka.classifiers.bayes.net.search.fixed.FromFile;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Bnlearn {
    
    private static Dag_n readXBifFile(String resultBifPath) throws Exception {
        BIFReader reader = new BIFReader();
    
        // procesar el archivo BIF
        reader.processFile(resultBifPath);
        BayesPm bayesPm = Utils.transformBayesNetToBayesPm(reader);
        MlBayesIm resultBn = new MlBayesIm(bayesPm);
        Dag_n resultDag = new Dag_n(Utils.removeInconsistencies(resultBn.getDag()));
        return resultDag;
    }

    /*    private void calcuateMeasurements(MlBayesIm controlBayesianNetwork) {
        // Getting time
        this.elapsedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        
        // SHD Tetrad
        //GraphUtils.GraphComparison comparison = SearchGraphUtils.getGraphComparison(controlBayesianNetwork.getDag(), algorithm.getCurrentDag());
        //this.structuralHamiltonDistanceValue = comparison.getShd();
        
        // "SDM": 
        this.structuralHamiltonDistanceValue = Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        
        this.differencesOfMalkovsBlanket = Utils.avgMarkovBlanketDelta(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag());
        this.numberOfIterations = algorithm.getIterations();
        this.bdeuScore = GESThread.scoreGraph(algorithm.getCurrentDag(), algorithm.getProblem());

        measurementsMap.put("elapsedTime(s)", (double) stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000);
        measurementsMap.put("shd", (double)Utils.SHD(Utils.removeInconsistencies(controlBayesianNetwork.getDag()), algorithm.getCurrentDag()));
        measurementsMap.put("dfMM_avg", differencesOfMalkovsBlanket[0]);
        measurementsMap.put("dfMM_plus", differencesOfMalkovsBlanket[1]);
        measurementsMap.put("dfMM_minus", differencesOfMalkovsBlanket[1]);
        measurementsMap.put("iterations", (double) algorithm.getIterations());
        measurementsMap.put("bdeu", GESThread.scoreGraph(algorithm.getCurrentDag(), algorithm.getProblem()));

    } */

    private static LinkedMap<String, String> generateSaveLinkedMap(Dag_n controlDag_n, Dag_n resultDag_n, LinkedMap<String, String> infoHashMap) {
        System.out.println("Loading problem...");
        System.out.println("Database is in: " + infoHashMap.get("csv_path"));
        Problem problem = new Problem(infoHashMap.get("csv_path"));
    
        LinkedMap<String, String> saveMap = new LinkedMap<>();
    
        // SHD Tetrad
        saveMap.put("shd", String.format("%d", Utils.SHD(controlDag_n, resultDag_n)));
    
        // differencesOfMalkovsBlanket
        double[] differencesOfMalkovsBlanket = Utils.avgMarkovBlanketDelta(controlDag_n, resultDag_n);
        saveMap.put("dfMM_avg", String.format("%.3f", differencesOfMalkovsBlanket[0]));
        saveMap.put("dfMM_plus", String.format("%.3f", differencesOfMalkovsBlanket[1]));
        saveMap.put("dfMM_minus", String.format("%.3f", differencesOfMalkovsBlanket[2]));
       
        //bdeu
        saveMap.put("bdeu", String.format("%.3f", GESThread.scoreGraph(resultDag_n, problem)));
       
        //time
        saveMap.put("time(s)", String.format("%.3f", Double.parseDouble(infoHashMap.get("execution_time"))));
    
        //others
        saveMap.put("index", infoHashMap.get("index"));
        saveMap.put("algorithm", infoHashMap.get("algorithm"));
        saveMap.put("csv_path", infoHashMap.get("csv_path"));
        return saveMap;
    }
    

    public static LinkedMap<String, String> readInfoCsv(String infoCsvPath) {
        LinkedMap<String,String> data = new LinkedMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(infoCsvPath))) {
            List<String[]> records = reader.readAll();
            String[] header = records.get(0); // Suponemos que la primera fila es el encabezado

            // El csv solo tiene una fila
            String[] record = records.get(1);

            for (int j = 0; j < header.length; j++) {
                data.put(header[j], record[j]);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    private static void writeCsvFile(LinkedMap<String, String> saveMap, String savePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(savePath))) {
            // Write header
            String[] header = new String[saveMap.size()];
            Iterator<String> keyIterator = saveMap.keySet().iterator();
            for (int i = 0; i < saveMap.size(); i++) {
                header[i] = keyIterator.next();
            }
            writer.writeNext(header);

            // Write values
            String[] values = new String[saveMap.size()];
            Iterator<String> valueIterator = saveMap.values().iterator();
            for (int i = 0; i < saveMap.size(); i++) {
                values[i] = valueIterator.next();
            }
            writer.writeNext(values);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   public static void main(String[] args) {
        try {
            // Ruta al archivo XBIF
            String resultXBifPath = "/Users/jdls/developer/projects/ParallelBNs/results/pruebas/bnlearn/tabu/exp_tabu_1.xbif";
            String originalXbifPath = "/Users/jdls/developer/projects/ParallelBNs/res/networks/alarm/alarm.xbif";
            // Ruta al archivo info
            String infoPath = "/Users/jdls/developer/projects/ParallelBNs/results/pruebas/bnlearn/tabu/exp_tabu_1_info.csv";
            String savePath = "/Users/jdls/developer/projects/ParallelBNs/results/pruebas/bnlearn/tabu/exp_tabu_1_results.csv";
            // Crear un cargador BIF
            Dag_n resultDag_n = readXBifFile(resultXBifPath);
            Dag_n originalDag_n = readXBifFile(originalXbifPath);
            LinkedMap<String,String> infoMap = readInfoCsv(infoPath);

            System.out.println("Info Csv:");
            for (Map.Entry<String, String> entry : infoMap.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            
            LinkedMap<String,String> saveMap = generateSaveLinkedMap(originalDag_n, resultDag_n, infoMap);
            
            // Saving the results
            for (Map.Entry<String, String> entry : saveMap.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            System.out.println("Saving to " + savePath);
            writeCsvFile(saveMap, savePath);

            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
