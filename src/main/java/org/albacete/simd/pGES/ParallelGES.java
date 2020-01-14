package org.albacete.simd.pGES;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import consensusBN.ConsensusBES;
import consensusBN.Fusionable;
import consensusBN.HeuristicConsensusMVoting;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.Dag;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.SearchGraphUtils;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.net.BIFReader;



@SuppressWarnings("DuplicatedCode")
public class ParallelGES {

	DataSet data;
	int nThreads;
	int nItInterleaving;
	int maxIterations = 15;
	DataSet[] samples;
	ThGES[] search;
	Thread[] threads;
	ArrayList[] subSets;
	ArrayList<Dag> graphs = null;
	Graph currentGraph = null;
	Graph previousGraph = null;
	Scorer scorer;
	int it = 1;
	
	long totalTimeIterations;
	

	String fusionConsensus = "ConsensusBES";
	String net_path = null;
	String bbdd_path = null;
	String net_name = null;
	String bbdd_name = null;
	MlBayesIm bn2 = null;
	FileWriter csvWriter_iters;
	FileWriter csvWriter_global;
	
	
	ArrayList<Long> times_iterations = new ArrayList<>();
	ArrayList<Long> times_fusion = new ArrayList<>();
	ArrayList<Long> times_delta = new ArrayList<>();
	ArrayList<Double> scores_threads = new ArrayList<>();
	ArrayList<Double> scores_fusion = new ArrayList<>();
	ArrayList<Double> scores_delta = new ArrayList<>();
	
	public ParallelGES(DataSet data,  int nThreads, int nItInterleaving) {
		super();
		this.data = data;
		this.nThreads = nThreads;
		this.nItInterleaving = nItInterleaving;
		this.samples = new DataSet[this.nThreads];
		this.search = new ThGES[this.nThreads];
		this.threads = new Thread[this.nThreads];
		this.subSets = new ArrayList[this.nThreads];
		int n = 0;
		for(int s = 0; s< subSets.length-1; s++){
			ArrayList<Node> sub = new ArrayList<>();
			for(int i = 0; i < Math.floorDiv(data.getNumColumns(),this.nThreads) ; i++){
				sub.add(data.getVariable(n++));
			}
			this.subSets[s] = sub;
		}
		ArrayList<Node> sub = new ArrayList<>();
		for(int i = n; i < data.getNumColumns(); i++ ){
			sub.add(data.getVariable(i));
		}
		this.subSets[this.subSets.length-1] = sub;
		
		for( int i = 0; i< this.nThreads; i++){
			this.samples[i] =  this.data; //DataUtils.getBootstrapSample(this.data, this.data.getNumRows());
			this.search[i] = new ThGES(this.samples[i],this.subSets[i],this.nItInterleaving);
		}
		
		//Setting scorer
		this.scorer = new Scorer(this.data);
		
	}
	public ParallelGES(DataSet data, MlBayesIm bn2, String net_name, String bbdd_name, String fusionConsensus, int nThreads, int nItInterleaving){
		this(data,nThreads, nItInterleaving);
		try {
		this.net_name = net_name;
		this.bbdd_name = bbdd_name;
		//System.out.println("Numero de variables: "+bn.getNrOfNodes());
		this.bn2 = bn2;
		System.out.println("Creating bn2. Bn2 null?");
		
		// Setting fusion parameter
		this.fusionConsensus = fusionConsensus;
		
		// Setting writing configuration
		String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusionConsensus + "_iteratation_results.csv";
		
		// File
		File file_iters = new File(path_iters);
		file_iters.getParentFile().mkdirs();

		// File Writer
		csvWriter_iters = new FileWriter(file_iters);
		
		// Iterations results header
		csvWriter_iters.append("Iteration");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Score_Threads");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Score_Fusion");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Score_Delta");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Time_Iteration(ms)");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Time_Fusion(ms)");
		csvWriter_iters.append(",");
		csvWriter_iters.append("Time_Delta(ms)");
		csvWriter_iters.append("\n");
		
		// Flushing 
		csvWriter_iters.flush();
		csvWriter_iters.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	
	public static double [] avgMarkovBlanquetdif(Dag original, Dag created) {
		
		if (original.getNodes().size() != created.getNodes().size())
			return null;
		
		// First number is the average dfMB, the second one is the amount of more variables in each MB, the last number is the the amount of missing variables in each MB 
		double [] result = new double[3];
		double res1 = 0;
		double res2 = 0;
		double res3 = 0;
		
		
		for( Node e1 : original.getNodes()) {
			Node e2 = created.getNode(e1.getName());
			
			// Creando los mantos de markov de la variable en cada DAG.
			ArrayList<Node> mb1 = new ArrayList<>();
			ArrayList<Node> mb2 = new ArrayList<>();

			mb1.addAll(original.getParents(e1));
			mb1.addAll(original.getChildren(e1));
			for(Node c: original.getChildren(e1)) {
				mb1.addAll(original.getParents(c));
			}
			
			mb2.addAll(created.getParents(e2));
			mb2.addAll(created.getChildren(e2));
			for(Node c: created.getChildren(e2)) {
				mb2.addAll(created.getParents(c));
			}

			ArrayList<String> names1 = new ArrayList<>();
			ArrayList<String> names2 = new ArrayList<>();
			// Nodos de más en el manto creado
			for (Node n1 : mb1) {
				String name1 = n1.getName();
				names1.add(name1);
			}
			for (Node n2 : mb2) {
				String name2 = n2.getName();
				names2.add(name2);
			}
			
			//Variables de más
			for(String s2: names2) {
				if(!names1.contains(s2)) {
					res1++;
					res2++;
				}
			}
			// Variables de menos
			for(String s1: names1) {
				if(!names2.contains(s1)) {
					res1++;
					res3++;
				}
			}
		}
		
		// Avg difference
		res1 = res1 / original.getNodes().size();
		
		result[0] = res1;
		result[1] = res2;
		result[2] = res3;
		
		return result;
		
	}
	
	private boolean convergence(int it) throws InterruptedException {
		if (it > this.maxIterations)
			return true;
		
		// Checking that the threads have done something
		for(int i=0; i<this.nThreads; i++) {
			if (this.search[i].getFlag()) {
				return false;
			}
		}
		return true;
		
	}

	public void search() throws InterruptedException{
		
		//boolean change = true;
		
		this.it = 1;
		long startTime_It;
		long endTime_It;
		long startTime_Fusion;
		long endTime_Fusion;
		
		long time_It;
		long time_Fusion;
		long delta_time;
		
		while(true){
			startTime_It = System.currentTimeMillis();
			System.out.println("Iteration: " + (it));
			
			// Saving previous graph
			if (this.currentGraph != null)
				this.previousGraph = new EdgeListGraph(this.currentGraph);
			// parallel GES running
			this.graphs = new ArrayList<>();
			
			// Starting threads
			
			for(int i = 0 ; i< this.nThreads; i++){
				//Graph g = this.search[i].search();
				search[i].resetFlag(); 				// Reseting flag search
				threads[i] = new Thread(search[i]);
				threads[i].start();
			}
			// Getting results
			double score_threads = 0;
			for(int i = 0 ; i< this.nThreads; i++){
				// Joining threads and getting currentGraph
				threads[i].join(); 
				Graph g = search[i].getCurrentGraph();
				
				score_threads = score_threads + search[i].getScoreBDeu();
				
				
				SearchGraphUtils.pdagToDag(g);
				Node nodeT, nodeH;
                for (Edge e : g.getEdges()){
                    if(!e.isDirected()) continue;
                    Endpoint endpoint1 = e.getEndpoint1();
                    if (endpoint1.equals(Endpoint.ARROW)){
                        nodeT = e.getNode1(); 
                        nodeH = e.getNode2();
                    }else{
                        nodeT = e.getNode2();
                        nodeH = e.getNode1();
                    }
                    if(g.existsDirectedPathFromTo(nodeT, nodeH)) g.removeEdge(e);
                }
	                 
	                Dag gdag = new Dag(g);
	                this.graphs.add(gdag);

			}
			
			// FUSION
			// Aquí estaba puesto el ConsensusBES, que es más conservador y por tanto borra mucho más. (Borrar mucho es malo)
//			HeuristicConsensusMVoting fusion = new HeuristicConsensusMVoting (this.graphs,0.25);
			
			Fusionable fusion;
			if(this.fusionConsensus.equals("ConsensusBES"))
				fusion = new ConsensusBES(this.graphs);
			
			else{
				if(this.fusionConsensus.equals("HeuristicConsensusMVoting"))
					fusion = new HeuristicConsensusMVoting (this.graphs,0.25);
				
				else
					fusion = new ConsensusBES(this.graphs);
			}
			
			startTime_Fusion = System.currentTimeMillis();
			fusion.fusion();
			endTime_Fusion = System.currentTimeMillis();
			time_Fusion = endTime_Fusion - startTime_Fusion;
			this.currentGraph = fusion.getFusion();
			//System.out.println("CurrentGraph is null? ");
			//System.out.println(this.currentGraph == null);
			
			// Scores
			score_threads = score_threads / this.nThreads;
			double score_fusion = this.scorer.scoreGraph(this.currentGraph);
			double score_delta = score_fusion - score_threads;
			// Saving scores
			scores_threads.add(score_threads);
			scores_fusion.add(score_fusion);
			scores_delta.add(score_delta);
			
			System.out.println("Score threads: " + score_threads);
			System.out.println("Score fusion: " + score_fusion);
			System.out.println("Score delta fusion: " + score_delta);
			
			
			for(int i=0; i< this.nThreads; i++){
				this.search[i].setInitialGraph(this.currentGraph);
			}
			
			// Interrupting threads
			for(int i = 0 ; i< this.nThreads; i++)
				threads[i].interrupt();
			
			
			// Calculating Times
			endTime_It = System.currentTimeMillis();
			time_It = endTime_It - startTime_It;
			delta_time = time_It - time_Fusion;
			this.totalTimeIterations = this.totalTimeIterations + time_It;
			//Printing
			System.out.println("Time iteration(ms): " + time_It);
			System.out.println("Time fusion(ms): " + time_Fusion);
			System.out.println("delta time: " + delta_time);
			System.out.println("Total time iterations(ms): " + totalTimeIterations);
			//Saving
			times_iterations.add(time_It);
			times_fusion.add(time_Fusion);
			times_delta.add(delta_time);
			
			// Writing data into csv
			saveExperiment();
			
			//Convergence method
			if(convergence(it)) {
				//change = false;
				break;
				}
			it++;

			
			
		}
		System.out.println("Total Iterations: " + it);
	}
	
	
	public void saveExperiment() {
		try {
			// Saving Iteration data
			String path_iters = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusionConsensus + "_iteratation_results.csv";
			
			// File
			File file_iters = new File(path_iters);
			file_iters.getParentFile().mkdirs();

			// File Writer
			csvWriter_iters = new FileWriter(file_iters,true);

			// All of the arrays should have the same size.
			
			String row = (it) + "," + scores_threads.get(it-1) + "," + scores_fusion.get(it-1) + "," + scores_delta.get(it-1) + "," + times_iterations.get(it-1) + "," + times_fusion.get(it-1) + "," + times_delta.get(it-1) + "\n";  
			csvWriter_iters.append(row);
			csvWriter_iters.flush();
			csvWriter_iters.close();
			
			
			// Saving global results
			String path_global = "experiments/" + this.net_name + "/" + this.bbdd_name + "T" + this.nThreads + "_I" + this.nItInterleaving + "_" + this.fusionConsensus + "_global_results.csv";
			// Files
			File file_global = new File(path_global);
			file_global.getParentFile().mkdirs();			
			// File Writers
			FileWriter csvWriter_global = new FileWriter(file_global);
			//Header
			csvWriter_global.append("SHD");
			csvWriter_global.append(",");
			csvWriter_global.append("BDeu Score");
			csvWriter_global.append(",");
			csvWriter_global.append("dfMM");
			csvWriter_global.append(",");
			csvWriter_global.append("dfMM plus");
			csvWriter_global.append(",");
			csvWriter_global.append("dfMM minus");
			csvWriter_global.append(",");
			csvWriter_global.append("Total iterations");
			csvWriter_global.append(",");
			csvWriter_global.append("Total time");
			csvWriter_global.append("\n");
			// Results
			//System.out.println("CurrentGraph is null? ");
			//System.out.println(this.currentGraph == null);
			//System.out.println("bn2 is null? ");
			//System.out.println(this.bn2 == null);
			
			int shd = compare(this.bn2.getDag(), (Dag) this.currentGraph);
			double [] dfmm = avgMarkovBlanquetdif(this.bn2.getDag(), (Dag)this.currentGraph);
			double score = this.getFinalScore();
			long endTime = System.currentTimeMillis();

			String row2 = "null";
			if (dfmm != null) {
				row2 = shd + "," + score + "," + dfmm[0] + "," + dfmm[1] + "," + dfmm[2] + "," + this.it + "," + this.totalTimeIterations + "\n";
			}
			csvWriter_global.append(row2);
			
			csvWriter_global.flush();
			csvWriter_global.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
		
	public static int compare(Dag bn1, Dag bn2){
		ArrayList<Dag> dags = new ArrayList<>();
		dags.add(bn1);
		dags.add(bn2);
		ensureVariables(dags);
		ConsensusBES cons = new ConsensusBES(dags);
		cons.fusion();
		return cons.getNumberOfInsertedEdges();
	}
	
	 private static void ensureVariables(ArrayList<Dag> setofbns){

		   List<Node> nodes = setofbns.get(0).getNodes();

		   for(int i = 1 ; i< setofbns.size(); i++){
			   Dag oldDag = setofbns.get(i);
			   List<Edge> oldEdges = oldDag.getEdges();
			   Dag newdag = new Dag(nodes);
			   for(Edge e: oldEdges){
				   Node node1 = setofbns.get(0).getNode(e.getNode1().getName());
				   Node node2 = setofbns.get(0).getNode(e.getNode2().getName());
				   Edge newEdge = new Edge(node1,node2, e.getEndpoint1(), e.getEndpoint2());
				   newdag.addEdge(newEdge);
			   }
			   setofbns.remove(i);
			   setofbns.add(i, newdag);			
		   }
	   }

	public ArrayList<Long> getTimes_iterations() {
		return times_iterations;
	}

	public ArrayList<Long> getTimes_fusion() {
		return times_fusion;
	}

	public ArrayList<Long> getTimes_delta() {
		return times_delta;
	}

	public ArrayList<Double> getScores_threads() {
		return scores_threads;
	}

	public ArrayList<Double> getScores_fusion() {
		return scores_fusion;
	}

	public ArrayList<Double> getScores_delta() {
		return scores_delta;
	}

	public double getFinalScore() {
		return this.scorer.scoreGraph(this.currentGraph);
	}
	
	public int getIterations() {
		return this.it;
	}
	
	public long getTotalTimeIterations() {
		return totalTimeIterations;
	}
	
	
	public static void main(String[] args) {
		
		   try {
			   long startTime = System.currentTimeMillis();
			   BIFReader bf = new BIFReader();
			   bf.processFile("networks/alarm.xbif");
			   BayesNet bn = (BayesNet) bf;
			   System.out.println("Numero de variables: "+bn.getNrOfNodes());
			   MlBayesIm bn2 = new MlBayesIm(bn);
			   DataReader reader = new DataReader();
			   reader.setDelimiter(DelimiterType.COMMA);
			   reader.setMaxIntegralDiscrete(100);
			   DataSet dataSet = reader.parseTabular(new File("networks/BBDD/alarm.xbif50002_.csv"));
			   ParallelGES alg = new ParallelGES(dataSet,4,10);
			   alg.search();
			   long endTime = System.currentTimeMillis();
			   long elapsedTime = endTime - startTime;
			   int shd = compare(bn2.getDag(),(Dag) alg.currentGraph);
			   double [] dfmm = avgMarkovBlanquetdif(bn2.getDag(), (Dag) alg.currentGraph);
			   System.out.println("SHD: "+shd);
			   System.out.println("Total execution time (s): " + elapsedTime/1000);
			   if (dfmm != null) {
				   System.out.println("dfMM: " + dfmm[0]);
				   System.out.println("dfMM plus: " + dfmm[1]);
				   System.out.println("dfMM minus: " + dfmm[2]);
			   }
			   
		   }catch (Exception e) {
			   e.printStackTrace();
		   }
		}
		
	
}
