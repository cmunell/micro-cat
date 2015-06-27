# micro-cat

This repository contains contains code for the 
NELL noun-phrase categorization micro-reader and 
for training/evaluating other kinds of mention
categorization micro-readers.  It relies heavily on
the https://github.com/cmunell/micro-util 
library in the sense that it uses many of its classes,
and many of the classes in micro-cat are just extensions
of classes in micro-util.  It would be useful to 
read micro-util's README
documentation before you read this README to get
an idea about why this project is organized as it is.

## Setup and build ##

You can get the source code for the project by running

    git clone https://github.com/cmunell/micro-cat.git
    
and then build by running

    mvn compile 
    
from the root directory of the project assuming that you have
Maven setup and configured to access the internal RTW repository
as described at http://rtw.ml.cmu.edu/wiki/index.php?title=Maven.

## Layout of the project ##

The code is organized into the following packages in 
the *src* directory:

* *edu.cmu.ml.rtw.micro.cat.data* - miscellaneous tools for
cleaning and manipulating the data.

* *edu.cmu.ml.rtw.micro.cat.data.annotation* - for
representing general annotation and data.

* *edu.cmu.ml.rtw.micro.cat.data.annotation.nlp* - for representing
NLP-specific annotations and data.  The *NELLMentionCategorizer* 
noun-phrase categorization micro-reader is also here.

* *edu.cmu.ml.rtw.micro.cat.hadoop* - for Hadoop jobs.  Currently,
the main job in here merges the Hazy and FACC1
(lemurproject.org/clueweb09/FACC1/) annotations of ClueWeb09
into a single NLP annotated data set with noun-phrases labeled
 with their Freebase topics and types.  This merging is done
 by a sequence of Hadoop tasks each of which is run through
 the *edu.cmu.ml.rtw.micro.cat.hadoop.HRun* wrapper. The sequence
 is: *HConstructFreebaseTypeTopic* > *HConstructFACC1DocumentType* > 
 *HConstructFACC1Type* > *HConstructHazyFACC1*.
There are script templates for running each of these steps in 
*src/main/resources/hadoop/scripts*.  You can find additional documentation
for each step in its source code file.

* *edu.cmu.ml.rtw.micro.cat.model.evaluation.metric* - for model
evaluations

* *edu.cmu.ml.rtw.micro.cat.scratch* - for various command-line
programs.  For example, some construct data sets, and
others perform the model training and evaluation procedures.  The
NELL noun-phrase categorization command-line tool is also here
in the *NELLCategorizeNPMentions* class.

* *edu.cmu.ml.rtw.micro.cat.util* - for utilities and configu

