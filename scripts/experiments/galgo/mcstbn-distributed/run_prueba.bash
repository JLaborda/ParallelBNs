#!/bin/bash

PROJECT_PATH="/home/jorlabs/projects/ParallelBNs"
SCRIPT="/home/jorlabs/projects/ParallelBNs/scripts/experiments/galgo/mcstbn-distributed/run_experiments_from_jar.bash"

read -p "Run mvn clean package?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

INDEX=1
PARAMS="/home/jorlabs/projects/ParallelBNs/res/params/mctsbn-distributed/parameters-failed.txt"
echo "--------------------------------------------------"
echo "FROM run_prueba.bash"
echo "Running experiment with index: $INDEX, params: $PARAMS,"
echo "--------------------------------------------------"

bash ${SCRIPT} ${INDEX} ${PARAMS}