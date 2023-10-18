#!/bin/bash

JAR_PATH="/home/jorlabs/projects/ParallelBNs/target/mctsbn-distributed-1.0-jar-with-dependencies.jar"
JAVA_BIN=java #"/home/jorlabs/java/jdk1.8.0_251/bin/java"
#SAVE_FOLDER="${PROJECT_FOLDER}/results/galgo/clustering/"

#echo "Arguments: $@"

if [ -z "$PBS_ARRAY_INDEX" ]; then PBS_ARRAY_INDEX=$1;  fi
if [ -z "$PARAMS" ]; then PARAMS=$2; fi
if [ -z "$JAR_PATH" ]; then JAR_PATH=$3; fi
#if [ -z "$CWD" ]; then CWD=PROJECT_FOLDER; fi

echo "--------------------------------------------------"
echo "FROM run_experiments_from_jar.bash"
echo "Running experiment with index: $PBS_ARRAY_INDEX, params: $PARAMS, JAR_file: $JAR_FILE"
echo "--------------------------------------------------"

# Run experiment
# cd $CWD
# -Djava.util.concurrent.ForkJoinPool.common.parallelism=$THREADS 
$JAVA_BIN -Xmx32g -jar ${JAR_PATH} ${PBS_ARRAY_INDEX} ${PARAMS}