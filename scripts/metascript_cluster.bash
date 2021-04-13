#!/bin/bash

#declare -a net_names=("andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")

# declare -a suffixes=("01" "02" "03" "04" "05" "06")
#PARAMS_FOLDER="res/params/"

#echo "building docker images in all of the nodes of the cluster..."
#build-image parallelbns /home/jdls/developer/projects/ParallelBNs/Dockerfile
#echo "build complete"

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/run_experiments_cluster.bash"
for net in ${net_names[@]}; do
    #for suffix in ${suffixes[@]}; do
    PARAMS="/home/jdls/developer/projects/ParallelBNs/res/params/experiments_${net}.txt" #${net}/${net}_params_${suffix}"
    LENGTH=$(cat ${PARAMS} | wc -l)
    PARAMS="/parallelbns/res/params/experiments_${net}.txt"
    #qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH -l nodes=1:ppn=8 $SCRIPT
    qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH $SCRIPT
    #done
done