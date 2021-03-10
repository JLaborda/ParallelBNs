#!/bin/sh

declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare -a suffixes=("01" "02" "03" "04" "05" "06")
PARAMS_FOLDER="/home/jorlabs/projects/ParallelBNs-git/ParallelBNs/experiments/parameters/"
SCRIPT="/home/jorlabs/projects/ParallelBNs-git/ParallelBNs/scripts/experiments.bash"
for net in ${net_names[@]}; do
    for suffix in ${suffixes[@]}; do
        params="${PARAMS_FOLDER}${net}/${net}_params_${suffix}"
        qsub -v param=${params} ${SCRIPT}
    done
done