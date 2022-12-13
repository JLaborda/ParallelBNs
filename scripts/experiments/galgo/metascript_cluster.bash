#!/bin/bash

FILE="$HOME/ParallelBNs/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
SCRIPT="$HOME/ParallelBNs/scripts/galgo.bash"

PARAMS_FOLDER="$HOME/ParallelBNs/res/params/";

PARAMS2="${PARAMS_FOLDER}hyperparams2.txt";
PARAMS4="${PARAMS_FOLDER}hyperparams4.txt";
PARAMS8="${PARAMS_FOLDER}hyperparams8.txt";
PARAMS16="${PARAMS_FOLDER}hyperparams16.txt";

PARAMS_GES="${PARAMS_FOLDER}hyperparams_ges.txt";
PARAMS_FGES="${PARAMS_FOLDER}hyperparams_fges.txt";

#qsub -N pGES-2 -J 0-1077 -v CWD="$PWD",PARAMS="$PARAMS2",FILE="$FILE",THREADS="2" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-4 -J 0-1077 -v CWD="$PWD",PARAMS="$PARAMS4",FILE="$FILE",THREADS="4" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-8 -J 0-1077 -v CWD="$PWD",PARAMS="$PARAMS8",FILE="$FILE",THREADS="8" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
#qsub -N pGES-16 -J 0-1077 -v CWD="$PWD",PARAMS="$PARAMS16",FILE="$FILE",THREADS="16" -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"

#qsub -N GES-1 -J 0-153 -v CWD="$PWD",PARAMS="$PARAMS_GES",FILE="$FILE",THREADS="1" -l select=1:ncpus=1:mem=10gb:cluster=galgo2 "$SCRIPT"

qsub -N Fges-1 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",FILE="$FILE",THREADS="1" -l select=1:ncpus=1:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N Fges-2 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",FILE="$FILE",THREADS="2" -l select=1:ncpus=2:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N Fges-4 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",FILE="$FILE",THREADS="4" -l select=1:ncpus=4:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N Fges-8 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",FILE="$FILE",THREADS="8" -l select=1:ncpus=8:mem=10gb:cluster=galgo2 "$SCRIPT"
qsub -N Fges-16 -J 0-461 -v CWD="$PWD",PARAMS="$PARAMS_FGES",FILE="$FILE",THREADS="16" -l select=1:ncpus=16:mem=10gb:cluster=galgo2 "$SCRIPT"
