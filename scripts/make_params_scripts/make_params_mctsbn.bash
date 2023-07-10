#!/bin/bash
# Define parameter values
algorithm_name="mctsbn-distributed"
bn_names=("alarm" "andes" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
bn_path="./res/networks/"  # Replace with the actual BN path
db_path="./res/networks/BBDD/"
database_endings=(".xbif_.csv" ".xbif50001_.csv" ".xbif50002_.csv" ".xbif50003_.csv" ".xbif50004_.csv" ".xbif50005_.csv" ".xbif50006_.csv" ".xbif50007_.csv" ".xbif50008_.csv" ".xbif50009_.csv" ".xbif50001246_.csv")
iteration_limit=10000
exploit_constants=(50)
number_swaps=(0 0.25 0.5 0.75 1)
probability_swaps=(0 0.25 0.5 0.75 1)
selection_constants=(1 2 4 8 16 32)

# Generate parameters file
output_file="./parameters_mctsbn_distributed.txt"
rm -f "$output_file"

for bn_name in "${bn_names[@]}"; do
    bn_file="${bn_path}${bn_name}.xbif"
    for database_ending in "${database_endings[@]}"; do
        db_file="${db_path}${bn_name}${database_ending}"
        for exploit_constant in "${exploit_constants[@]}"; do
            for number_swap in "${number_swaps[@]}"; do
                for probability_swap in "${probability_swaps[@]}"; do
                    for selection_constant in "${selection_constants[@]}"; do
                        iter_limit=$((iteration_limit / selection_constant))
                        line="${algorithm_name} ${bn_name} ${bn_file} ${db_file} ${iter_limit} ${exploit_constant} ${number_swap} ${probability_swap} ${selection_constant}"
                        echo "$line" >> "$output_file"
                    done
                done
            done
        done
    done
done

echo "Parameters file generated successfully."
