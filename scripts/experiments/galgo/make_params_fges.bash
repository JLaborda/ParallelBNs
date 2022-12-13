#!/bin/bash
HOME_FOLDER="$HOME/ParallelBNs/";
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
PARAMS_FOLDER="res/params/";
ENDING_NETWORKS=".xbif";

SAVE_FILE="${PARAMS_FOLDER}hyperparams_fges.txt";

#seeds=(2 3 5 7 11 13 17 19 23 29)
seeds=(2)

declare -a net_names=("alarm" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance"  "mildew" "water" "win95pts" "andes" "pigs" "link" "munin")

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


for net in ${net_names[@]};
do
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

    echo "fges" $net $network $database $test "-1" "-1" $seed >> $SAVE_FILE
    echo "fges-faithfulness" $net $network $database $test "-1" "-1" $seed >> $SAVE_FILE
    echo "ges-tetrad" $net $network $database $test "-1" "-1" $seed >> $SAVE_FILE
  done
done

