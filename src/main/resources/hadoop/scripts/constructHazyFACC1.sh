#!/bin/bash

JAR=[/path/to/micro/cat/jar/file.jar]
CLASS=poly.hadoop.HRun
LIB_JARS=[/paths/to/other/library/jars/required/by/micro/cat/jar.jar]
REDUCE_TASKS=25
TIMEOUT=600000
JOB_NAME=ConstructHazyFACC1

INPUT_DIRS=[/hdfs/path/to/output/from/constructFACC1Type/],[/hdfs/path/to/ClueWeb09/data/like/data/rtwdata/hazy/ClueWeb09/ClueWebParsed/ClueWeb_English_en0000*]
OUTPUT_DIR=[/hdfs/path/to/ClueWeb09_HazyFACC1/output/data/directory/]

PROPERTIES_FILE=[/hdfs/path/to/properties/configuration/file.properties]
# -D mapred.job.map.memory.mb=8192 -D mapred.child.java.opts='-server -Xmx7168m -Djava.net.preferIPv4Stack=true'

export HADOOP_CLASSPATH=`echo ${LIB_JARS} | sed s/,/:/g`
hadoop dfs -rmr $OUTPUT_DIR
hadoop jar $JAR $CLASS -libjars $LIB_JARS -D mapred.reduce.tasks=$REDUCE_TASKS -D mapred.task.timeout=$TIMEOUT $JOB_NAME $INPUT_DIRS $OUTPUT_DIR $PROPERTIES_FILE