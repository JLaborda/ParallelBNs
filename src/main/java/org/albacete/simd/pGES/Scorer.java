package org.albacete.simd.pGES;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.LocalScoreCache;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.ProbUtils;

@SuppressWarnings("DuplicatedCode")
public class Scorer {
	
	private DataSet data = null;
	
    /**
     * List of variables in the data set, in order.
     */
    private List<Node> variables;
    
    /**
     * Array of variable names from the data set, in order.
     */
    private String[] varNames;

    /**
     * Caches scores for discrete search.
     */
    private final LocalScoreCache localScoreCache = new LocalScoreCache();
    private static int numTotalCalls=0;
    private int numNonCachedCalls=0;

    private int[][] cases;
    private int[] nValues;
    /**
     * For discrete data scoring, the sample prior.
     */
    private double samplePrior;
	 /**
     * For discrete data scoring, the structure prior.
     */
    private double structurePrior;

    
    
 
    private void setDataSet(DataSet dataSet) {
        List<String> _varNames = dataSet.getVariableNames();

        this.data = dataSet;
        this.varNames = _varNames.toArray(new String[0]);
        this.variables = dataSet.getVariables();
    }

    private void initialize() {
        setStructurePrior(0.001);
        setSamplePrior(10.0);
    }
	
	public Scorer(DataSet dataSet) {
		setDataSet(dataSet);
		cases=new int[dataSet.getNumRows()][dataSet.getNumColumns()];
		for(int i=0;i<dataSet.getNumRows();i++) {
        	for(int j=0;j<dataSet.getNumColumns();j++) {
        		cases[i][j]=dataSet.getInt(i, j);
        	}
		}
        nValues=new int[dataSet.getNumColumns()];
        for(int i=0;i<dataSet.getNumColumns();i++)
        	nValues[i]=((DiscreteVariable)dataSet.getVariable(i)).getNumCategories();
        initialize();
	}
	
	
    private List<Node> getVariables() {
        return variables;
    }
	
    public void setStructurePrior(double structurePrior) {
        this.structurePrior = structurePrior;
    }

    public void setSamplePrior(double samplePrior) {
        this.samplePrior = samplePrior;
    }

    
    public double getSamplePrior() {
        return samplePrior;
    }
    
    public double getStructurePrior() {
        return structurePrior;
    }
    
	public double scoreGraph(Graph graph) {
//      Graph dag = SearchGraphUtils.dagFromPattern(graph);
      Graph dag = new EdgeListGraph(graph);
      SearchGraphUtils.pdagToDag(dag);
      double score = 0.;

      for (Node next : dag.getNodes()) {
          Collection<Node> parents = dag.getParents(next);
          int nextIndex = -1;
          for (int i = 0; i < getVariables().size(); i++) {
              if (varNames[i].equals(next.getName())) {
                  nextIndex = i;
                  break;
              }
          }
          int[] parentIndices = new int[parents.size()];
          Iterator<Node> pi = parents.iterator();
          int count = 0;
          while (pi.hasNext()) {
              Node nextParent = pi.next();
              for (int i = 0; i < getVariables().size(); i++) {
                  if (varNames[i].equals(nextParent.getName())) {
                      parentIndices[count++] = i;
                      break;
                  }
              }
          }
          score += localBdeuScore(nextIndex, parentIndices);
      }
      return score;
  }
	
	@SuppressWarnings("DuplicatedCode")
    protected double localBdeuScore(int nNode, int[] nParents) {
    	numTotalCalls++;
     	double oldScore = localScoreCache.get(nNode, nParents);
     	if (!Double.isNaN(oldScore)) {
     		return oldScore;
     	}
     	numNonCachedCalls++;
		int numValues=nValues[nNode];
		int numParents=nParents.length;
		
		double ess=getSamplePrior();
		double kappa=getStructurePrior();
		
		int[] numValuesParents=new int[nParents.length];
		int cardinality=1;
		for(int i=0;i<numValuesParents.length;i++) {
			numValuesParents[i]=nValues[nParents[i]];
			cardinality*=numValuesParents[i];
		}
		
		int[][] Ni_jk = new int[cardinality][numValues];
		double Np_ijk = (1.0*ess) / (numValues*cardinality);
		double Np_ij = (1.0*ess) / cardinality;
		
		// initialize
		for (int j = 0; j < cardinality;j++)
			for(int k= 0; k<numValues; k++)
				Ni_jk[j][k] = 0;

        for (int[] aCase : cases) {
            int iCPT = 0;
            for (int iParent = 0; iParent < numParents; iParent++) {
                iCPT = iCPT * numValuesParents[iParent] + aCase[nParents[iParent]];
            }
            Ni_jk[iCPT][aCase[nNode]]++;
        }

		double fLogScore = 0.0;

		for (int iParent = 0; iParent < cardinality; iParent++) {
            double N_ij = 0;
            double N_ijk = 0;

            for (int iSymbol = 0; iSymbol < numValues; iSymbol++) {
                if (Ni_jk[iParent][iSymbol] != 0) {
                	N_ijk = Ni_jk[iParent][iSymbol];
                    fLogScore += ProbUtils.lngamma(N_ijk + Np_ijk);
                    fLogScore -= ProbUtils.lngamma(Np_ijk);
                    N_ij += N_ijk;
                }
            }
            if (Np_ij != 0)
                fLogScore += ProbUtils.lngamma(Np_ij);
            if (Np_ij + N_ij != 0)
                fLogScore -= ProbUtils.lngamma(Np_ij + N_ij);
		}
        fLogScore += Math.log(kappa) * cardinality * (numValues - 1);
        
        localScoreCache.add(nNode, nParents, fLogScore);
		return fLogScore;
	}


}
