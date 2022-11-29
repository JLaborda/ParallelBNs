#!/bin/bash

#declare -a net_names=("andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
#declare -a net_names=("alarm" "andes" "barley" "cancer" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
#declare -a net_names=("andes" "link" "mildew" "munin" "pigs" "water" "win95pts")
#declare -a net_names=("alarm", "barley", "child", "insurance", "hailfinder", "hepar2") # Redes pequeñas

# declare -a net_names=("mildew", "water", "win95pts") # Redes medianas/pequeñas faltantes
#"alarm", 
# declare -a suffixes=("01" "02" "03" "04" "05" "06")
#

#echo "building docker images in all of the nodes of the cluster..."
#build-image parallelbns /home/jdls/developer/projects/ParallelBNs/Dockerfile
#echo "build complete"
PARAMS_FOLDER="/res/params/"

declare -a hyperparams=( ${PARAMS_FOLDER}"hyperparams_alarm_pges.txt" )
#echo "${hyperparams[@]}"

SCRIPT="/home/jdls/developer/projects/ParallelBNs/scripts/experiments/run_experiments_cluster.bash"
#EL BUENO
echo "Adding experiments to qsub"
for params in ${hyperparams[@]}; do
    qsub -J 0-329 -v PARAMS="$params" "$SCRIPT"				  	# 1 Hilo
    qsub -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=2 "$SCRIPT"    	# 2 Hilos
    qsub -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=4 "$SCRIPT"    	# 4 Hilos
    qsub -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=6 "$SCRIPT"    	# 6 Hilos
    qsub -J 0-329 -v PARAMS="$params" -l nodes=1:ppn=8 "$SCRIPT"    	# 8 Hilos
done