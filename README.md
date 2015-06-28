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

* *edu.cmu.ml.rtw.micro.cat.util* - for utilities and configuration

## Retraining the noun-phrase categorizer ##

You can retrain the classification models used in the noun-phrase 
categorization micro-reader by running 
*edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinaryNELLNormalized*.  
Assuming that you've cloned the repository, and you're compiling
the source, you should first copy the *src/main/resources/cat.properties*
configuration file to the top-level directory of the project.  Before
training, you need to fill out this configuration file with 
paths pointing to the relevant data locations on the system.  At the
minimum, you should fill in the following fields:

* *contextInputDirPath* - path to the *src/main/resources/contexts*.  The
value *contexts* value that's already there should work.

* *experimentOutputDirPath* - path to the directory where the experiment
output model files and results should be stored.

* *hazyFacc1DataDirPath* - Path to the HazyFACC1 data.  This should be
*/nell/data/parses/HazyFACC1/English/1/en0000/* on the rtw machines.

* *NELLDataFileDirPath* - Path to directory where you want to store the 
noun-phrase categorization data generated from HazyFACC1. 

* All fields ending with *GazetteerPath* - paths to gazetteers on the file
system.  The relative paths that are already there should work, but if 
they don't, then you can use absolute paths to 
*/nell/data/micro/micro-cat-data/src/main/resources/gazetteers/* on the
rtw machines.

After you've filled out the properties file, you can run by executing something
like:

    cd [top-level directory of the project]
    export MAVEN_OPTS=-Xmx190G (Can probably get away with a little less than this)
    mvn clean compile -U
    mvn exec:java -Dexec.mainClass="edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinaryNELLNormalized" -Dexec.args="--experimentName=LRBasel2"

You can change the *experimentName* value to be the name of any of the ctx script
files in *src/main/resources/contexts/GSTBinaryNELLNormalized*.  You can also
create and use other ctx scripts that have different feature sets and models.

The models, features, and evaluation results will be stored in the 
*experimentOutputDirPath* directory determined by the properties file.  The files
in that directory are:

* **.model.out.[category]* - serialized *[category]* model
* **.data.out.[category]* - data where the model for *[category]* made the wrong classification
* **.results.out.[category]* - evaluation results for the *[category]* model
* **.debug.out.[category]* - debug messages output when training the *[category]* model
* **.model.out* - serialized model feature vocabularies
* **.results.out* - evaluation results aggregated across all categories
* **.debug.out* - general debug messages

If you want to replace the models used by the micro-reader in the NELL micro-reading pipeline
then you can copy all the **.model.out* files to 
*/nell/data/micro/micro-cat-data/src/main/resources/models/GSTBinaryNELLNormalized/HazyFacc1_AllNELL_c90_e2000/*
on the rtw machines, and redeploy the *micro-cat-data* project.

There's additional documentation for how the training/evaluation process works
at the top of the *edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinaryNELLNormalized*
source file.

If all else fails, this is already set up for training in 
*/home/wmcdowel/NELL/micro/Projects/micro-cat* on the rtw machines, and you can run it
using the script at */home/wmcdowel/NELL/micro/Jobs/trainGSTBinaryNELLNormalized.sh*.
 
## Training other mention classifiers ##

You can train additional classifiers on labeled token spans using the 
*edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinary*.  First, set up
your *cat.properties* configuration file as described in the previous 
section (except that you don't need to fill in the path to the HazyFACC1 data set).
Next, create train, dev, and test document sets in the NELL micro-reading
annotation format, and add them to 
*edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.DocumentSetNLPFactory*.  These
document sets should have mention token spans labeled with some annotation
for which you want to train classifiers.  Declare a constant for this
custom annotation type in *edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat*,
and add an instance of this annotation type to 
*edu.cmu.ml.rtw.micro.cat.data.CatDataTools*. Then, you can train and
evaluate classifiers by doing something like:

    cd [top-level directory of micro-cat]
    mvn clean compile -U
    mvn exec:java -Dexec.mainClass="edu.cmu.ml.rtw.micro.cat.scratch.TrainGSTBinary" -Dexec.args="--experimentName=[name of ctx script file]  --trainDocumentSetName=[train set] --devDocumentSetName=[dev set] --testDocumentSetName=[test set] --categoryType=[label annotation type]

Your ctx script file should be in *src/main/resources/contexts/GSTBinary*.  
The training, dev, and test sets should be the names of document sets declared as constants 
in *DocumentSetNLPFactory*.  The *categoryType* should be the string
name of the annotation type used for labels in the document sets.  See
*/home/wmcdowel/NELL/micro/Jobs/trainGSTBinary.sh* on the rtw machines 
for an example of how this is set up
to train Freebase type noun-phrase classifiers on the the CoNLL-YAGO data.

## Running the noun-phrase categorizer ##

*edu.cmu.ml.rtw.micro.cat.scratch.NELLCategorizeNPMentions* provides
a command-line version of the noun-phrase NELL categorization 
micro-reader.  You can get information on the possible arguments for
this tool by running with the '--help' option.