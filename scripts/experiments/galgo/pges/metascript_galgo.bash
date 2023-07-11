#!/bin/bash

# CONSTANT VARIABLES
PROJECT_FOLDER="/home/jorlabs/projects/ParallelBNs"
PARAMS_FOLDER="${PROJECT_FOLDER}/res/params/"
SCRIPT="${PROJECT_FOLDER}/scripts/experiments/galgo/run_experiments_from_jar.bash"
JAR_FILE="${PROJECT_FOLDER}/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
# Ask if the user wants to compile and package
read -p "Build singularity image?(Y/N)" BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

# DEFINING HYPERPARAMS FOR JAR_FILE
PARAMS2="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_2.txt"
PARAMS4="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_4.txt"
PARAMS8="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_8.txt"
PARAMS16="${PARAMS_FOLDER}pges-jc/hyperparams_pges-jc_16.txt"
PARAMS_FGES="${PARAMS_FOLDER}fges/hyperparams_fges.txt"

# DEFINING SAVE FOLDERS
SAVE_FOLDER_CLUSTERING="${PROJECT_FOLDER}/results/galgo/joint-clustering/yes-updateedges-fes/speedup/"
SAVE_FOLDER_FGES="${PROJECT_FOLDER}/results/galgo/fges/"

echo "Adding experiments to qsub"

# PGES-JC EXPERIMENTS
qsub -N pGES-JC2-speedUp -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS2",JAR_FILE="$JAR_FILE",THREADS="2",SAVE_FOLDER="$SAVE_FOLDER_CLUSTERING" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N pGES-JC4-speedUp -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS4",JAR_FILE="$JAR_FILE",THREADS="4",SAVE_FOLDER="$SAVE_FOLDER_CLUSTERING" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N pGES-JC8-speedUp -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS8",JAR_FILE="$JAR_FILE",THREADS="8",SAVE_FOLDER="$SAVE_FOLDER_CLUSTERING" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N pGES-JC16-speedUp -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS16",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER_CLUSTERING" -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"


# FGES
#qsub -N Fges-1 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="1",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=1:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-2 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="2",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-4 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="4",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-8 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="8",SAVE_FOLDER="$SAVE_FOLDER_FGES" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N Fges-16 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",JAR_FILE="$JAR_FILE",THREADS="16",SAVE_FOLDER="$SAVE_FOLDER_FGES"   -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"

