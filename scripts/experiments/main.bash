#!/bin/bash

# Local Singularity image creation script
SINGULARITY_SCRIPT_PATH="/home/jdls/developer/projects/ParallelBNs/scripts/singularity_scripts/create_singularity_image_from_dockerfile.bash"
METASCIPT_PATH="/home/jdls/developer/projects/ParallelBNs/scripts/experiments/metascript_cluster.bash"

# Create Singularity image
bash ${SINGULARITY_SCRIPT_PATH}

# Run Metascript
bash ${METASCIPT_PATH}