#
# Build stage
#
FROM maven:3.6.3-jdk-8-slim AS build
MAINTAINER Jorge D. Laborda <jorgedaniel.laborda@uclm.es>
#ADD ./target/ParallelBNs-1.0-SNAPSHOT.jar pbn-demo.jar
#COPY ./developer/projects/ParallelBNs/src/ /parallelbns/src
#COPY ./developer/projects/ParallelBNs/pom.xml /parallelbns/
COPY src /src
COPY repos /repos
COPY pom.xml /
RUN mvn -f /pom.xml clean package


#
# Package stage
#
FROM openjdk:8-jre-slim
COPY --from=build /target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar /experiment.jar
#EXPOSE 8080
ENTRYPOINT ["java","-jar","/experiment.jar"]
#RUN java

#COPY ./developer/projects/ParallelBNs/scripts /parallelbns/scripts
#COPY ./res /parallelbns/res


#ENTRYPOINT scripts/experiments.bash

# COPY . /ParallelBNs
# WORKDIR /ParallelBNs
# RUN javac src/pGESv2/java/org/albacete/simd/Main.java
#CMD ["java", "Main"]