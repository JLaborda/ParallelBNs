#!/bin/bash
#PBS -N parallelBNs
#PBS -l mem=16gb
#PBS -o parallelBNs-output
#PBS -e parallelBNs-error
#PBS -J 1-10000

# Change directory to the job submission directory
#cd $PBS_O_WORKDIR

MAIN_PATH="/home/jorlabs/projects/ParallelBNs-git/ParallelBNs"
PARAMS=$2 #${params} #"${MAIN_PATH}/experiments/params/alarm/experiments_alarm.txt"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"
LIB_PATH="${MAIN_PATH}/src/lib/"
SOURCE_PATH="${LIB_PATH}colt.jar:${LIB_PATH}commons-collections-3.1.jar:${LIB_PATH}f2jutil.jar:${LIB_PATH}fusion.jar:${LIB_PATH}Jama-1.0.2.jar:${LIB_PATH}jdepend.jar:${LIB_PATH}jh.jar:${LIB_PATH}junit.jar:${LIB_PATH}jxl.jar:${LIB_PATH}mrjtoolkitstubs.jar:${LIB_PATH}mtj.jar:${LIB_PATH}opt77.jar:${LIB_PATH}pal-1.4.jar:${LIB_PATH}weka.jar:${LIB_PATH}xom-1.1.jar"
CLASSPATH="${MAIN_PATH}/target/classes/"


# $JAVA_BIN -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.RunExperiments

#echo ${PARAMS}

## Load parameters:
Y=$1 #"$PBS_ARRAY_INDEX" # This was the original
#Y=41

#echo "PBS_ARRAY_INDEX = $PBS_ARRAY_INDEX"
linea=`awk -v line=$Y 'NR==line {print $0}' $PARAMS`
col=0
#echo "Hello"
#echo $linea
for i in $linea
do
	params[$col]=$i
	#echo ${params[$col]}
	col=`expr $col + 1`
done

nCols=`expr $col - 1`
nColsMenos1=`expr $nCols - 1`
## End load parameters

## Map Parameters:
ALG=${params[0]}
#echo "$ALG"
NETWORK=${params[1]}
DATASET=${params[2]}
TEST=${params[3]}
NITINTERLEAVING=${params[4]}
if [ "$ALG" == "hc" ]; then
	#NTHREADS=${params[3]}
	MAXITERATIONS=${params[5]}
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $ALG $NETWORK $DATASET $TEST $NITINTERLEAVING $MAXITERATIONS

elif [ "$ALG" == "ges" ]; then
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $ALG $NETWORK $DATASET $TEST $NITINTERLEAVING

else
	NTHREADS=${params[5]}
	MAXITERATIONS=${params[6]}
	SEED=${params[7]}
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $ALG $NETWORK $DATASET $TEST $NITINTERLEAVING $NTHREADS $MAXITERATIONS $SEED
fi

