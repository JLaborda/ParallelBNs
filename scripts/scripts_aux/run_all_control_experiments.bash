#!/bin/bash
#PBS -N parallelBNs
#PBS -l mem=8gb
#PBS -o parallel-output
#PBS -e parallel-error
#PBS -J 1-3456

# Change directory to the job submission directory
cd $PBS_O_WORKDIR

MAIN_PATH="/home/jorlabs/projects/ParallelBNs-git/ParallelBNs"
PARAMS="${MAIN_PATH}/scripts/scripts/prueba.txt"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"
LIB_PATH="${MAIN_PATH}/src/lib/"
SOURCE_PATH="${LIB_PATH}colt.jar:${LIB_PATH}commons-collections-3.1.jar:${LIB_PATH}f2jutil.jar:${LIB_PATH}fusion.jar:${LIB_PATH}Jama-1.0.2.jar:${LIB_PATH}jdepend.jar:${LIB_PATH}jh.jar:${LIB_PATH}junit.jar:${LIB_PATH}jxl.jar:${LIB_PATH}mrjtoolkitstubs.jar:${LIB_PATH}mtj.jar:${LIB_PATH}opt77.jar:${LIB_PATH}pal-1.4.jar:${LIB_PATH}weka.jar:${LIB_PATH}xom-1.1.jar"
CLASSPATH="${MAIN_PATH}/target/classes/"

cat $PARAMS

## Load parameters:
Y="$PBS_ARRAY_INDEX"
#Y=1
linea=`awk -v line=$Y 'NR==line {print $0}' $PARAMS`
col=0
for i in $linea
do
	params[$col]=$i
	col=`expr $col + 1`
done

nCols=`expr $col - 1`
nColsMenos1=`expr $nCols - 1`
## End load parameters

## Map Parameters:
NETWORK=${params[0]}
DATASET=${params[1]} 



$JAVA_BIN -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.Experiment "$MAIN_PATH/$NETWORK" "$MAIN_PATH/$DATASET"
