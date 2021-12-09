#!/bin/bash
PROJECT_FOLDER="/home/jdls/developer/projects/ParallelBNs"


#singularity pull docker://jorlabs/parallelbns:feature-experiments
#singularity run -B ${PROJECT_FOLDER}/res:/res ${PROJECT_FOLDER}/results:/results --rm parallelbns alarm pges /res/networks/alarm.xbif /res/networks/BBDD/alarm.xbif_.csv /res/networks/BBDD/tests/alarm_test.csv 5 250 1 3
singularity run -B /home/jdls/developer/projects/ParallelBNs/res:/res -B /home/jdls/developer/projects/ParallelBNs/results:/results docker://jorlabs/parallelbns:feature-experiments alarm pges /res/networks/alarm.xbif /res/networks/BBDD/alarm.xbif_.csv /res/networks/BBDD/tests/alarm_test.csv 5 250 1 3


#docker pull jorlabs/parallelbns:feature-experiments 
#docker run -v ${PROJECT_FOLDER}/res:/res -v ${PROJECT_FOLDER}/results:/results --rm parallelbns alarm pges /res/networks/alarm.xbif /res/networks/BBDD/alarm.xbif_.csv /res/networks/BBDD/tests/alarm_test.csv 5 250 1 3

#singularity instance.start parallelbns exp1

# Metascript
# SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/run_experiments_cluster.bash"
# for net in ${net_names[@]}; do
#     #for suffix in ${suffixes[@]}; do
#     PARAMS="/home/jdls/developer/projects/ParallelBNs/res/params/experiments_${net}.txt" #${net}/${net}_params_${suffix}"
#     LENGTH=$(cat ${PARAMS} | wc -l)
#     PARAMS="/parallelbns/res/params/experiments_${net}.txt"
#     #qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH -l nodes=1:ppn=8 $SCRIPT
#     qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH $SCRIPT
#     #done
# done

# Docker Script
#docker run -v /home/jdls/developer/projects/ParallelBNs/res:/parallelbns/res -v /home/jdls/developer/projects/ParallelBNs/results:/parallelbns/results --rm parallelbns scripts/experiments.bash $PBS_ARRAY_INDEX $PARAMS
