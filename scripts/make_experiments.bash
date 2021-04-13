#!/bin/bash
HOME_FOLDER="/parallelbns/"
NETWORKS_FOLDER=${HOME_FOLDER}"res/networks/";
BBDD_FOLDER=${NETWORKS_FOLDER}"BBDD/";
TEST_FOLDER=${NETWORKS_FOLDER}"BBDD/tests/";
ENDING_NETWORKS=".xbif";
ENDING_BBDD10K="10k.csv";
ENDING_BBDD50K="50k.csv";
PARAMS_FOLDER="/home/jdls/developer/projects/ParallelBNs/res/params/";

declare -a algorithms=("ges" "pges" "hc" "phc" "pfhcbes")

declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
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
endings=(".xbif_.csv" ".xbif50001_.csv" ".xbif50002_.csv" ".xbif50003_.csv"
".xbif50004_.csv" ".xbif50005_.csv" ".xbif50006_.csv" ".xbif50007_.csv" ".xbif50008_.csv"
".xbif50009_.csv" ".xbif50001246_.csv")

databases=()
for net in ${net_names[@]}; do
    for ending in ${endings[@]}; do
        databases+=(${BBDD_FOLDER}$net$ending)
    done
done

tests=()
for net in ${net_names[@]}; do
    tests+=(${TEST_FOLDER}$net"_test.csv")
done
seeds=(2 3 5 7 11 13 17 19 23 29)


#echo "${tests[@]}"

declare -a nThreads=(2 4 6 8)
declare -a nItInterleavings=(5 10 15)
maxIterations=1000

len_nets=${#networks[@]};
for((i=0;i<$len_nets;i++))
do
    netPath=${networks[$i]};
    testPath=${tests[$i]};
    net_name=${net_names[$i]}

    # Defining Databases for current network
    databases=()
    for ending in ${endings[@]}; do
        databases+=(${BBDD_FOLDER}${net_name}$ending)
    done

    for dataPath in ${databases[@]};
    do
        for nthread in ${nThreads[@]};
        do
            for nItInterleaving in ${nItInterleavings[@]};
            do
                for alg in ${algorithms[@]};
                do
                    if [ $alg == "ges" ]
                    then    
                        echo ${net_names[$i]} $alg $netPath $dataPath $testPath $nItInterleaving >> ${PARAMS_FOLDER}experiments_${net_names[$i]}.txt
                    elif [ $alg == "hc" ]
                        then
                            echo ${net_names[$i]} $alg $netPath $dataPath $testPath $nItInterleaving $maxIterations >> ${PARAMS_FOLDER}/experiments_${net_names[$i]}.txt
                    else
                        for seed in ${seeds[@]};
                        do
                            echo ${net_names[$i]} $alg $netPath $dataPath $testPath $nItInterleaving $nthread $maxIterations $seed >> ${PARAMS_FOLDER}/experiments_${net_names[$i]}.txt
                        done
                    fi
                done
            done
        done
    done
done

# Iterate the string array using for loop
# echo "$net_number $bbdd_number $fusion $nThreads $nItInterleaving" #>> experiments.txt
                    