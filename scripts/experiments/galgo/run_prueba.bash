#!/bin/bash

PROJECT_PATH="/home/jorlabs/projects/ParallelBNs"
SCRIPT="${PROJECT_PATH}/scripts/experiments/galgo/run_experiments_from_jar.bash"

read -p "Build singularity image?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

INDEX=3
PARAMS="/home/jorlabs/projects/ParallelBNs/res/params/pges-jc/hyperparams_pges-jc_2.txt"
THREADS=8
SAVE_FOLDER="/home/jorlabs/projects/ParallelBNs/results/pruebas/"
FILE="${PROJECT_PATH}/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"

echo "--------------------------------------------------"
echo "FROM run_prueba.bash"
echo "Running experiment with index: $INDEX, params: $PARAMS, threads: $THREADS, file: $FILE"
echo "--------------------------------------------------"

bash ${SCRIPT} ${INDEX} ${PARAMS} ${THREADS} ${FILE} ${SAVE_FOLDER}