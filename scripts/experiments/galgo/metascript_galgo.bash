#!/bin/bash

PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs"
PARAMS_FOLDER="${PROJECT_FOLDER}/res/params/"
#NET_NAMES=(munin link andes pigs hailfinder) #(alarm andes win95pts)
NET_NAMES=(andes)
#HYPERPARAMS_PATHS PATHS
#HYPERPARAMS_PATHS=()
#for NET in "${NET_NAMES[@]}"; do
#  HYPERPARAMS_PATHS+=("${PARAMS_FOLDER}hyperparams_${NET}_pges.txt")
#done

#echo "${hyperparams[@]}"

SCRIPT="${PROJECT_FOLDER}/scripts/experiments/galgo/run_experiments_from_jar.bash"

echo "Adding experiments to qsub"

for NET in "${NET_NAMES[@]}"; do
    params="${PARAMS_FOLDER}hyperparams_${NET}_pges.txt"
    #qsub -J 0-329 -v PARAMS="$params" "$SCRIPT"				  	# 1 Hilo
    qsub -N pges_clust2 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=2 "$SCRIPT"    	# 2 Hilos
    qsub -N pges_clust4 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=4 "$SCRIPT"    	# 4 Hilos
    qsub -N pges_clust8 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=8 "$SCRIPT"    	# 8 Hilos
    qsub -N pges_clust16 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=16 "$SCRIPT"    	# 16 Hilos
done