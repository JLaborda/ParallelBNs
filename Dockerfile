FROM maven:3.6.3-jdk-8
MAINTAINER Jorge D. Laborda <jorgedaniel.laborda@uclm.es>
#ADD ./target/ParallelBNs-1.0-SNAPSHOT.jar pbn-demo.jar
COPY . /parallelbns
WORKDIR /parallelbns
RUN mvn compile
ENTRYPOINT ./scripts/experiments.bash

# COPY . /ParallelBNs
# WORKDIR /ParallelBNs
# RUN javac src/pGESv2/java/org/albacete/simd/Main.java
#CMD ["java", "Main"]