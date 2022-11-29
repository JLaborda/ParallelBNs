#
# Build stage
#
FROM maven:3.6.3-jdk-8-slim AS build
MAINTAINER Jorge D. Laborda <jorgedaniel.laborda@uclm.es>

COPY src /src
COPY repos /repos
COPY pom.xml /
RUN mvn -f /pom.xml clean package
#COPY --from=build /target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar /experiment.jar
ENTRYPOINT ["java","-jar","/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"]

#
# Package stage
#
# FROM openjdk:8-jre-slim
# COPY --from=build /target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar /experiment.jar
# ENTRYPOINT ["java","-jar","/experiment.jar"]

