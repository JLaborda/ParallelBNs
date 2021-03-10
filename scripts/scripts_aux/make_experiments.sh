#!/bin/sh

threads=(2 4 8 16)
interleaving=(5 10 15)
net_name=(alarm andes barley cancer child earthquake hailfinder hepar2 insurance link mildew munin pigs water win95pts)
bbdd_numbers=(0 50000 50001 50002 50003 50004 50005 50006 50007 50008 50009 50001246)
for thread in "${threads[@]}"
do
    #echo "fusion = $fusion nThreads = $nThreads"
    #echo ""
    for inter in "${interleaving[@]}"
    do
        
        for net in "${net_name[@]}"
        do
            for bbdd in "${bbdd_numbers[@]}"
            do
                if [ $bbdd == 0 ]
                then
                    echo "res/networks/${net}.xbif res/networks/BBDD/${net}.xbif_.csv" $thread $inter >> pgesv2_experiments.txt
                else
                    echo "res/networks/${net}.xbif res/networks/BBDD/${net}.xbif${bbdd}_.csv" $thread $inter >> pgesv2_experiments.txt
                fi
                #echo "$net_number $bbdd_number $fusion $nThreads $nItInterleaving" >> experiments.txt
                
                #if ! [[ $fusion == 1 && $nThreads == 4 && $nItInterleaving == 15 && $net_number == 15 && $bbdd_number == 11 ]]
                #then
                #    echo "" >> experiments.txt # Printing new line
                #fi

                #echo "$net_number" >> experiments.txt
                #echo " " >> experiments.txt
                #echo "$bbdd_numnber" >> experiments.txt
                #echo " " >> experiments.txt
                #echo "$fusion" >> experiments.txt
                #echo " " >> experiments.txt
                #echo "$nThreads" >> experiments.txt
                #echo " " >> experiments.txt
                #echo "$nItInterleaving" >> experiments.txt
                #echo "\n" >> experiments.txt
                #echo "" + $net_number+ " " +  $bbdd_number $fusion $nThreads $nItInterleaving"\n" >> experiments.txt 
            done
        done
    done
done