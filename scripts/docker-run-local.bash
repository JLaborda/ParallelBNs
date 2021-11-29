#!/bin/bash
docker run -v $(pwd)/res:/res -v $(pwd)/results:/results --rm parallelbns alarm pges /res/networks/alarm.xbif /res/networks/BBDD/alarm.xbif_.csv /res/networks/BBDD/tests/alarm_test.csv 5 250 1 3
