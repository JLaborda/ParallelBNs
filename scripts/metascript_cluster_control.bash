#!/bin/bash

#declare -a net_names=("andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
#declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
#declare -a net_names=("andes" "link" "mildew" "munin" "pigs" "water" "win95pts")
declare -a net_names=("alarm", "barley", "child", "insurance", "hailfinder", "hepar2", "mildew", "water", "win95pts") # Redes pequeñas

#declare -a net_names=("mildew", "water", "win95pts") # Redes medianas/pequeñas faltantes
#"alarm", 
# declare -a suffixes=("01" "02" "03" "04" "05" "06")
#PARAMS_FOLDER="res/params/"

#echo "building docker images in all of the nodes of the cluster..."
#build-image parallelbns /home/jdls/developer/projects/ParallelBNs/Dockerfile
#echo "build complete"

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/run_experiments_cluster.bash"

for net in ${net_names[@]}; do
    qsub -J 0-10 -v PARAMS="$net" "$SCRIPT"				  	# 1 Hilo
    #qsub -J 0-329 -v PARAMS="$net" -l nodes=1:ppn=2 "$SCRIPT"    	# 2 Hilos
    #qsub -J 0-329 -v PARAMS="$net" -l nodes=1:ppn=4 "$SCRIPT"    	# 4 Hilos
    #qsub -J 0-329 -v PARAMS="$net" -l nodes=1:ppn=6 "$SCRIPT"    	# 6 Hilos
    #qsub -J 0-329 -v PARAMS="$net" -l nodes=1:ppn=8 "$SCRIPT"    	# 8 Hilos
done

# Previous
#for net in ${net_names[@]}; do
#    #for suffix in ${suffixes[@]}; do
#    PARAMS="/home/jdls/developer/projects/ParallelBNs/res/params/experiments_${net}.txt" #${net}/${net}_params_${suffix}"
#    LENGTH=$(cat ${PARAMS} | wc -l)
#    PARAMS="/parallelbns/res/params/experiments_${net}.txt"
#    #qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH -l nodes=1:ppn=8 $SCRIPT
#    qsub -v PARAMS=${PARAMS} -N "ParallelBNs-${net}" -J 1-$LENGTH $SCRIPT
#    #done
#done