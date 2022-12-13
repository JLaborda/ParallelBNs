#!/bin/bash
# FOLDERS AND SAVE FILE

#docker Home Folder
HOME_FOLDER="/home/jorlabs/projects/ParallelBNs/" # Project Home Folder: Galgo
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
ENDING_NETWORKS=".xbif";
PARAMS_FOLDER=${HOME_FOLDER}"res/params/pges-jc/";
#SAVE_FILE="${PARAMS_FOLDER}hyperparams.txt";
# SAVE_FOLDER="/Users/jdls/developer/projects/ParallelBNs/res/params/" # Mac Save Folder
SAVE_FOLDER=${PARAMS_FOLDER}   # Cluster Save Folder
###################
# HYPERPARAMETERS #
###################
# ALGORITHMS
declare -a algorithms=("pges-jc") # "ges" "hc" "phc" "pfhcbes")
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
seeds=(2 3 5 7 11 13 17 19 23 29)


# INTERLEAVING AND THREADS
declare -a nThreads=(1 2 4 6 8)
declare -a nItInterleavings=(5 10 15)
#maxIterations=250

#echo "Saving data into ${SAVE_FILE}"
#echo "" > $SAVE_FILE



# SAVING HYPERPARAMETERS
for alg in ${algorithms[@]};
do
  SAVE_FILE_2T="${SAVE_FOLDER}hyperparams_${alg}_2.txt"
  SAVE_FILE_4T="${SAVE_FOLDER}hyperparams_${alg}_4.txt"
  SAVE_FILE_8T="${SAVE_FOLDER}hyperparams_${alg}_8.txt"
  SAVE_FILE_16T="${SAVE_FOLDER}hyperparams_${alg}_16.txt"

  echo >> ${SAVE_FILE_2T} # Deleting previous info
  echo >> ${SAVE_FILE_4T} # Deleting previous info
  echo >> ${SAVE_FILE_8T} # Deleting previous info
  echo >> ${SAVE_FILE_16T} # Deleting previous info
  for net in ${net_names[@]};
  do
    #Defining SAVE_FILE
    

    # Defining network
    network="${NETWORKS_FOLDER}$net${ENDING_NETWORKS}"

    # Defining test
    test=${TEST_FOLDER}$net"_test.csv"

    # Echo where we are saving info
    #echo "Saving params into ${SAVE_FILE}"

    for ending in ${endings[@]};
    do
      # Defining database of the network
      database=${BBDD_FOLDER}$net$ending
      #for threads in ${nThreads[@]};
      #do
      for nItInterleaving in ${nItInterleavings[@]}
      do
        # Saving hyperparameters
        echo $alg $net $network $database $test 2 $nItInterleaving >> $SAVE_FILE_2T
        echo $alg $net $network $database $test 4 $nItInterleaving >> $SAVE_FILE_4T
        echo $alg $net $network $database $test 8 $nItInterleaving >> $SAVE_FILE_8T
        echo $alg $net $network $database $test 16 $nItInterleaving >> $SAVE_FILE_16T
      done
      #done
    done
  done
done
                    