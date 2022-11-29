#!/bin/bash

PARAMS_FOLDER="/res/params/"
SINGULARITY_SCRIPT_PATH="/home/jdls/developer/projects/ParallelBNs/scripts/singularity_scripts/create_singularity_image_from_dockerfile.bash"

read -p "Build singularity image?(Y/N)" BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  bash ${SINGULARITY_SCRIPT_PATH}
fi

declare -a hyperparams=( ${PARAMS_FOLDER}"hyperparams_alarm_pges.txt" )
#echo "${hyperparams[@]}"

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/experiments/run_experiments_cluster.bash"
#EL BUENO
echo "Adding experiment to qsub"
qsub -J 0-10 -v PARAMS="${PARAMS_FOLDER}hyperparams_alarm_circular_fusion.txt" -l nodes=1:ppn=2 "$SCRIPT"
