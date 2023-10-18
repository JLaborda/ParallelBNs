#!/bin/bash

# CONSTANT VARIABLES
PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs/"
PARAMS_FOLDER="${PROJECT_FOLDER}res/params/"
SCRIPT="${PROJECT_FOLDER}scripts/experiments/galgo/mcstbn-distributed/run_experiments_from_jar.bash"
JAR_PATH="${PROJECT_FOLDER}mctsbn-distributed-1.0-jar-with-dependencies.jar"

# Ask if the user wants to compile and package
read -p "Run mvn clean package?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

# DEFINING HYPERPARAMS FOR JAR_FILE
PARAMS="${PROJECT_FOLDER}/res/params/mctsbn-distributed/parameters-failed.txt"

echo "Adding experiments to qsub"

# PGES-JC EXPERIMENTS
qsub -N mctsbn-failed -J 0-7 -v PARAMS="$PARAMS",JAR_PATH="$JAR_PATH" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 "$SCRIPT"


# FGES
#qsub -N Fges-1 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="1",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=1:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-2 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="2",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-4 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="4",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-8 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="8",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-16 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER_FGES"   -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"

