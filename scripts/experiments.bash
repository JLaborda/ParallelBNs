#!/bin/bash

MAIN_PATH="/parallelbns" # "/home/jdls/developer/projects/ParallelBNs" #
PARAMS=$2 #"${MAIN_PATH}/res/params/experiments_alarm.txt"
JAVA_BIN=java #"/home/jorlabs/java/jdk1.8.0_251/bin/java"
LIB_PATH="${MAIN_PATH}/src/lib/"
SOURCE_PATH="${LIB_PATH}colt.jar:${LIB_PATH}commons-collections-3.1.jar:${LIB_PATH}f2jutil.jar:${LIB_PATH}fusion.jar:${LIB_PATH}Jama-1.0.2.jar:${LIB_PATH}jdepend.jar:${LIB_PATH}jh.jar:${LIB_PATH}junit.jar:${LIB_PATH}jxl.jar:${LIB_PATH}mrjtoolkitstubs.jar:${LIB_PATH}mtj.jar:${LIB_PATH}opt77.jar:${LIB_PATH}pal-1.4.jar:${LIB_PATH}weka.jar:${LIB_PATH}xom-1.1.jar"
CLASSPATH="${MAIN_PATH}/target/classes/"

#echo "Params are: "
#echo $PARAMS

#echo "First arg is: "
#echo $1

#echo "Second arg is: "
#echo $2

#echo Your container args are: "$@"


# $JAVA_BIN -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.RunExperiments

#echo ${PARAMS}

## Load parameters:
Y=$1 #"$PBS_ARRAY_INDEX" # This was the original
#Y=41

#echo "PBS_ARRAY_INDEX = $PBS_ARRAY_INDEX"
linea=`awk -v line=$Y 'NR==line {print $0}' $PARAMS`
col=0
#echo "Hello"
#echo $linea #> /parallelbns/results/linea.txt
for i in $linea
do
	params[$col]=$i
	#echo "Param ${col} es ${params[$col]}"
	col=`expr $col + 1`
done

nCols=`expr $col - 1`
nColsMenos1=`expr $nCols - 1`
## End load parameters

## Map Parameters:
NET_NAME=${params[0]}
#echo "Alg: $ALG"

ALG=${params[1]}
#echo "NETWORK: $NETWORK"

NET_PATH=${params[2]}

DATASET=${params[3]}
#echo "DATASET: $DATASET"

TEST=${params[4]}
#echo "TEST: $TEST"

NITINTERLEAVING=${params[5]}
echo "NITINTERLEAVING: $NITINTERLEAVING"

if [ "$ALG" == "hc" ]; then
	#NTHREADS=${params[3]}
	MAXITERATIONS=${params[6]}
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $NET_NAME $ALG $NET_PATH $DATASET $TEST $NITINTERLEAVING $MAXITERATIONS

elif [ "$ALG" == "ges" ]; then
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $NET_NAME $ALG $NET_PATH $DATASET $TEST $NITINTERLEAVING 

else
	NTHREADS=${params[6]}
	echo "NTHREADS: $NTHREADS"
	MAXITERATIONS=${params[7]}
	echo "MAXITERATIONS: $MAXITERATIONS"
	
	SEED=${params[8]}
	echo "SEED: $SEED"
	
	$JAVA_BIN -Xmx8g -classpath $CLASSPATH:$SOURCE_PATH org.albacete.simd.experiments.MainExperiment $NET_NAME $ALG $NET_PATH $DATASET $TEST $NITINTERLEAVING $NTHREADS $MAXITERATIONS $SEED
fi

