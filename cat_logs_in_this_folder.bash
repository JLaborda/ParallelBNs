#!/bin/bash

# Crear un nuevo archivo para almacenar todos los datos
touch logs_error.txt
touch logs_output.txt
# Obtener una lista de todos los archivos con extensión .txt en el directorio actual
files_error=$(ls -1 | grep -E 'pGES-JC.*-speedUp\.e[0-9]{6}\.[0-9]{3}')
files_output=$(ls -1 | grep -E 'pGES-JC.*-speedUp\.o[0-9]{6}\.[0-9]{3}')
# Recorrer cada archivo de experimento
for file in $files_error
do
    # Añadiendo línea para diferenciar los archivos adjuntos entre si
    echo "------------------------------------------------------------------" >> logs_error.txt
    echo "Log error del archivo: $file" >> logs_error.txt
    echo "------------------------------------------------------------------" >> logs_error.txt
    # Agregar el contenido del archivo de experimento al archivo de datos
    cat "$file" >> logs_error.txt
done

for file in $files_output
do
    # Añadiendo línea para diferenciar los archivos adjuntos entre si
    echo "------------------------------------------------------------------" >> logs_output.txt
    echo "Log output del archivo: $file" >> logs_output.txt
    echo "------------------------------------------------------------------" >> logs_output.txt
    # Agregar el contenido del archivo de experimento al archivo de datos
    cat "$file" >> logs_output.txt
done