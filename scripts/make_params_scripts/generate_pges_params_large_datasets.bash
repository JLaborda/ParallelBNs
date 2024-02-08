#!/bin/bash

# Array with network names
networks=("munin" "andes" "pigs" "link" "diabetes" "pathfinder")

# Array with database sizes
sizes=("1000" "2000" "5000" "10000" "20000" "50000" "100000" "200000")

# Number of threads
threads=("16" "8" "4" "2")

# Output file
output_file="/Users/jdls/developer/projects/ParallelBNs/res/params/large_datasets/pges_params.txt"

# Remove the existing output file
rm -f "$output_file"

# Loop through networks and sizes to generate parameters
for network in "${networks[@]}"; do
    # net path
    net_path="/home/jorlabs/projects/ParallelBNs/res/networks/$network/${network}.xbif"
    for size in "${sizes[@]}"; do
        # Database path
        db_path="/home/jorlabs/projects/ParallelBNs/res/large_datasets/$network/${network}_${size}.csv"
        for thread in "${threads[@]}"; do
            # Generate parameters and append to the output file
            echo "algName pges numberOfRealThreads $thread netPath $net_path databasePath $db_path netName $network" >> "$output_file"
            echo "algName ges numberOfRealThreads $thread netPath $net_path databasePath $db_path netName $network" >> "$output_file"
        done
    done
done

echo "Done!"
