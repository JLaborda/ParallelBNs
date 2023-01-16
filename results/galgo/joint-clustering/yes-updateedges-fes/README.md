# Experiments for joint-clustering with update in the subset of edges in the FES stage
Author: Jorge Daniel Laborda
Date: 10th of January 2023

## Description
These experiments use the new parallelized pges that executes much faster and uses the new joint clustering. It also updates the subset of edges of each FESThread so that it only considers the edges neighbouring the newest added edge as well as the transformation of revertToCPDAG if there is no speedUp. The only difference when using the speedup is that it doesn't use the method revertToCPDAG when updating the edges.