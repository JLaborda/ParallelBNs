#!/bin/bash
# FOLDERS AND SAVE FILE
# Docker container Home Folder
HOME_FOLDER="/"
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
ENDING_NETWORKS=".xbif";
#ENDING_BBDD10K="10k.csv";
#ENDING_BBDD50K="50k.csv";
PARAMS_FOLDER=${HOME_FOLDER}"res/params/";
#SAVE_FILE="${PARAMS_FOLDER}hyperparams.txt";
# SAVE_FOLDER="/Users/jdls/developer/projects/ParallelBNs/res/params/" # Mac Save Folder
SAVE_FOLDER="/home/jdls/developer/projects/ParallelBNs/res/params/"    # Cluster Save Folder
###################
# HYPERPARAMETERS #
###################
# ALGORITHMS
declare -a algorithms=("pges_clustering") # "ges" "hc" "phc" "pfhcbes")
#NETWORKS PATHS
declare -a net_names=("alarm" "andes" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare networks=()
for net in ${net_names[@]}; do
    networks+=(${NETWORKS_FOLDER}$net${ENDING_NETWORKS} )
done

# DATABASES PATHS
endings=(".xbif_.csv" ".xbif50001_.csv" ".xbif50002_.csv" ".xbif50003_.csv"
".xbif50004_.csv" ".xbif50005_.csv" ".xbif50006_.csv" ".xbif50007_.csv" ".xbif50008_.csv"
".xbif50009_.csv" ".xbif50001246_.csv")
declare databases=()
for net in ${net_names[@]}; do
  for ending in ${endings[@]}; do
    databases+=(${BBDD_FOLDER}$net$ending)
  done
done

#TESTS PATHS
tests=()
for net in ${net_names[@]}; do
    tests+=(${TEST_FOLDER}$net"_test.csv")
done

#SEEDS
seeds=(-1)


# INTERLEAVING AND THREADS
declare -a nThreads=(1 2 4 6 8)
declare -a nItInterleavings=(5 10 15)
#maxIterations=250

#echo "Saving data into ${SAVE_FILE}"
#echo "" > $SAVE_FILE
# SAVING HYPERPARAMETERS
for alg in ${algorithms[@]};
do
  for net in ${net_names[@]};
  do
    #Defining SAVE_FILE
    SAVE_FILE="${SAVE_FOLDER}hyperparams_${net}_${alg}.txt"
    > ${SAVE_FILE} # Deleting previous info

    # Defining network
    network="${NETWORKS_FOLDER}$net${ENDING_NETWORKS}"

    # Defining test
    test=${TEST_FOLDER}$net"_test.csv"

    # Echo where we are saving info
    echo "Saving params into ${SAVE_FILE}"

    for ending in ${endings[@]};
    do
      # Defining database of the network
      database=${BBDD_FOLDER}$net$ending
      #for threads in ${nThreads[@]};
      #do
      for nItInterleaving in ${nItInterleavings[@]}
      do
        for seed in ${seeds[@]}
        do
          echo $alg $net $network $database $test $nItInterleaving $seed >> $SAVE_FILE
        done
      done
      #done
    done
  done
done
                    