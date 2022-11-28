#!/bin/bash
#pip3 install spython # if you do not have spython install it from the command line

DOCKERFILE_PATH="/home/jdls/developer/projects/ParallelBNs/Dockerfile"
SINGULARITY_RECIPE_PATH="/home/jdls/developer/projects/ParallelBNs/Singularity"
SINGULARITY_IMAGE_PATH="/home/jdls/developer/projects/ParallelBNs/Singularity.sif"
# save in the *.def file
echo "Creating Singularity File from Dockerfile (${DOCKERFILE_PATH})..."
spython recipe ${DOCKERFILE_PATH} &> ${SINGULARITY_RECIPE_PATH}
echo "Singularity recipe file saved to ${SINGULARITY_RECIPE_PATH}"

echo "Building image from recipe"
sudo singularity build --force ${SINGULARITY_IMAGE_PATH} ${SINGULARITY_RECIPE_PATH}