#!/bin/bash
#PBS -l mem=16gb
#PBS -o /home/jdls/developer/projects/ParallelBNs/parallelBNs-output
#PBS -e /home/jdls/developer/projects/ParallelBNs/parallelBNs-error

#docker run -v /home/jdls/developer/projects/ParallelBNs/res:/parallelbns/res -v /home/jdls/developer/projects/ParallelBNs/results:/parallelbns/results --rm parallelbns scripts/experiments.bash $PBS_ARRAY_INDEX $PARAMS

singularity run -B /home/jdls/developer/projects/ParallelBNs/res:/res -B /home/jdls/developer/projects/ParallelBNs/results:/results /home/jdls/developer/projects/ParallelBNs/parallelbns_feature-experiments.sif $PBS_ARRAY_INDEX $PARAMS

#-v /Users/jdls/developer/projects/ParallelBNs/experiments:/parallelBNs/experiments