#!/bin/bash

PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs"
PARAMS_FOLDER="${PROJECT_FOLDER}/res/params/"
NET_NAMES=(munin link andes pigs hailfinder) #(alarm andes win95pts)
#NET_NAMES=(andes)
#HYPERPARAMS_PATHS PATHS
#HYPERPARAMS_PATHS=()
#for NET in "${NET_NAMES[@]}"; do
#  HYPERPARAMS_PATHS+=("${PARAMS_FOLDER}hyperparams_${NET}_pges.txt")
#done

#echo "${hyperparams[@]}"

SCRIPT="${PROJECT_FOLDER}/scripts/experiments/galgo/run_experiments_from_jar.bash"
FILE="${PROJECT_FOLDER}/target/ParallelBNs-1.0-SNAPSHOT-jar-with-dependencies.jar"
echo "Adding experiments to qsub"

# Ask if the user wants to compile and package
read -p "Build singularity image?(Y/N)" BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

# DEFINING HYPERPARAMS FILE
PARAMS2="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_2.txt"
PARAMS4="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_4.txt"
PARAMS8="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_8.txt"
PARAMS16="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_16.txt"

qsub -N pGES-JC2 -J 0-1 -v CWD="$PWD",PARAMS="$PARAMS2",FILE="$FILE",THREADS="2" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-JC4 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS4",FILE="$FILE",THREADS="4" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-JC8 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS8",FILE="$FILE",THREADS="8" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-JC16 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS16",FILE="$FILE",THREADS="16" -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"


# for NET in "${NET_NAMES[@]}"; do
#     params="${PARAMS_FOLDER}hyperparams_${NET}_pges-jc.txt"
#     #qsub -J 0-329 -v PARAMS="$params" "$SCRIPT"				  	# 1 Hilo
#     qsub -N pges_clust2 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=2 "$SCRIPT"    	# 2 Hilos
#     qsub -N pges_clust4 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=4 "$SCRIPT"    	# 4 Hilos
#     qsub -N pges_clust8 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=8 "$SCRIPT"    	# 8 Hilos
#     qsub -N pges_clust16 -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=16 "$SCRIPT"    	# 16 Hilos
# done