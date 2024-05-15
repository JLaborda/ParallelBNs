
# Cargar la biblioteca phsl
#devtools::install_github("jirehhuang/phsl")
#library(phsl)



#chooseCRANmirror(ind=1)

# Instalar y cargar la biblioteca bnlearn si aún no está instalada
if (!require(bnlearn)) {
  chooseCRANmirror(ind = 1)
  install.packages("bnlearn")
  library(bnlearn)
}

library(bnlearn)


version_bnlearn <- packageVersion("bnlearn")
print(paste("bnlearn version:", version_bnlearn))

# Obtener argumentos de la línea de comandos
args <- commandArgs(trailingOnly = TRUE)

# Verificar la cantidad correcta de argumentos
if (length(args) != 4) {
  cat("Uso: Rscript script.R <ruta_csv> <algorithm> <index> <save_folder>\n")
  quit(save = "no", status = 1)
}

# Asignar argumentos a variables
path_csv <- args[1]
algorithm <- args[2]
index <- args[3]
save_folder <- args[4]

# Leer el dataframe desde el archivo CSV
data <- read.csv(path_csv, header = TRUE, sep = ",", stringsAsFactors = TRUE)

# Crear un vector con la clase 'factor' para cada columna
clases <- rep("factor", ncol(data))

# Reading again the data with the correct classes
data <- read.csv(path_csv, header = TRUE, colClasses = clases, stringsAsFactors = TRUE)

#print("Data read from CSV:")
#print(data)

# Imprimir el algoritmo seleccionado
cat("Algorithm:", algorithm, "\n")

# Seleccionar el algorithm y aprender la red bayesiana
# Score based algorithms
if (algorithm == "hc") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo = hc(data, score= "bde")
    end_time <- Sys.time()
} else if (algorithm == "tabu") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- tabu(data, score= "bde")
    end_time <- Sys.time()
} else if (algorithm == "rsmax2") { # Hybrid learning algorithms
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- rsmax2(data, maximize.args = list(score = "bde"))
    end_time <- Sys.time()
} else if (algorithm == "mmhc") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()
    modelo <- mmhc(data, maximize.args = list(score = "bde"))
    end_time <- Sys.time()
} else if (algorithm == "h2pc") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- h2pc(data, maximize.args = list(score = "bde"))
    end_time <- Sys.time()
} else if (algorithm == "aracne") { # Pairwise mutual information based algorithms
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- aracne(data, mi="mi")
    modelo <- cextend(modelo)
    #modelo <- orient(modelo, data = data)
    end_time <- Sys.time()
} else if (algorithm == "chow-liu") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- chow.liu(data, mi="mi")
    modelo <- cextend(modelo)
    #modelo <- orient(modelo, data = data)
    end_time <- Sys.time()
} else if (algorithm == "pc-stable") { # Constraint based learning algorithms
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- pc.stable(data,test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
} else if (algorithm == "gs") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- gs(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "iamb") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- iamb(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "fast-iamb") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- fast.iamb(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}  else if (algorithm == "inter-iamb") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- inter.iamb(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "fdr-iamb") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- iamb.fdr(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "mmpc") { # Other Constraint based learning algorithms
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- mmpc(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "hiton-pc") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- si.hiton.pc(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
}else if (algorithm == "hpc") {
    # Medir el tiempo de ejecución
    start_time <- Sys.time()  
    modelo <- hpc(data, test="mi", undirected = FALSE)
    modelo <- cextend(modelo)
    end_time <- Sys.time()
# }else if (algorithm == "ppc-tabu") {
#     # Medir el tiempo de ejecución
#     ## Read Bayesian network object 
#     true_bn <- bnrepository("child")

#     ## Generate data and relevel for simplicity
#     set.seed(1)
#     x <- bnlearn::rbn(true_bn, n = 1e4)
#     x <- as.data.frame(sapply(x, function(x) as.factor(as.integer(x) - 1L)),
#                     stringsAsFactors = TRUE)

#     ## pPC with PATH
#     bn1 <- bnsl(x = x, restrict = "ppc", maximize = "",
#                 restrict.args = list(alpha = 1e-3, max.sx = 3, sort_pval = TRUE),
#                 maximize.args = list(maxp = 8), path = 10, min_alpha = 1e-5,
#                 hgi = FALSE, debug = TRUE)
}else {
    cat("Algoritmo no válido\n")
    quit(save = "no", status = 1)
}

# Imprimiendo resultados
#print(modelo)
#print(class(modelo))

# Creating a fit model
fit_model = bn.fit(modelo, data=data, method = "mle")

# Calcular el tiempo de ejecución
execution_time <- as.numeric(difftime(end_time, start_time, units = "secs"))
cat("Execution time:", execution_time, "seconds\n")

### Saving the model ###
# If save folder does not end in "/", add it
if (substr(save_folder, nchar(save_folder), nchar(save_folder)) != "/") {
    save_folder <- paste(save_folder, "/", sep = "")
}

# If save folder does not exist, create it
if (!dir.exists(save_folder)) {
    dir.create(save_folder)
}
if(!dir.exists(paste(save_folder, algorithm, "/", sep = ""))) {
    dir.create(paste(save_folder, algorithm, "/", sep = ""))
}


# Creating save path
save_path <- paste(save_folder, algorithm, "/exp_",algorithm,"_", index, ".bif", sep = "")
print("Saving model to:")
print(save_path)

# Creating the save file
save_file <- file(save_path, "w")
write.bif(save_path, fit_model)
close(save_file)


# Guardando la información del experimento en un archivo CSV
result <- data.frame(
  algorithm = algorithm,
  index = index,
  execution_time = execution_time,
  csv_path = path_csv
)

# Crear la ruta del archivo CSV de resultados
result_csv_path <- paste(save_folder, algorithm, "/exp_",algorithm,"_", index, "_info.csv", sep = "")

# Guardar la información en el archivo CSV
write.csv(result, result_csv_path, row.names = FALSE)

print("Results saved to:")
print(result_csv_path)


