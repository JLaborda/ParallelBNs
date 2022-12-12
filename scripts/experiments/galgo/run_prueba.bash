#!/bin/bash

SCRIPT="./scripts/experiments/galgo/run_experiments_from_jar.bash"

read -p "Build singularity image?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  mvn clean package -Dmaven.test.skip
fi

THREADS=8

bash ${SCRIPT} 6 "/home/jorlabs/projects/ParallelBNs/res/params/pges-jc/hyperparams_pges-jc_2.txt" ${THREADS}