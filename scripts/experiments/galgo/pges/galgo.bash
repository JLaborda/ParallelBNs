#!/bin/bash

# Move to the current working directory
cd $CWD

# Activate the virtual environment
#mamba activate mAnDE

# Run the experiment with the provided arguments
java -Djava.util.concurrent.ForkJoinPool.common.parallelism=$THREADS -jar $FILE $PBS_ARRAY_INDEX $PARAMS $THREADS