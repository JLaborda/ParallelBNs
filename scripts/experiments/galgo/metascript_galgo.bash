#!/bin/bash

# CONSTANT VARIABLES
PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs"
PARAMS_FOLDER="${PROJECT_FOLDER}/res/params/"
SCRIPT="${PROJECT_FOLDER}/scripts/experiments/galgo/run_experiments_from_jar.bash"
JAR_FILE="${PROJECT_FOLDER}/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
# Ask if the user wants to compile and package
read -p "Build project? (Y/N)" BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

# DEFINING HYPERPARAMS FOR JAR_FILE

PARAMS="/home/jorlabs/projects/ParallelBNs/res/params/large_datasets/pges_params.txt"
# Contar las líneas
LINE_COUNT=$(wc -l < "$PARAMS")

# DEFINING SAVE FOLDERS
SAVE_FOLDER="/home/jorlabs/projects/ParallelBNs/results/galgo/large_databases2/"
echo "Adding experiments to qsub"

# PC Experiments
qsub -N pges-large -J 0-$LINE_COUNT -v CWD="$PWD",PARAMS="$PARAMS",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER" -l select=1:ncpus=2:mem=32gb:cluster=galgo2 "$SCRIPT"

#qsub -N pc -J 0-$LINE_COUNT -v CWD="$PWD",PARAMS="$PARAMS",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER" -l select=1:ncpus=2:mem=64gb:cluster=galgo2 "$SCRIPT"


# FGES
#qsub -N Fges-1 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="1",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=1:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-2 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="2",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-4 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="4",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-8 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="8",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-16 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER_FGES"   -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"

