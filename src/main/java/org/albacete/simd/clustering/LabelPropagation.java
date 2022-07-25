package org.albacete.simd.clustering;

/*-******************************************
 *  Rahil Sharma                             *
 *  Multi-thread Label Propogation Algorithm *
 *  (Main Class LabelPropagation)            *
 *  Modified version                         *
 *  Scalable to Multicore Architcture        *
 *  Date : 18th October, 2014 (version 1.0)  * 
 *********************************************/
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Node;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.albacete.simd.utils.Problem;
import org.albacete.simd.utils.Utils;

class NODE {

    int label_name;
    int id;
    Set<Integer> neighbors;

    public NODE(int id, int label_name) {
        this.label_name = label_name;
        this.id = id;
        this.neighbors = new HashSet<>();
    }

    /*-************************* Node methods defined ***********************-*/
    public int get_id() {
        return id;
    }

    public void set_label_name(int label_name) {
        this.label_name = label_name;
    }

    public int get_label_name() {
        return label_name;
    }

    public void set_neighbors(Set<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public void append_neighbors(int id) {
        this.neighbors.add(id);
    }

    public Set<Integer> get_neighbors() {
        return neighbors;
    }
}

/*-****************************** main class *****************************/
/* Read input file, write output file, Community Detection Algorithm     */
/*-***********************************************************************/
public class LabelPropagation {

    ArrayList<NODE> node_list;
    ArrayList<Integer> ordered_nodes;

    public LabelPropagation() { //default constructor
    }

    /*-************************** Read input file from a Problem ***************************-*/
    public void readinput(Problem problem) throws IOException {
        int total_nodes = problem.getVariables().size();
        
        HashMap<Node,Integer> hashNodes = new HashMap<>();
        List<Node> nodeList = problem.getVariables();
        for (int i = 0; i < nodeList.size(); i++) {
            hashNodes.put(nodeList.get(i), i);
        }
        
        node_list = new ArrayList<>(total_nodes);
        ordered_nodes = new ArrayList<>(total_nodes);

        for (int i = 0; i < total_nodes; i++) {
            node_list.add(new NODE(i, i));                       //adding all the nodes to the node list
            ordered_nodes.add(i);                               //Preserving order of nodes
        }
        
        Set<Edge> allEdges = Utils.calculateEdges(problem.getData());

        Random ran = new Random();
        for (Edge edge : allEdges) {
            if (ran.nextInt(100) == 0){
                node_list.get(hashNodes.get(edge.getNode1())).append_neighbors(hashNodes.get(edge.getNode2()));  		//add v2 in the neighborlist of v1
                node_list.get(hashNodes.get(edge.getNode2())).append_neighbors(hashNodes.get(edge.getNode1()));  		//add v1 in the neighborlist of v2
                System.out.println("Enlace " + hashNodes.get(edge.getNode1()) + "-" + hashNodes.get(edge.getNode2()) + ",  " + edge.getNode1() + "-" + edge.getNode2());
            }
        }
    }

    /*-************************** Write output file ***************************-*/
    /*                       Format : "node_id community label"                 */
    /*-************************************************************************-*/
    public void final_communities(String file) throws IOException {
        Map<Integer, Integer> assign_label = new HashMap<>();
        int label_count = 0;
        for (int i = 0; i < node_list.size(); i++) {
            int label = node_list.get(i).get_label_name();
            Integer r = assign_label.get(label);
            if (r == null) {
                label_count++;
                assign_label.put(label, label_count);
            }
        }
        System.out.println("communities = " + label_count);
        /* label_count communities found */
        FileOutputStream fso = new FileOutputStream(file);
        OutputStreamWriter fileWriter = new OutputStreamWriter(fso, Charset.forName("UTF-8"));
        NODE node;
        for (int i = 0; i < node_list.size(); i++) {
            node = node_list.get(i);
            fileWriter.write(node.get_id() + "--" + assign_label.get(node.get_label_name()) + "\n");
        }
        System.out.println("DONE");
        fileWriter.close();
        fso.close();
    }


    /*-************************** Community Detection ***********************-*/
    /*Multi-Threading can also be adapted to multi-processor architecture     */
    /*-**********************************************************************-*/
    public void communityDetection(int total_threads) throws IOException, ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(total_threads);
        ArrayList<FindDominantLabel> dominantLabel_Calc = new ArrayList<>(total_threads);
        for (int i = 0; i < total_threads; i++) {
            dominantLabel_Calc.add(new FindDominantLabel(node_list));
        }

        int label_change = 100; //number of nodes change labels (unstable configuration)
        while (label_change > 0) {
            label_change = 0;
            Collections.shuffle(ordered_nodes);
            //PARALLELISM
            for (int i = 0; i < node_list.size(); i += total_threads) {    //for all nodes
                for (int j = 0; j < total_threads; j++) {                   //blocks of total threads number of nodes run paralley together
                    if ((i + j) < node_list.size()) // if there are enough nodes(= number of threads) to run parallely
                        /*pull each of the j threads from ArrayList (dominantLabel_Calc) and link
                        each node from the ordered list to a thread (one to one mapping) */ {
                        dominantLabel_Calc.get(j).link_node_to_process(ordered_nodes.get(i + j));
                    } else {
                        dominantLabel_Calc.get(j).link_node_to_process(-1);
                    }
                }
                List<Future<Boolean>> result = threadPool.invokeAll(dominantLabel_Calc);
                for (int k = 0; k < result.size(); k++) {
                    Boolean b = result.get(k).get();
                    if (b != null && b == true) {
                        label_change++;
                        if (label_change == 1) // System.out.print("once more");
                        {
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("Communities found");
        threadPool.shutdown();
    }

    /*-************************** Main***************************************-*/
    /*Change number of threads here, (also other parameters)                  */
    /*-**********************************************************************-*/
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        LabelPropagation algo = new LabelPropagation();
        int total_threads = 1;                                // Total threads to use
        
        String networkFolder = "./res/networks/";
        String net_name = "andes";
        String bbdd_path = networkFolder + "BBDD/" + net_name + ".xbif50001_.csv";
        DataSet ds = Utils.readData(bbdd_path);
        Problem problem = new Problem(ds);

        algo.readinput(problem);
        long startTime = System.currentTimeMillis();
        algo.communityDetection(total_threads);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("Time_Taken" + totalTime / 1000.0);
        algo.final_communities("Comunidades.txt");
    }
}
