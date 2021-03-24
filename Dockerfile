FROM maven:3.6.3-jdk-8-slim
MAINTAINER Jorge D. Laborda <jorgedaniel.laborda@uclm.es>
#ADD ./target/ParallelBNs-1.0-SNAPSHOT.jar pbn-demo.jar
COPY ./src /parallelbns/src
COPY ./pom.xml /parallelbns/
WORKDIR /parallelbns
RUN mvn compile
COPY ./scripts /parallelbns/scripts
#COPY ./res /parallelbns/res


#ENTRYPOINT scripts/experiments.bash

# COPY . /ParallelBNs
# WORKDIR /ParallelBNs
# RUN javac src/pGESv2/java/org/albacete/simd/Main.java
#CMD ["java", "Main"]