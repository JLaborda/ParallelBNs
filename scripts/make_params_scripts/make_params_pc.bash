#!/bin/bash

# Carpetas de redes y bases de datos
folder_networks="/home/jorlabs/projects/ParallelBNs/res/networks"
folder_databases="/home/jorlabs/projects/ParallelBNs/res/datasets"
output_file="/home/jorlabs/projects/ParallelBNs/res/params/pc/pc_params.txt"

# Lista de nombres de redes
networks=("alarm" "barley" "child" "insurance" "mildew" "water" "hailfinder" "hepar2" "win95pts" "andes" "diabetes" "link" "pathfinder" "pigs" "munin")

# Generar combinaciones de parámetros
for netName in "${networks[@]}"; do
    for datasetNum in {1..10} "All"; do
        algName="pc"
        netPath="$folder_networks/$netName/$netName.xbif"
        databasePath="$folder_databases/$netName/${netName}${datasetNum}.csv"

        # Imprimir combinación de parámetros
        echo "algName $algName netName $netName netPath $netPath databasePath $databasePath" netName $netName >> "$output_file"
    done
done
