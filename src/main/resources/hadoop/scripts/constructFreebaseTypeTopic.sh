#!/bin/bash

JAR=[/path/to/micro/cat/jar/file.jar]
CLASS=poly.hadoop.HRun
LIB_JARS=[/paths/to/other/library/jars/required/by/micro/cat/jar.jar]
REDUCE_TASKS=10
TIMEOUT=1200000
JOB_NAME=ConstructFreebaseTypeTopic

INPUT_DIRS=[/hdfs/path/to/directory/containing/freebase/rdf/dump]

OUTPUT_DIR=[/hdfs/path/to/output/directory/FreebaseTypeTopic/]

PROPERTIES_FILE=[/hdfs/path/to/properties/configuration/file.properties]
OTHER_CACHE_FILES=[/hdfs/path/to/FreebaseNELLCategory.gazetteer]

export HADOOP_CLASSPATH=`echo ${LIB_JARS} | sed s/,/:/g`
hadoop dfs -rmr $OUTPUT_DIR
hadoop jar $JAR $CLASS -libjars $LIB_JARS -D mapred.reduce.tasks=$REDUCE_TASKS -D mapred.task.timeout=$TIMEOUT $JOB_NAME $INPUT_DIRS $OUTPUT_DIR $PROPERTIES_FILE,$OTHER_CACHE_FILES
