#!/bin/bash
# FOLDERS AND SAVE FILE
HOME_FOLDER="/" # DOCKER HOME_FOLDER
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
ENDING_NETWORKS=".xbif";
PARAMS_FOLDER=${HOME_FOLDER}"res/params/";
SAVE_FOLDER="/Users/jdls/developer/projects/ParallelBNs/res/params/"; # Mac save_folder

###################
# HYPERPARAMETERS #
###################
# ALGORITHMS
declare -a algorithms=("ges") # "pges" "hc" "phc" "pfhcbes")
#NETWORKS PATHS
declare -a net_names=("alarm" "andes" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")

# ENDINGS
endings=(".xbif_.csv" ".xbif50001_.csv" ".xbif50002_.csv" ".xbif50003_.csv"
".xbif50004_.csv" ".xbif50005_.csv" ".xbif50006_.csv" ".xbif50007_.csv" ".xbif50008_.csv"
".xbif50009_.csv" ".xbif50001246_.csv")

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

      # Saving hyperparams
      echo $alg $network $database $test >> $SAVE_FILE

    done
  done
done
