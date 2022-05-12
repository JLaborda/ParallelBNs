#!/bin/bash
docker pull jorlabs/parallelbns:feature-experiments
docker run -v $(pwd)/res:/res -v $(pwd)/results:/results --rm parallelbns 1 /res/params/hyperparams_clustering_alarm.txt
# docker run -v $(pwd)/res:/res -v $(pwd)/results:/results --rm parallelbns alarm pges /res/networks/alarm.xbif /res/networks/BBDD/alarm.xbif_.csv /res/networks/BBDD/tests/alarm_test.csv 5 250 1 3
# On Windows
# docker run -v ${PWD}/res:/res -v ${PWD}/results:/results --rm parallelbns 1 /res/params/hyperparams_clustering_alarm.txt
