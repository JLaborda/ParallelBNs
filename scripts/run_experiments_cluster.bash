#!/bin/bash
#PBS -l mem=16gb
#PBS -o parallelBNs-output
#PBS -e parallelBNs-error
docker build -t parallelBNs -f /home/jdls/developer/projects/ParallelBNs/Dockerfile
docker run --rm parallelbns $PBS_ARRAY_INDEX $PARAMS