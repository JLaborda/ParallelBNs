# Experiments for joint-clustering with no update in the subset of edges in the FES stage
Author: Jorge Daniel Laborda
Date: 10th of January 2023

## Description
These experiments use the new parallelized pges that executes much faster and uses the new joint clustering. However, since there is still issues deleting the unnecesary edges from the subset in the FES stage, we don't update the subset of edges for each thread of FES. This approach will be done later on.