#!/bin/bash

PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs/"
JAR_PATH="${PROJECT_FOLDER}target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"
#SAVE_FOLDER="${PROJECT_FOLDER}/results/galgo/clustering/"

#echo "Arguments: $@"

if [ -z "$PBS_ARRAY_INDEX" ]; then PBS_ARRAY_INDEX=$1;  fi
if [ -z "$PARAMS" ]; then PARAMS=$2; fi
if [ -z "$THREADS" ]; then THREADS=$3; fi
if [ -z "$JAR_FILE" ]; then JAR_FILE=$4; fi
if [ -z "$SAVE_FOLDER" ]; then SAVE_FOLDER=$5; fi
#if [ -z "$CWD" ]; then CWD=PROJECT_FOLDER; fi

echo "--------------------------------------------------"
echo "FROM run_experiments_from_jar.bash"
echo "Running experiment with index: $PBS_ARRAY_INDEX, params: $PARAMS, threads: $THREADS, JAR_file: $JAR_FILE, save_folder: $SAVE_FOLDER"
echo "--------------------------------------------------"

# Run experiment
#cd $CWD
$JAVA_BIN -Djava.util.concurrent.ForkJoinPool.common.parallelism=$THREADS -jar ${JAR_FILE} ${PBS_ARRAY_INDEX} ${PARAMS} ${THREADS} ${SAVE_FOLDER}