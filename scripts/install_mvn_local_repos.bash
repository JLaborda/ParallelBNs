#!/bin/bash
LIB_PATH="/Users/jdls/developer/projects/ParallelBNs/lib/"
FUSION_PATH="${LIB_PATH}fusion.jar"
F2J_PATH="${LIB_PATH}f2jutil.jar"
OPT_PATH="${LIB_PATH}opt77.jar"
PAL_PATH="${LIB_PATH}pal-1.4.jar"

mvn install:install-file -DlocalRepositoryPath=repos/fusion -DcreateChecksum=true -Dpackaging=jar -Dfile=$FUSION_PATH -DgroupId=fusion -DartifactId=fusion -Dversion=1.0
mvn install:install-file -DlocalRepositoryPath=repos/f2jutil -DcreateChecksum=true -Dpackaging=jar -Dfile=$F2J_PATH -DgroupId=f2jutil -DartifactId=f2jutil -Dversion=1.0
mvn install:install-file -DlocalRepositoryPath=repos/opt -DcreateChecksum=true -Dpackaging=jar -Dfile=$OPT_PATH -DgroupId=opt -DartifactId=opt -Dversion=7.7
mvn install:install-file -DlocalRepositoryPath=repos/pal -DcreateChecksum=true -Dpackaging=jar -Dfile=$PAL_PATH -DgroupId=pal -DartifactId=pal -Dversion=1.4

#mvn install:install-file -DlocalRepositoryPath=repo -DcreateChecksum=true -Dpackaging=jar -Dfile=[your-jar] -DgroupId=[...] -DartifactId=[...] -Dversion=[...]
#mvn install:install-file -DlocalRepositoryPath=repo -DcreateChecksum=true -Dpackaging=jar -Dfile=[your-jar] -DgroupId=[...] -DartifactId=[...] -Dversion=[...]
#mvn install:install-file -DlocalRepositoryPath=repo -DcreateChecksum=true -Dpackaging=jar -Dfile=[your-jar] -DgroupId=[...] -DartifactId=[...] -Dversion=[...]
