#!/bin/sh
for ((fusion=0; fusion <2; fusion++))
do
    for ((nThreads=1; nThreads <=4; nThreads=nThreads*2))
    do
        #echo "fusion = $fusion nThreads = $nThreads"
        #echo ""
        for ((nItInterleaving=5; nItInterleaving<=15; nItInterleaving=nItInterleaving+5))
        do
            for ((net_number=0; net_number<16; net_number++))
            do
                for ((bbdd_number=0; bbdd_number<12; bbdd_number++))
                do
                    echo "$net_number $bbdd_number $fusion $nThreads $nItInterleaving" >> experiments.txt
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
done