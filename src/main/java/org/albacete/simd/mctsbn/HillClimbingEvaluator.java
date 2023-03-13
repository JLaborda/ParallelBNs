package org.albacete.simd.mctsbn;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.BDeuScore;
import org.albacete.simd.utils.Problem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.ArrayUtils;

public class HillClimbingEvaluator {

    private final Problem problem;
    
    private final ConcurrentHashMap<String,Double> localScoreCache;

    private final List<Integer> order;

    private Graph graph;

    private double finalScore = 0;

    private final static int MAX_ITERATIONS = 1000;

    private final BDeuScore metric;


    public HillClimbingEvaluator(Problem problem, List<Node> order, ConcurrentHashMap<String,Double> localScoreCache){
        this.problem = problem;
        this.localScoreCache = localScoreCache;
        this.order = new ArrayList(order.size());
        for (Node node : order) {
            this.order.add(problem.getHashIndices().get(node));
        }
        this.graph = new EdgeListGraph_n();
        metric = new BDeuScore(problem.getData());
    }



    private Set<Integer> evaluate(int child, Set<Integer> candidates){
        int iteration = 0;

        //System.out.println("\n-------------- " + child + " --------------");
        
        Set<Integer> parents = new HashSet<>();

        while(iteration < MAX_ITERATIONS) {
            //System.out.println("\nITERATION " + iteration);
            AtomicReference<Double> bestScore = new AtomicReference<>(Double.NEGATIVE_INFINITY);
            AtomicInteger bestParent = new AtomicInteger();
            
            // Generate parents array
            Integer[] parentsArr = new Integer[parents.size()];
            parents.toArray(parentsArr);
            Arrays.sort(parentsArr);

            candidates.parallelStream().forEach(candidate -> {
                double score;

                // OPERATION ADD
                if (!parents.contains(candidate)) {
                    //System.out.println("ADD: " + candidate + " -> " + child + ", " + parents);
                    score = getAdditionScore(child, candidate, new HashSet(parents), parentsArr);
                }
                    
                // OPERATION DELETE
                else {
                    //System.out.println("DELETE: " + candidate + " -> " + child + ", " + parents);
                    score = getDeleteScore(child, candidate, new HashSet(parents), parentsArr);
                }
                if(score > bestScore.get()){
                    bestScore.set(score);
                    bestParent.set(candidate);
                }
                 
            });
            
            // Updating graph
            if(bestScore.get() > 0){
                int bp = bestParent.get();
                if(parents.contains(bp)) {
                    parents.remove(bp);
                    //System.out.println("   BEST PARENT DEL: " + bp + ", SCORE: " + bestScore.get());
                }
                else {
                    parents.add(bp);
                    //System.out.println("   BEST PARENT ADD: " + bp + ", SCORE: " + bestScore.get());
                }
                iteration++;
            } 
            else {
                //System.out.println("     FIN DE LA ITERACIÓN: " + bestParent.get() + ", SCORE: " + bestScore.get());
                break;
            }
        }
        
        return parents;
    }

    
    private double getAdditionScore(int indexChild, int indexParent, Set<Integer> parents, Integer[] indexParents) {
        // Creating an array adding the index of the parent
        int[] indexUnion = new int[indexParents.length + 1];
        for (int i = 0; i < indexParents.length; i++) {
            indexUnion[i] = indexParents[i];
        }
        indexUnion[indexUnion.length - 1] = indexParent;
        parents.add(indexParent);
        if (indexUnion.length > 1)
            Arrays.sort(indexUnion);
        
        double scorePart1 = localBdeuScore(indexChild, indexUnion, parents);
        
        // Removing again the parent to the set
        parents.remove(indexParent);
        double scorePart2 = localBdeuScore(indexChild, ArrayUtils.toPrimitive(indexParents), parents);
        
        // Score = localbdeu(x,P(G) + {x_p}) - localbdeu(x,P(G))
        return scorePart1 - scorePart2;
    }
    
    
    private double getDeleteScore(int indexChild, int indexParent, Set<Integer> parents, Integer[] indexParents) {
        // Calculating indexes for the difference set of parents
        parents.remove(indexParent);    
        Integer[] indexParentsAux = new Integer[parents.size()];
        parents.toArray(indexParentsAux);
        if (indexParentsAux.length > 1)
            Arrays.sort(indexParentsAux);

        double scorePart1 = localBdeuScore(indexChild, ArrayUtils.toPrimitive(indexParentsAux), parents);
        
        // Adding again the parent to the set
        parents.add(indexParent);
        double scorePart2 = localBdeuScore(indexChild, ArrayUtils.toPrimitive(indexParents), parents);
        
        // Score = localbdeu(x,P(G) - {x_p}) - localbdeu(x,P(G))
        return scorePart1 - scorePart2;
    }


    public double localBdeuScore(int nNode, int[] nParents, Set<Integer> setParents) {
        Double oldScore = localScoreCache.get("" + nNode + Arrays.toString(nParents));
        problem.counter.getAndIncrement();

        if (oldScore != null) {
            return oldScore;
        }
        
        problem.counterSinDict.getAndIncrement();
        
        double fLogScore = metric.localScore(nNode, nParents);
        localScoreCache.put("" + nNode + Arrays.toString(nParents), fLogScore);
        
        return fLogScore;
    }
    

    public double search(){
        double c1 = problem.counter.get();
        double c2 = problem.counterSinDict.get();
        
        graph = new EdgeListGraph_n(problem.getVariables());
        
        Set<Integer> candidates = new HashSet<>();
        for (int node : order) {
            Set<Integer> parents = evaluate(node, candidates);
            for (int parent : parents) {
                Edge edge = Edges.directedEdge(problem.getNode(parent), problem.getNode(node));
                graph.addEdge(edge);
            }
            
            Integer[] arr = parents.toArray(new Integer[0]);
            finalScore += localBdeuScore(node, ArrayUtils.toPrimitive(arr), parents);
            
            candidates.add(node);
        }

        System.out.println("\n TOTAL CALCULATIONS:  " + (problem.counter.get() - c1));
        System.out.println(" TOTAL CALCULATIONS no dictionary:  " + (problem.counterSinDict.get() - c2));
        System.out.println(" SIZE OF CONCURRENT: " + localScoreCache.size());
        
        return finalScore;
    }
    

    public double getScore() {
        return finalScore;
    }

    public Graph getGraph() {
        return graph;
    }
}
