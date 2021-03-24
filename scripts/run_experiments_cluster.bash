#!/bin/bash
#PBS -l mem=16gb
#PBS -o parallelBNs-output
#PBS -e parallelBNs-error
docker build -t parallelbns -f /Users/jdls/developer/projects/ParallelBNs/Dockerfile .
#docker run --rm parallelbns $PBS_ARRAY_INDEX $PARAMS
docker run -v $(PWD)/res:/parallelbns/res -v $(PWD)/results:/parallelbns/results --rm parallelbns scripts/experiments.bash 1 /parallelbns/res/params/experiments_alarm.txt
#-v /Users/jdls/developer/projects/ParallelBNs/experiments:/parallelBNs/experiments