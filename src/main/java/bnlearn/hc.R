# Instalar y cargar la biblioteca bnlearn si aún no está instalada
#if (!require(bnlearn)) {
#  install.packages("bnlearn")
#  library(bnlearn)
#}



library(bnlearn)
library(igraph)

# Función para moralizar una red bayesiana
moralizeGraph <- function(graph) {
  # Crear una copia de graph
  print("Arcs of graph")
  print(arcs(graph))
  igraph_graph <- igraph::graph_from_edgelist(arcs(graph), directed = TRUE)
  moralizedGraph <- as.undirected(igraph_graph)
  #moralizedGraph <- make_undirected_graph(arcs(graph))
  print("igraph initial graph")
  print(moralizedGraph)
  #moralizedGraph <- graph
  for (n in nodes(graph)) {
    # Obtiene los padres del nodo
    p <- parents(graph, n)

    if (length(p) == 0) {
      next
    }

    len_parents <- length(p)
    if(len_parents > 1){
      for (i in 1:(len_parents - 1)) {
        for (j in (i + 1):len_parents) {
          # Si p[i] o p[j] son NA, continuar con el siguiente par de nodos
          if (is.na(p[i]) || is.na(p[j])) {
            next
          }
          # Añadir un enlace entre los nodos p[i] y p[j]
          #moralizedGraph <- set.arc(moralizedGraph, from = p[i], to = p[j])
          #moralizedGraph <- set.arc(moralizedGraph, from = p[j], to = p[i])
          #arcs(moralizedGraph) <- rbind(arcs(moralizedGraph), c(p[i], p[j]))
          print("Adding edge:")
          cat(p[i], "---", p[j], "\n")
          add_edges(moralizedGraph, c(p[i],p[j]))#c("Esto","es","una","mierda"))#c(as.character(p[i]), as.character(p[j])))
          print("Added...")
        }
      }
    }
  }
  print("igraph final graph")
  print(moralizedGraph)
  return(moralizedGraph)
}

# Función para calcular la métrica SHD entre dos redes bayesianas
shmd <- function(bn1, bn2) {
  # Asegúrate de que las redes tengan el mismo conjunto de variables
  variables <- intersect(colnames(bn1), colnames(bn2))
  bn1 <- bn1[, variables]
  bn2 <- bn2[, variables]

  # Convierte las redes a objetos de la clase "bn" de bnlearn
  bn1 <- bn(as(bn1, "data.frame"))
  bn2 <- bn(as(bn2, "data.frame"))

  # Moraliza las redes
  bn1_moral <- bn1
  bn2_moral <- bn2

 

  # Calcula la métrica SHD
  shd_sum <- 0
  for (variable1 in nodes(bn1_moral)) {
    for (variable2 in nodes(bn1_moral)) {
      if (variable1 != variable2) {
        edges1 <- as.character(arcs(bn1_moral, variable1))
        edges2 <- as.character(arcs(bn2_moral, variable1))
        if (!edges1 %in% edges2 && !edges2 %in% edges1) {
          shd_sum <- shd_sum + 1
        }
      }
    }
  }

  return(shd_sum)
}



# Obtener la ruta del archivo CSV desde la línea de comandos
args <- commandArgs(trailingOnly = TRUE)
csv <- args[1]
# Leer el dataframe desde el archivo CSV
data <- read.csv(csv, header = TRUE, sep = ",")

# Crear un vector con la clase 'factor' para cada columna
clases <- rep("factor", ncol(data))

# Reading again the data with the correct classes
data <- read.csv(csv, header = TRUE, colClasses = clases)
#print(str(data))

# Read original network
original_network_path <- args[2]

# Cargar datos
#data("alarm")

#df <- as.data.frame(data("alarm"))
#print(head(data))
# Aplicar el algoritmo HC (Hill-Climbing) para aprender la estructura de la red
#network_structure <- hc(data, score= "bde")

# BDeu score of the resulting network
#bdeu_score <- score(network_structure, data, type = "bde")

# Calcular shmd de la red

# Imprimir la estructura aprendida
#print(network_structure)


# Ejemplo de uso de moralize
# Crea un grafo de ejemplo
print("Ejemplo de moralize")
example_graph <- empty.graph(nodes = c("A", "B", "C", "D", "E"))
arcs(example_graph) <- matrix(c("A", "B", "C", "B", "D", "B", "E", "C"), ncol = 2, byrow = TRUE)

print("Antes de moralizar")
print(example_graph)
# Moraliza el grafo
example_graph <- moralizeGraph(example_graph)
print("Después de moralizar")
print(example_graph)

# Muestra el grafo resultante
#pdf("moralizeGraph.pdf")
#graphviz.plot(example_graph)
#dev.off()
