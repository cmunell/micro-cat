#!/bin/bash

JAR=[/path/to/micro/cat/jar/file.jar]
CLASS=poly.hadoop.HRun
LIB_JARS=[/path/to/other/library/jars/required/by/micro/cat/jar.jar]
REDUCE_TASKS=20
TIMEOUT=2400000
JOB_NAME=ConstructFACC1DocumentType

INPUT_DIRS=[/hdfs/path/to/FACC1/data/directory/containing/gz/files/like/ClueWeb09_FACC1/English/1/],[/hdfs/path/to/output/directory/from/constructFreebaseTypeTopic/]
OUTPUT_DIR=[/hdfs/path/to/output/ClueWeb09_FACC1DocumentType/]

PROPERTIES_FILE=[/hdfs/path/properties/configuration/file.properties]

export HADOOP_CLASSPATH=`echo ${LIB_JARS} | sed s/,/:/g`
hadoop dfs -rmr $OUTPUT_DIR
hadoop jar $JAR $CLASS -libjars $LIB_JARS -D mapred.reduce.tasks=$REDUCE_TASKS -D mapred.task.timeout=$TIMEOUT $JOB_NAME $INPUT_DIRS $OUTPUT_DIR $PROPERTIES_FILE