package edu.cmu.ml.rtw.micro.cat.hadoop;

import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

@SuppressWarnings("deprecation")
public class HRun {
	public static class PolyMapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {
		protected CatProperties properties;
		protected CatDataTools dataTools;
		
		@Override
		protected void setup(Context context) {
			this.properties = new CatProperties();
			this.dataTools = new CatDataTools(new OutputWriter(), this.properties);
		}
	}
	
	public static class PolyReducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT> extends Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT> {
		protected CatProperties properties;
		protected CatDataTools dataTools;
		
		@Override
		protected void setup(Context context) {
			this.properties = new CatProperties();
			this.dataTools = new CatDataTools(new OutputWriter(), this.properties);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		String jobName = otherArgs[0];
		String[] inputPaths = otherArgs[1].split(",");
		String outputPath = otherArgs[2];
		String[] cacheFiles = otherArgs[3].split(",");

		Job job = new Job(conf, jobName);
		conf = job.getConfiguration();
		for (String cacheFile : cacheFiles) {
			DistributedCache.addCacheFile(new URI(cacheFile), conf);
		}
		DistributedCache.createSymlink(conf);
		
		if (jobName.equals("ConstructHazyFACC1")) {
			job.setJarByClass(HConstructHazyFACC1.class);
			job.setMapperClass(HConstructHazyFACC1.Mapper.class);
			job.setReducerClass(HConstructHazyFACC1.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("ConstructFACC1DocumentType")) {
			job.setJarByClass(HConstructFACC1DocumentType.class);
			job.setMapperClass(HConstructFACC1DocumentType.Mapper.class);
			job.setReducerClass(HConstructFACC1DocumentType.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("ConstructFACC1Type")) {
			job.setJarByClass(HConstructFACC1Type.class);
			job.setMapperClass(HConstructFACC1Type.Mapper.class);
			job.setReducerClass(HConstructFACC1Type.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("ConstructFreebaseTypeTopic")) {
			job.setJarByClass(HConstructFreebaseTypeTopic.class);
			job.setMapperClass(HConstructFreebaseTypeTopic.Mapper.class);
			job.setReducerClass(HConstructFreebaseTypeTopic.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("ConstructPolysemyDataSet")) {
			job.setJarByClass(HConstructPolysemyDataSet.class);
			job.setMapperClass(HConstructPolysemyDataSet.Mapper.class);
			job.setReducerClass(HConstructPolysemyDataSet.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("MeasureFACC1TypePhrasePolysemy")) {
			job.setJarByClass(HMeasureFACC1TypePhrasePolysemy.class);
			job.setMapperClass(HMeasureFACC1TypePhrasePolysemy.Mapper.class);
			job.setReducerClass(HMeasureFACC1TypePhrasePolysemy.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("MeasureFACC1TypePairPolysemy")) {
			job.setJarByClass(HMeasureFACC1TypePairPolysemy.class);
			job.setMapperClass(HMeasureFACC1TypePairPolysemy.Mapper.class);
			job.setReducerClass(HMeasureFACC1TypePairPolysemy.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else if (jobName.equals("MeasureFACC1TypePolysemy")) {
			job.setJarByClass(HMeasureFACC1TypePolysemy.class);
			job.setMapperClass(HMeasureFACC1TypePolysemy.Mapper.class);
			job.setReducerClass(HMeasureFACC1TypePolysemy.Reducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
		} else {
			System.err.println("Invalid job name: " + jobName);
			System.exit(1);
		}
		
		for (String inputPath : inputPaths)
			FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
