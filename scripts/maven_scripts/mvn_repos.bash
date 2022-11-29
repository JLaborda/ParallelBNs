#!/bin/bash

#mvn install:install-file -DlocalRepositoryPath=repo -DcreateChecksum=true -Dpackaging=jar -Dfile=[your-jar] -DgroupId=[...] -DartifactId=[...] -Dversion=[...]
mvn install:install-file -DlocalRepositoryPath=repos/tetrad -DcreateChecksum=true -Dpackaging=jar -Dfile=lib/tetrad-lib-6.9.0.jar -DgroupId=edu.cmu -DartifactId=tetrad-lib -Dversion=6.9.0