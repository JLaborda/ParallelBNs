#!/bin/bash
#PBS -l mem=16gb
#PBS -o parallelBNs-output
#PBS -e parallelBNs-error
docker build -t parallelbns -f /home/jdls/developer/projects/ParallelBNs/Dockerfile
#docker run --rm parallelbns $PBS_ARRAY_INDEX $PARAMS
docker run --rm -v /home/jdls/developer/projects/ParallelBNs/experiments:/parallelBNs/experiments parallelbns 1 /home/jdls/developer/projects/ParallelBNs/experiments/params/experiments_alarm.txt