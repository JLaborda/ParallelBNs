#!/bin/bash



PARAMS_FOLDER="/res/params/"
SINGULARITY_SCRIPT_PATH="/home/jdls/developer/projects/ParallelBNs/scripts/singularity_scripts/create_singularity_image_from_dockerfile.bash"


bash ${SINGULARITY_SCRIPT_PATH}

declare -a hyperparams=( ${PARAMS_FOLDER}"hyperparams_alarm_pges.txt" )
#echo "${hyperparams[@]}"

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/experiments/run_experiments_cluster.bash"
#EL BUENO
echo "Adding experiment to qsub"
qsub -v PARAMS="${PARAMS_FOLDER}hyperparams_alarm_pges.txt" -l nodes=1:ppn=2 "$SCRIPT"
