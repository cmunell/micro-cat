package edu.cmu.ml.rtw.micro.cat.data.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.util.NELLUtil;

/**
 * CategoryList represents a (possibly weighted)
 * list of categories (typically used as a label
 * for a noun-phrase or some other piece of 
 * text)
 * 
 * @author Bill McDowell
 *
 */
public class CategoryList {
	public enum Type {
		ALL_NELL_CATEGORIES,
		FREEBASE_NELL_CATEGORIES
	}
	
	private Map<String, Double> categories;
	
	public CategoryList() {
		this.categories = new TreeMap<String, Double>();
	}
	
	public CategoryList(Type type, CatDataTools dataTools) {
		NELLUtil nell = new NELLUtil(dataTools);
		this.categories = new TreeMap<String, Double>();
		if (type == Type.ALL_NELL_CATEGORIES) {
			for (String category : nell.getCategories())
				this.categories.put(category, 1.0);
		} else if (type == Type.FREEBASE_NELL_CATEGORIES) {
			for (String category : nell.getFreebaseCategories())
				this.categories.put(category, 1.0);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public CategoryList(Map<String, Double> weightedLabels) {
		this.categories = new TreeMap<String, Double>();
		this.categories.putAll(weightedLabels);
	}
	
	public CategoryList(List<Pair<String, Double>> weightedLabels) {
		this.categories = new TreeMap<String, Double>();
		for (Pair<String, Double> weightedLabel : weightedLabels)
			this.categories.put(weightedLabel.getFirst(), weightedLabel.getSecond());
	}
	
	public CategoryList(String[] categories, double[] labelWeights, int startIndex) {
		this.categories = new TreeMap<String, Double>();
		
		for (int i = startIndex; i < categories.length; i++) {
			this.categories.put(categories[i], (labelWeights != null) ? labelWeights[i] : 1.0);
		}
		
	}
	
	public CategoryList(String[] categories, int startIndex) {
		this(categories, null, startIndex);
	}
	
	public CategoryList(CategoryList list1, CategoryList list2) {
		this.categories = new TreeMap<String, Double>();
		for (String label : list1.getCategories())
			this.categories.put(label, 1.0);
		for (String label : list2.getCategories())
			this.categories.put(label, 1.0);
	}
	
	public CategoryList(Collection<CategoryList> categoriesLists) {
		this.categories = new TreeMap<String, Double>();
	
		for (CategoryList categoriesList : categoriesLists) {
			for (String label : categoriesList.getCategories())
				this.categories.put(label, 1.0);
		}
	}
	
	public boolean contains(String str) {
		return this.categories.containsKey(str);
	}
	
	public String[] getCategories() {
		return this.categories.keySet().toArray(new String[0]);
	}
	
	public Map<String, Double> getWeightMap() {
		return this.categories;
	}
	
	public int size() {
		return this.categories.size();
	}
	
	public double getCategoryWeight(String label) {
		if (this.categories.containsKey(label))
			return this.categories.get(label);
		return 0.0;
	}
	
	public List<String> getCategoriesAboveWeight(double threshold) {
		List<String> retLabels = new ArrayList<String>();
		
		for (Entry<String, Double> entry : this.categories.entrySet())
			if (entry.getValue() >= threshold)
				retLabels.add(entry.getKey());
		
		return retLabels;
	}
	
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		List<Entry<String, Double>> labelEntries = new ArrayList<Entry<String, Double>>(this.categories.size());
		labelEntries.addAll(this.categories.entrySet());
			
		Collections.sort(labelEntries, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> l1,
					Entry<String, Double> l2) {
				if (l1.getValue() > l2.getValue())
					return -1;
				else if (l1.getValue() < l2.getValue())
					return 1;
				else
					return 0;
			}
		});
			
		for (Entry<String, Double> weightedLabel : labelEntries)
			str.append(weightedLabel.getKey())
				.append(":")
				.append(weightedLabel.getValue())
				.append(",");
		
		if (str.length() > 0)
			str.delete(str.length() - 1, str.length());
		
		return str.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		CategoryList l = (CategoryList)o;
		
		if (l.categories.size() != this.categories.size())
			return false;
		
		return l.categories.keySet().containsAll(this.categories.keySet())
				&& this.categories.keySet().containsAll(l.categories.keySet());
	}
	
	@Override
	public int hashCode() {
		int h = 0;
		for (String label : this.categories.keySet())
			h ^= label.hashCode();
		return h;
	}
	
	public static CategoryList fromString(String str, CatDataTools dataTools) {
		if (str.equals("NONE"))
			str = "";
		
		if (str.equals("ALL_NELL_CATEGORIES")) {
			return new CategoryList(Type.ALL_NELL_CATEGORIES, dataTools);
		} else if (str.equals("FREEBASE_NELL_CATEGORIES")) {
			return new CategoryList(Type.FREEBASE_NELL_CATEGORIES, dataTools);
		} else {
			String[] strParts = str.split(",");
			if (strParts.length == 0 || (strParts.length == 1 && strParts[0].length() == 0))
				return new CategoryList();
				
			if (!strParts[0].contains(":"))
				return new CategoryList(strParts, 0);	
			
			String[] categories = new String[strParts.length];
			double[] labelWeights = new double[strParts.length];
			for (int i = 0; i < strParts.length; i++) {
				String[] labelParts = strParts[i].split(":");
				categories[i] = labelParts[0];
				labelWeights[i] = Double.parseDouble(labelParts[1]);
			}
			
			return new CategoryList(categories, labelWeights, 0);
		}
	}
}
