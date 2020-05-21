#!/bin/bash
#PBS -N parallelBN
#PBS -l mem=8gb
#PBS -o parallel-output
#PBS -e parallel-error

# Change directory to the job submission directory
cd $PBS_O_WORKDIR

MAIN_PATH="/home/frubio/jorge"
PARAMS="${MAIN_PATH}/unexperimento.txt"
JAVA_BIN="/home/frubio/java/jdk1.8.0_45/bin/java"
JAR_PATH="${MAIN_PATH}/ParallelGES/Fusion/libraries/"
SOURCE_PATH="${JAR_PATH}weka.jar:${JAR_PATH}tetrad/tetrad.jar:${JAR_PATH}tetrad-libs/"
CLASSPATH="${MAIN_PATH}/ParallelGES/Fusion/bin/"

## Load parameters:
#Y="$PBS_ARRAY_INDEX"
Y=1
linea=`awk -v line=$Y 'NR==line {print $0}' $PARAMS`
col=1
for i in $linea
do
	params[$col]=$i
	col=`expr $col + 1`
done

nCols=`expr $col - 1`
nColsMenos1=`expr $nCols - 1`
## End load parameters

## Map Parameters:
NETWORK=${params[1]}
DATASET=${params[2]}
FUSION=${params[3]}
NTHREADS=${params[4]}
NITINTERLEAVING=${params[5]}

#$JAVA_BIN -classpath $CLASSPATH:$SOURCE_PATH parallelGES.LaunchExperiment $NETWORK $DATASET $FUSION $NTHREADS $NITINTERLEAVING
