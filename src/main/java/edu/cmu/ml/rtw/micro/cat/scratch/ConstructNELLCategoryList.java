package edu.cmu.ml.rtw.micro.cat.scratch;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

/**
 * ConstructNELLCategoryList produces a comma-separated
 * list of all the categories in the NELL ontology
 * (taken from 
 * http://rtw.ml.cmu.edu/resources/results/08m/NELL.08m.930.ontology.csv.gz).
 * 
 * @author Bill McDowell
 *
 */
public class ConstructNELLCategoryList {
	public static void main(String[] args) {
		CatDataTools dataTools = new CatDataTools();
		Gazetteer categories = dataTools.getGazetteer("NELLCategoryGeneralization");
		StringBuilder str = new StringBuilder();
		for (String category : categories.getValues())
			str.append("\"").append(category).append("\", ");
		str.delete(str.length()-2, str.length());
		System.out.println(str);
	}
}
