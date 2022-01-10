package org.albacete.simd.experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentBNLauncher {

    public static final String[] algorithmNames = {"ges", "pges", "hc", "phc", "pfhcbes"};
    public static final String[] netNames = {"alarm", "andes", "barley", "cancer", "child", "earthquake", "hailfinder",
            "hepar2", "insurance", "link", "mildew", "munin", "pigs", "water", "win95pts"};
    public static final String[] bbddEndings = {".xbif_.csv", ".xbif50001_.csv", ".xbif50002_.csv", ".xbif50003_.csv",
            ".xbif50004_.csv", ".xbif50005_.csv", ".xbif50006_.csv", ".xbif50007_.csv", ".xbif50008_.csv",
            ".xbif50009_.csv", ".xbif50001246_.csv"};
    public static final int [] interleavings = {5, 10, 15};
    public static final int [] seeds = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29};
    public static Map<String, String> networkPaths = new HashMap<>();
    public static Map<String, List<String>> bbddPaths = new HashMap<>();
    public static Map<String, String> testPaths = new HashMap<>();
    public static Map<Integer, List<Object>> parameters = new HashMap<>();

    public static void main(String[] args) {
        int nThreads = Runtime.getRuntime().availableProcessors();
        int index = Integer.parseInt(args[0]);
        initializeParameters();


    }

    public static void initializeParameters(){

        for(String name: netNames) {
            // Setting networkPaths
            networkPaths.put(name, "./res/networks/" + name + ".xbif");

            // Setting BBDDPaths
            List<String> paths = new ArrayList<>();
            for(String ending: bbddEndings){
                paths.add("./res/networks/BBDD/" + name + ending);
            }
            bbddPaths.put(name, paths);

            // Setting test paths
            testPaths.put(name, "./res/networks/BBDD/tests/" + name + "_test.csv");


        }

    }
}
