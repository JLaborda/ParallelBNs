#!/bin/sh

declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare -a suffixes=("01" "02" "03" "04" "05" "06")
PARAMS_FOLDER="experiments/parameters/"
SCRIPT="scripts/run_experiments_cluster.bash"
for net in ${net_names[@]}; do
    for suffix in ${suffixes[@]}; do
        PARAMS="${PARAMS_FOLDER}${net}/${net}_params_${suffix}"
        LENGTH=$(cat ${PARAMS} | wc -l)
        qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH $SCRIPT
        done