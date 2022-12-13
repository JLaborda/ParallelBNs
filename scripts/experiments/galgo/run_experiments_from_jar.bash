#!/bin/bash

PROJECT_PATH="/home/jorlabs/projects/ParallelBNs"
JAR_PATH="${PROJECT_PATH}/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"

INDEX=""
if [ -z "$PBS_ARRAY_INDEX" ]; then INDEX=$1; else echo INDEX=$PBS_ARRAY_INDEX; fi
if [ -z "$PARAMS" ]; then PARAMS=$2; fi
if [ -z "$THREADS" ]; then INDEX=$3; fi
if [ -z "$FILE" ]; then FILE=$JAR_PATH; fi

PARAMS=$2
THREADS=$3


# Run experiment
cd $CWD
$JAVA_BIN -Xmx8g -jar $FILE $INDEX $PARAMS $THREADS

#mvn deploy:deploy-file -DgroupId=[GROUP] -DartifactId=[ARTIFACT] -Dversion=[VERS] -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=[FILE_PATH]