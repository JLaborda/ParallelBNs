#!/bin/bash
# FOLDERS AND SAVE FILE
 HOME_FOLDER="/home/jdls/developer/projects/ParallelBNs/"
# HOME_FOLDER="/Users/jdls/developer/projects/ParallelBNs/"
# HOME_FOLDER="~/developer/projects/ParallelBNs/"
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
ENDING_NETWORKS=".xbif";
#ENDING_BBDD10K="10k.csv";
#ENDING_BBDD50K="50k.csv";
PARAMS_FOLDER=${HOME_FOLDER}"res/params/";
#SAVE_FILE=${PARAMS_FOLDER}"hyperparams.txt"
SAVE_FILE="/Users/jdls/developer/projects/ParallelBNs/res/params/hyperparams.txt"
###################
# HYPERPARAMETERS #
###################
# ALGORITHMS
declare -a algorithms=("ges" "pges") #"hc" "phc" "pfhcbes")
#NETWORKS PATHS
declare -a net_names=("alarm" "andes" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare networks=()
for net in ${net_names[@]}; do
    networks+=(${NETWORKS_FOLDER}$net${ENDING_NETWORKS} )
done

#echo "${networks[@]}"

#declare -a databases=(
#    ${BBDD_FOLDER}"alarm"${ENDING_BBDD10K} ${BBDD_FOLDER}"alarm"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"cancer"${ENDING_BBDD10K} ${BBDD_FOLDER}"cancer"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"barley"${ENDING_BBDD10K} ${BBDD_FOLDER}"barley"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"child"${ENDING_BBDD10K} ${BBDD_FOLDER}"child"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"insurance"${ENDING_BBDD10K} ${BBDD_FOLDER}"insurance"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"mildew"${ENDING_BBDD10K} ${BBDD_FOLDER}"mildew"${ENDING_BBDD50K}
#    ${BBDD_FOLDER}"water"${ENDING_BBDD10K} ${BBDD_FOLDER}"water"${ENDING_BBDD50K}
#)
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

echo "Saving data into ${SAVE_FILE}"

# SAVING HYPERPARAMETERS
for alg in ${algorithms[@]};
do
  echo "Hellooooo"
  for database in ${databses[@]};
  do
    for test in ${tests[@]};
    do
      for threads in ${nThreads[@]};
      do
        for nItInterleaving in ${nItInterleavings[@]}
        do
            for seed in ${seeds[@]}
            do
              echo $alg $database $test $threads $nItInterleaving $seed >> $SAVE_FILE
            done
        done
      done
    done
  done
done



# len_nets=${#networks[@]};
# for((i=0;i<$len_nets;i++))
# do
#     netPath=${networks[$i]};
#     testPath=${tests[$i]};
#     net_name=${net_names[$i]}

#     # Deleting files of previous params
#     FILE=${PARAMS_FOLDER}experiments_${net_name}.txt
#     #if test -f "$FILE"; then
#     #    rm $FILE
#     #fi

#     # Creating new File
#     >${FILE}
#     echo "Creating experiments for: ${net_name} at: ${FILE}"
#     #if test -f "$FILE"; then
#     #    echo "File Created: $FILE"
#     #fi
#     # Defining Databases for current network
#     databases=()
#     for ending in ${endings[@]}; do
#         databases+=(${BBDD_FOLDER}${net_name}$ending)
#     done

#     for dataPath in ${databases[@]};
#     do
#         for nthread in ${nThreads[@]};
#         do
#             for nItInterleaving in ${nItInterleavings[@]};
#             do
#                 for alg in ${algorithms[@]};
#                 do
#                     if [ $alg == "ges" ]
#                     then
#                         echo ${net_names[$i]} $alg $netPath $dataPath $testPath >> $FILE
#                     elif [ $alg == "hc" ]
#                         then
#                             echo ${net_names[$i]} $alg $netPath $dataPath $testPath $nItInterleaving $maxIterations >> $FILE
#                     else
#                         for seed in ${seeds[@]};
#                         do
#                             echo ${net_names[$i]} $alg $netPath $dataPath $testPath $nItInterleaving $maxIterations $nthread $seed >> $FILE
#                         done
#                     fi
#                 done
#             done
#         done
#     done
# done

# # Iterate the string array using for loop
# # echo "$net_number $bbdd_number $fusion $nThreads $nItInterleaving" #>> experiments.txt
                    