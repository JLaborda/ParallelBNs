#!/bin/bash

# Lista de algoritmos
algorithms=("hc" "tabu" "rsmax2" "mmhc" "h2pc" "aracne" "chow-liu" "pc-stable" "gs" "iamb" "fast-iamb" "inter-iamb" "fdr-iamb" "mmpc" "hiton-pc" "hpc")

# Ruta al script de R
r_script="src/main/java/bnlearn/main.R"

# Ruta al archivo de datos (ajusta según tu caso)
data_file="res/datasets/alarm/alarm1.csv"

# Carpeta de resultados (ajusta según tu caso)
results_folder="results/pruebas/bnlearn"

# Iterar sobre los algoritmos
for algorithm in "${algorithms[@]}"
do
  echo "Running algorithm: $algorithm"
  Rscript "$r_script" "$data_file" "$algorithm" "1" "$results_folder"
done
