#!/bin/bash

# CONSTANTS
DOCKER_PARAMS_FOLDER="/res/params/"
SINGULARITY_SCRIPT_PATH="/home/jdls/developer/projects/ParallelBNs/scripts/singularity_scripts/create_singularity_image_from_dockerfile.bash"
NET_NAMES=(alarm andes win95pts)

#HYPERPARAMS_PATHS PATHS
HYPERPARAMS_PATHS=()
for NET in "${NET_NAMES[@]}"; do
  HYPERPARAMS_PATHS+=("${DOCKER_PARAMS_FOLDER}hyperparams_${NET}_circular_fusion.txt")
done

#BUILDING SINGULARITY IMAGE
read -p "Build singularity image?(Y/N)" BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  bash ${SINGULARITY_SCRIPT_PATH}
fi

# RUNNING EXPERIMENTS

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/experiments/run_experiments_cluster.bash"
echo "Adding experiment to qsub"
for params in "${HYPERPARAMS_PATHS[@]}"; do
    qsub -J 0-362 -v PARAMS="${params}" -l nodes=node8:ppn=2 "$SCRIPT"    	# 2 Hilos
    qsub -J 0-362 -v PARAMS="${params}" -l nodes=node8:ppn=4 "$SCRIPT"    	# 4 Hilos
    qsub -J 0-362 -v PARAMS="${params}" -l nodes=node8:ppn=8 "$SCRIPT"    	# 8 Hilos
done