package org.albacete.simd.experiments;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentSmallMediumSet {



    public static HashMap<String, HashMap<String, String>> hashNetworks(List<String> net_paths, List<String> bbdd_paths, Set<String> names){

        HashMap<String, HashMap<String,String>> result = new HashMap<String,HashMap<String,String>>();

        ArrayList<String> bbdd_numbers = new ArrayList<String>();

        for(String bbdd: bbdd_paths) {
            Pattern pattern =Pattern.compile("(xbif.*).csv");
            Matcher matcher = pattern.matcher(bbdd);
            if(matcher.find()) {
                bbdd_numbers.add(matcher.group(1));
            }
        }

        for(String bbdd_number : bbdd_numbers) {
            HashMap<String, String> aux = new HashMap<String, String>();


            for(String bbdd_path: bbdd_paths) {
                if(bbdd_path.contains(bbdd_number)) {
                    for(String net_path: net_paths) {
                        //Pattern pattern = Pattern.compile("/(.*)\\.");
                        Pattern pattern = Pattern.compile("/(\\w+)\\..*");
                        Matcher matcher = pattern.matcher(net_path);

                        if (matcher.find()) {
                            //System.out.println("Match!");
                            String net_name = matcher.group(1);
                            System.out.println(net_name);
                            //System.out.println("BBDD Path: " + bbdd_path);
                            if (bbdd_path.contains(net_name) & names.contains(net_name)){
                                aux.put(net_path, bbdd_path);
                            }
                        }

                    }
                }
            }
            result.put(bbdd_number, aux);

        }
        return result;
    }


    public static void main(String[] args) {
        String netFolder = "res/networks/";
        String bbddFolder = "res/networks/BBDD/";
        ArrayList<String> net_paths = Experiment.getNetworkPaths(netFolder);
        ArrayList<String> bbdd_paths = Experiment.getBBDDPaths(bbddFolder);

        System.out.println("net_names: " + net_paths);
        System.out.println("bbdd_names: " + bbdd_paths);

        Set<String> names = new HashSet<>();
        names.add("alarm");
        /*
        names.add("cancer");
        names.add("earthquake");
        names.add("barley");
        names.add("child");
        names.add("mildew");
        names.add("insurance");
        names.add("water");
        */
        HashMap<String, HashMap<String, String>> map =  hashNetworks(net_paths, bbdd_paths, names);

        /*
        for (String key: map.keySet() ) {
            System.out.println("Map1: " + key);
            HashMap<String,String> submap = map.get(key);
            for (String key2 : submap.keySet()) {
                System.out.println("Network: " + key2);
                System.out.println("BBDD: "+submap.get(key2));
            }
        }
         */

        int[] nThreads = {1,2,4,8};
        int[] nInterleavings = {5, 10, 15};

        //Experiment.runAllExperiments(nThreads, nInterleavings, map);
    }


}
