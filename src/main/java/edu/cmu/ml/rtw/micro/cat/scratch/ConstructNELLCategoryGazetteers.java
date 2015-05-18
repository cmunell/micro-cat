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
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

public class ConstructNELLCategoryGazetteers {
	public static void main(String[] args) {
		String inputFilePath = args[0];
		
		CatProperties properties = new CatProperties();
	
		Map<String, List<String>> generalizationsToCategories = readInput(inputFilePath, "generalizations");
		Map<String, List<String>> mutexesToCategories = readInput(inputFilePath, "mutexpredicates");
		
		outputGazetteer(generalizationsToCategories, properties.getNELLCategoryGeneralizationGazetteerPath());
		outputGazetteer(mutexesToCategories, properties.getNELLCategoryMutexGazetteerPath());
	}
	
	private static Map<String, List<String>> readInput(String inputFilePath, String type) {
		Map<String, List<String>> idsToCategories = new HashMap<String, List<String>>();
		
		BufferedReader r = FileUtil.getFileReader(inputFilePath);
		try {
			String line = null;
			int l = 0;
			while ((line = r.readLine()) != null) {
				if (l % 1000 == 0)
					System.out.println("Reading " + type + " line: " + l);
				String[] lineParts = line.split("[\\s]+");
				if (lineParts.length < 3 || !lineParts[1].equals(type))
					continue;
				
				String category = lineParts[0].split(":")[1];
				String id = lineParts[2].split(":")[1];
				
				if (!idsToCategories.containsKey(id))
					idsToCategories.put(id, new ArrayList<String>());
				idsToCategories.get(id).add(category);
				
				l++;
			}
			
			r.close();
		} catch (Exception e) {
			
		}
		
		return idsToCategories;
	}
	
	private static boolean outputGazetteer(Map<String, List<String>> idsToValues, String path) {
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(path));
		
			for (Entry<String, List<String>> entry : idsToValues.entrySet()) {
				System.out.println("Writing id: " + entry.getKey());
				
				w.write(entry.getKey() + "\t");
				StringBuilder str = new StringBuilder();
				for (String value : entry.getValue()) {
					str.append(value).append("\t");
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
