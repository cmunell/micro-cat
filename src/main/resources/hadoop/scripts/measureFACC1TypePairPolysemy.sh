#!/bin/bash

JAR=[/path/to/micro/cat/jar/file.jar]
CLASS=poly.hadoop.HRun
LIB_JARS=[/paths/to/other/library/jars/required/by/micro/cat/jar.jar]
REDUCE_TASKS=20
TIMEOUT=600000
JOB_NAME=MeasureFACC1TypePairPolysemy

INPUT_DIRS=[/hdfs/path/to/output/from/measureFACC1TypePhrasePolysemy/]
OUTPUT_DIR=[/hdfs/path/to/output/ClueWeb09_FACC1TypePairPolysemy/data/directory/]

PROPERTIES_FILE=[/hdfs/path/to/properties/configuration/file.properties]

export HADOOP_CLASSPATH=`echo ${LIB_JARS} | sed s/,/:/g`
hadoop dfs -rmr $OUTPUT_DIR
hadoop jar $JAR $CLASS -libjars $LIB_JARS -D mapred.reduce.tasks=$REDUCE_TASKS -D mapred.task.timeout=$TIMEOUT $JOB_NAME $INPUT_DIRS $OUTPUT_DIR $PROPERTIES_FILE