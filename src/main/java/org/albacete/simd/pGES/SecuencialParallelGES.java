package org.albacete.simd.pGES;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import consensusBN.ConsensusBES;
import consensusBN.HeuristicConsensusMVoting;
import edu.cmu.tetrad.bayes.MlBayesIm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
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
public class SecuencialParallelGES {
	
	DataSet data;
	int nThreads;
	int nItInterleaving;
	int maxIterations = 15;
	DataSet[] samples;
	ThGES[] search;
	ArrayList[] subSets;
	ArrayList<Dag> graphs = null;
	Graph currentGraph = null;
	Graph previousGraph = null;
	
	
	public SecuencialParallelGES(DataSet data,  int nThreads, int nItInterleaving) {
		super();
		this.data = data;
		this.nThreads = nThreads;
		this.nItInterleaving = nItInterleaving;
		this.samples = new DataSet[this.nThreads];
		this.search = new ThGES[this.nThreads];
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
			this.samples[i] =  DataUtils.getBootstrapSample(this.data, this.data.getNumRows());
			this.search[i] = new ThGES(this.samples[i],this.subSets[i],this.nItInterleaving);
		}
		
		
	}
	
	private boolean convergence(int it) {
		return (this.previousGraph == this.currentGraph) || (it > this.maxIterations);
	}
	
	public void search(){
		
		boolean change = true;
		int it = 0;
		while(change){
			System.out.println("Iteration: " + it);
			
			// Saving previous graph
			if (this.currentGraph != null)
				this.previousGraph = new EdgeListGraph(this.currentGraph);

			this.graphs = new ArrayList<>();
			for(int i = 0 ; i< this.nThreads; i++){
				Graph g = this.search[i].search();
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
			// Aquí estaba puesto el ConsensusBES, que es más conservador y por tanto borra mucho más. (Borrar mucho es malo)
			HeuristicConsensusMVoting fusion = new HeuristicConsensusMVoting (this.graphs,0.25);
			fusion.fusion();
			this.currentGraph = fusion.getFusion();
			for(int i=0; i< this.nThreads; i++){
				this.search[i].setInitialGraph(this.currentGraph);
			}
			if (convergence(it)) change = false;
			it++;
		}
		System.out.println("Total Iterations: " + it);
	}
	
	public static void main(String[] args) {
	
		   try {
			   long startTime = System.currentTimeMillis();
			   BIFReader bf = new BIFReader();
			   bf.processFile("networks/pigs.xbif");
			   BayesNet bn = (BayesNet) bf;


			   System.out.println("Numero de variables: "+bn.getNrOfNodes());
			   MlBayesIm bn2 = new MlBayesIm(bn);
			   DataReader reader = new DataReader();
			   reader.setDelimiter(DelimiterType.COMMA);
			   reader.setMaxIntegralDiscrete(100);
			   DataSet dataSet = reader.parseTabular(new File("networks/BBDD/pigs.xbif_.csv"));
			   SecuencialParallelGES alg = new SecuencialParallelGES(dataSet,4,5);
			   alg.search();
			   long endTime = System.currentTimeMillis();
			   long elapsedTime = endTime - startTime;
			   int shd = compare(bn2.getDag(),(Dag) alg.currentGraph);
			   System.out.println("SHD: "+shd);
			   System.out.println("Total execution time (s): " + elapsedTime/1000);
		   } catch (Exception e) {
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

}
