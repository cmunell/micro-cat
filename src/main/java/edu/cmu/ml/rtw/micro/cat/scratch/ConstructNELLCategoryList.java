package edu.cmu.ml.rtw.micro.cat.scratch;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

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
