#!/bin/bash

PROJECT_PATH="/Users/jdls/developer/projects/ParallelBNs"
JAR_PATH="${PROJECT_PATH}/target/ParallelBNs-1.0-EXPERIMENTS-SNAPSHOT-jar-with-dependencies.jar"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"
PARAMS=$2

#PARAMS="alarm pges /Users/jdls/developer/projects/ParallelBNs/res/networks/alarm.xbif /Users/jdls/developer/projects/ParallelBNs/res/networks/BBDD/alarm.xbif_.csv /Users/jdls/developer/projects/ParallelBNs/res/networks/BBDD/tests/alarm_test.csv 5 250 1 2"
#$2 #"${PROJECT_PATH}/res/params/experiments_alarm.txt"
#JAVA_BIN=java #"/home/jorlabs/java/jdk1.8.0_251/bin/java"
#LIB_PATH="${PROJECT_PATH}/target/lib/"
#SOURCE_PATH="${LIB_PATH}colt.jar:${LIB_PATH}commons-collections-3.1.jar:${LIB_PATH}f2jutil.jar:${LIB_PATH}fusion.jar:${LIB_PATH}Jama-1.0.2.jar:${LIB_PATH}jdepend.jar:${LIB_PATH}jh.jar:${LIB_PATH}junit.jar:${LIB_PATH}jxl.jar:${LIB_PATH}mrjtoolkitstubs.jar:${LIB_PATH}mtj.jar:${LIB_PATH}opt77.jar:${LIB_PATH}pal-1.4.jar:${LIB_PATH}weka.jar:${LIB_PATH}xom-1.1.jar"
#SOURCE_PATH="${LIB_PATH}f2jutil-1.0.jar:${LIB_PATH}fusion-1.0.jar:${LIB_PATH}opt-7.7.jar:${LIB_PATH}pal-1.4.jar:"
#CLASSPATH="${PROJECT_PATH}/target/classes/"


# Run experiment
$JAVA_BIN -Xmx8g -jar $JAR_PATH $PARAMS

#mvn deploy:deploy-file -DgroupId=[GROUP] -DartifactId=[ARTIFACT] -Dversion=[VERS] -Durl=file:./local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=[FILE_PATH]