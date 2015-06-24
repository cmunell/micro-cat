package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

/**
 * ConstructNounPhraseNELLCategoryGazetteer converts the
 * noun-phrase to NELL category mapping from 
 * http://rtw.ml.cmu.edu/resources/nps/NELL.ClueWeb09.v1.nps.csv.gz
 * into a format that can be read as a 
 * edu.cmu.ml.rtw.micro.generic.data.Gazetteer.
 * 
 * @author Bill McDowell
 *
 */
public class ConstructNounPhraseNELLCategoryGazetteer {
	public static void main(String[] args) {
		String inputFilePath = args[0];
		Map<String, List<Pair<String, Double>>> categoriesToPhrases = readInputFile(inputFilePath);
		outputGazetteer(categoriesToPhrases);
	}
	
	private static Map<String, List<Pair<String, Double>>> readInputFile(String path) {
		Map<String, List<Pair<String, Double>>> categoriesToPhrases = new HashMap<String, List<Pair<String, Double>>>();
		BufferedReader r = FileUtil.getFileReader(path);
		try {
			String line = null;
			int l = 0;
			while ((line = r.readLine()) != null) {
				if (l % 1000 == 0)
					System.out.println("Reading line: " + l);
				String[] lineParts = line.split("\t");
				String np = lineParts[0];
				for (int i = 1; i < lineParts.length; i++) {
					String[] categoryParts = lineParts[i].split(" ");
					String category = categoryParts[0];
					double weight = Double.valueOf(categoryParts[1]);
					
					if (!categoriesToPhrases.containsKey(category))
						categoriesToPhrases.put(category, new ArrayList<Pair<String, Double>>());
					categoriesToPhrases.get(category).add(new Pair<String, Double>(np, weight));
				}
				l++;
			}
			
			r.close();
		} catch (Exception e) {
			
		}
		
		return categoriesToPhrases;
	}
	
	private static boolean outputGazetteer(Map<String, List<Pair<String, Double>>> categoriesToPhrases) {
		CatProperties properties = new CatProperties();
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(properties.getNounPhraseNELLCategoryGazetteerPath()));
		
			for (Entry<String, List<Pair<String, Double>>> entry : categoriesToPhrases.entrySet()) {
				System.out.println("Writing category: " + entry.getKey());
				
				w.write(entry.getKey() + "\t");
				StringBuilder str = new StringBuilder();
				for (Pair<String, Double> phrase : entry.getValue()) {
					str.append(phrase.getFirst()).append(":").append(phrase.getSecond()).append("\t");
				}
				w.write(str.toString().trim());
				w.write("\n");
			}
			
			w.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
