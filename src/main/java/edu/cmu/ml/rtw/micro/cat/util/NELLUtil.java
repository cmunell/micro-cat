package edu.cmu.ml.rtw.micro.cat.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

/**
 * NELLUtil gives various methods for interacting with
 * various NELL components, mappings, ontology, etc.
 * 
 * @author Bill McDowell
 *
 */
public class NELLUtil {
	private CatDataTools dataTools;
	
	private Gazetteer categoryGeneralization;
	private Gazetteer categoryMutex;
	
	private Gazetteer prefixAbbreviation;
	private Gazetteer suffixAbbreviation;
	private Gazetteer nounPhraseBadPrefix;
	private Gazetteer nounPhraseBadSuffix;
	private Gazetteer nounPhraseBadToken;
	private Gazetteer nounPhrasePhraseDictionary;
	private Gazetteer nounPhraseFnWord;
	private Gazetteer nounPhraseStopWord;
	
	public NELLUtil(CatDataTools dataTools) {
		this.dataTools = dataTools;
		
		this.categoryGeneralization = dataTools.getGazetteer("NELLCategoryGeneralization");
		this.categoryMutex = dataTools.getGazetteer("NELLCategoryMutex");
		
		this.prefixAbbreviation = dataTools.getGazetteer("NELLPrefixAbbreviation");
		this.suffixAbbreviation = dataTools.getGazetteer("NELLSuffixAbbreviation");
		this.nounPhraseBadPrefix = dataTools.getGazetteer("NELLNounPhraseBadPrefix");
		this.nounPhraseBadSuffix = dataTools.getGazetteer("NELLNounPhraseBadSuffix");
		this.nounPhraseBadToken = dataTools.getGazetteer("NELLNounPhraseBadToken");
		this.nounPhrasePhraseDictionary = dataTools.getGazetteer("NELLNounPhrasePhraseDictionary");
		this.nounPhraseFnWord = dataTools.getGazetteer("NELLNounPhraseFnWord");
		this.nounPhraseStopWord = dataTools.getGazetteer("NELLNounPhraseStopWord");
	}
	
	public List<String> getFreebaseCategories() {
		Gazetteer g = this.dataTools.getGazetteer("FreebaseNELLCategory");
		Set<String> freebaseLabels = g.getValues();
		Set<String> nellCategories = new HashSet<String>();
		for (String freebaseLabel : freebaseLabels) {
			nellCategories.addAll(g.getIds(freebaseLabel));
		}
		
		return new ArrayList<String>(nellCategories);
	}
	
	public List<String> getCategories() {
		return new ArrayList<String>(this.categoryGeneralization.getValues());
	}
	
	/**
	 * @param nounPhrase
	 * @param confidenceThreshold
	 * @return NELL categories for the nounPhrase that have confidence 
	 * greater than confidenceThreshold according to the 
	 * NounPhraseNELLCategory gazetteer which was constructed 
	 * from the flat noun-phrase to category mapping at
	 * http://rtw.ml.cmu.edu/resources/nps/NELL.ClueWeb09.v1.nps.csv.gz 
	 * linked from http://rtw.ml.cmu.edu/rtw/nps 
	 * 
	 */
	public List<String> getNounPhraseNELLCategories(String nounPhrase, double confidenceThreshold) {
		Gazetteer nounPhraseCategory = this.dataTools.getGazetteer("NounPhraseNELLCategory");
		
		List<Pair<String, Double>> weightedCategories = nounPhraseCategory.getWeightedIds(nounPhrase);
		List<String> categories = new ArrayList<String>();
		if (weightedCategories == null)
			return categories;
		
		for (Pair<String, Double> weightedCategory : weightedCategories)
			if (weightedCategory.getSecond() >= confidenceThreshold)
				categories.add(weightedCategory.getFirst());
		
		return categories;
	}
	
	public List<Pair<String, Double>> getNounPhraseNELLWeightedCategories(String nounPhrase, double confidenceThreshold) {
		Gazetteer nounPhraseCategory = this.dataTools.getGazetteer("NounPhraseNELLCategory");
		
		List<Pair<String, Double>> weightedCategories = nounPhraseCategory.getWeightedIds(nounPhrase);
		List<Pair<String, Double>> retCategories = new ArrayList<Pair<String, Double>>();
		if (weightedCategories == null)
			return retCategories;
		
		for (Pair<String, Double> weightedCategory : weightedCategories)
			if (weightedCategory.getSecond() >= confidenceThreshold)
				retCategories.add(weightedCategory);
		
		return retCategories;
	}
	
	public boolean isNounPhrasePolysemous(String nounPhrase, double confidenceThreshold) {
		return areCategoriesMutuallyExclusive(
				getNounPhraseNELLCategories(nounPhrase, confidenceThreshold)
				);
	}
	
	public boolean areCategoriesMutuallyExclusive(Collection<String> categories) {
		Set<String> doneCategories = new HashSet<String>();
		for (String c1 : categories) {
			doneCategories.add(c1);
			for (String c2 : categories) {
				if (!doneCategories.contains(c2))
					continue;
				if (areCategoriesMutuallyExclusive(c1, c2))
					return true;
				
			}
		}
		
		return false;
	}

	public boolean areCategoriesMutuallyExclusive(String category1, String category2) { 
		// FIXME: This can be done much more efficiently if the mutex gazetteer
		// is correctly constructed, but 
		// it currently slows things under the assumption that
		// not all mutexes are inherited from parent categories to child categories
		// in the gazetteer, and not all mutex relationships are 
		// symmetric in the gazetteer
		
		Set<String> gen1 = getCategoryGeneralizations(category1);
		Set<String> gen2 = getCategoryGeneralizations(category2);
		
		if (gen1.contains(category2) || gen2.contains(category1))
			return false;
		
		Set<String> mutex1 = getCategoryMutualExclusions(category1);
		Set<String> mutex2 = getCategoryMutualExclusions(category2);
		
		if (mutex1.contains(category2) || mutex2.contains(category1))
			return true;
		
		mutex1.retainAll(gen2);
		if (mutex1.size() > 0)
			return true;
		
		mutex2.retainAll(gen1);
		if (mutex2.size() > 0)
			return true;
		
		return false;
	}
	
	public Set<String> getCategoryGeneralizations(String category) {
		Set<String> generalizations = new HashSet<String>();
		Stack<String> toVisit = new Stack<String>();
		toVisit.push(category);
		generalizations.add(category);
		
		while (!toVisit.isEmpty()) {
			String curCategory = toVisit.pop();
			
			List<String> curGens = this.categoryGeneralization.getIds(curCategory);
			if (curGens == null)
				continue;
			for (String curGen : curGens) {
				if (!generalizations.contains(curGen)) {
					toVisit.push(curGen);
					generalizations.add(curGen);
				}
			}
		}
		
		return generalizations;
	}
	
	public Set<String> getCategoryMutualExclusions(String category) {
		Set<String> mutexes = new HashSet<String>();
		Set<String> generalizations = getCategoryGeneralizations(category);
		if (generalizations == null)
			return mutexes;
		
		for (String generalization : generalizations) {
			List<String> ids = this.categoryMutex.getIds(generalization);
			if (ids != null)
				mutexes.addAll(ids);
		}
		
		return mutexes;
	}
	
	// Note that this code is a modified version of the code from
	// edu.cmu.ml.rtw.mapred.cbl.ExtractorBase in the OntologyLearner NELL Java 
	// project.  It's kind of a mess, and it would be nice to fix it up
	// into a more easily readable form at some point.
	public List<TokenSpan> extractNounPhrases(DocumentNLP document) {
		List<TokenSpan> npSpans = new ArrayList<TokenSpan>();
		int sentenceCount = document.getSentenceCount();
		for (int i = 0; i < sentenceCount; i++) {
			npSpans.addAll(extractNounPhrases(document, i));
		}
		
		return npSpans;
	}
	
	public List<TokenSpan> extractNounPhrases(DocumentNLP document, int sentenceIndex) {
		Set<TokenSpan> spans = new HashSet<TokenSpan>();
		int tokenCount = document.getSentenceTokenCount(sentenceIndex);
			
		for (int j = 0; j < tokenCount; j++) {
			extractNounPhrasesEndingAt(document, sentenceIndex, j, spans);
			extractNounPhrasesStartingAt(document, sentenceIndex, j, spans);
		}
		
		List<TokenSpan> retSpans = new ArrayList<TokenSpan>(spans.size());
		for (TokenSpan span1 : spans) {
			boolean subspan = false;
			for (TokenSpan span2 : spans) {
				if (!span1.equals(span2) && span2.containsTokenSpan(span1)) {
					subspan = true;
					break;
				}
			}
			
			if (!subspan)
				retSpans.add(span1);
		}
	
		return retSpans;
	}
	
	private void extractNounPhrasesEndingAt(DocumentNLP document, int sentenceIndex, int endIndex, Set<TokenSpan> spans) {
		int tokenCount = document.getSentenceTokenCount(sentenceIndex);
	
		// Add single numbers
		if (document.getPoSTag(sentenceIndex, endIndex) == PoSTag.CD) {
			TokenSpan numberSpan = new TokenSpan(document, sentenceIndex, endIndex, endIndex + 1);
			if (!spanContainsBadTokens(numberSpan)) {
				// Add the partial match (consume DT if its there).
				if (endIndex - 1 >= 0 && document.getPoSTag(sentenceIndex, endIndex - 1) == PoSTag.DT) {
					spans.add(new TokenSpan(document, sentenceIndex, endIndex - 1, endIndex + 1));
				} else {
					spans.add(numberSpan);
				}
			}
	    }
		
		int i = endIndex;

		// look backward for DT? (CD|JJ|NN)* NN
		if (PoSTagClass.classContains(PoSTagClass.NN, document.getPoSTag(sentenceIndex, i))) {
			TokenSpan nnSpan = new TokenSpan(document, sentenceIndex, i, i + 1);
			i--;
			
			if (!spanContainsBadTokens(nnSpan)) {
				// Add the partial match (consume DT if its there).
				if (i >= 0 && document.getPoSTag(sentenceIndex, i) == PoSTag.DT) {
					spans.add(new TokenSpan(document, sentenceIndex, i, nnSpan.getEndTokenIndex()));
				} else {
					spans.add(nnSpan);
				}
			}
            
			while (i >= 0 && isTokenInNonNNPNounPhrasePoSTagClass(document, sentenceIndex, i)) {
				i--;
				if (i >= 0 && document.getPoSTag(sentenceIndex, i) == PoSTag.DT) {
					spans.add(new TokenSpan(document, sentenceIndex, i, nnSpan.getEndTokenIndex()));
				} else {
					spans.add(new TokenSpan(document, sentenceIndex, i + 1, nnSpan.getEndTokenIndex()));
				}
			}
		}
		
		// look backward for DT? [A-Z]/* ((/IN|DT|CC|POS{1,3} [A-Z]/*)+)*
		// look forward to see if we're inside a complex NP
		// if the next three words are function words, return.
		i = endIndex + 1;
		if (endIndex < tokenCount - 2) {
			int j = 0;
			for (; j < 3 && (i + j) < tokenCount && PoSTagClass.classContains(PoSTagClass.FN, document.getPoSTag(sentenceIndex, i+j)); j++) {
			}
			if (j > 0 && (i + j) < tokenCount && Character.isUpperCase(document.getTokenStr(sentenceIndex, i + j).charAt(0)))
				return;
		}
		
		i = endIndex;
		int firstNPIndex = -1;
		boolean hasFn = false;
		while (i >= 0) {
			if (isNounPhraseToken(document, sentenceIndex, i, 0, endIndex, i == 0 && !hasFn)) {
				firstNPIndex = i;
				i--;
				for (int j = 0; j < 3 && i >= 0 && PoSTagClass.classContains(PoSTagClass.FN, document.getPoSTag(sentenceIndex, i)); j++) {
	                hasFn = true;
					i--;
	            }	
			} else {
				break;
			}
		}
		
		if (firstNPIndex < 0)
			return;
		
		TokenSpan npSpan = new TokenSpan(document, sentenceIndex, firstNPIndex, endIndex + 1);
		if (spanContainsBadTokens(npSpan))
			return;
		
		// Segment complex np
		while (npSpan.getEndTokenIndex() - npSpan.getStartTokenIndex() > 1
				&& spanContainsFnWord(npSpan) 
				&& !this.nounPhrasePhraseDictionary.contains(npSpan.toString())) {
			int newStartTokenIndex = npSpan.getStartTokenIndex();
			// Chop off last capitalized piece
			for (; newStartTokenIndex < npSpan.getEndTokenIndex() - 1; newStartTokenIndex++)
				if (!Character.isUpperCase(document.getTokenStr(npSpan.getSentenceIndex(), newStartTokenIndex).charAt(0)))
					break;
			// Chop off last uncapitalized piece
			for (; newStartTokenIndex < npSpan.getEndTokenIndex() - 1; newStartTokenIndex++)
				if (Character.isUpperCase(document.getTokenStr(npSpan.getSentenceIndex(), newStartTokenIndex).charAt(0)))
					break;
		
			npSpan = new TokenSpan(npSpan.getDocument(), npSpan.getSentenceIndex(), newStartTokenIndex, npSpan.getEndTokenIndex());
		}
	    
	    if (npSpan.getStartTokenIndex() > 0 && document.getPoSTag(npSpan.getSentenceIndex(), npSpan.getStartTokenIndex() - 1) == PoSTag.DT)
	    	npSpan = new TokenSpan(npSpan.getDocument(), npSpan.getSentenceIndex(), npSpan.getStartTokenIndex()-1, npSpan.getEndTokenIndex());
	    
	    if (PoSTagClass.classContains(PoSTagClass.JJ, document.getPoSTag(npSpan.getSentenceIndex(), npSpan.getEndTokenIndex() - 1)))
	    	return;
		
		spans.add(npSpan);
	}
	
	private void extractNounPhrasesStartingAt(DocumentNLP document, int sentenceIndex, int startIndex, Set<TokenSpan> spans) {
		int tokenCount = document.getSentenceTokenCount(sentenceIndex);
		
		if (startIndex > tokenCount - 2)
			return;
		
		// return null here because otherwise we get duplicates with different boundaries
		if (startIndex > 0 && document.getPoSTag(sentenceIndex, startIndex - 1) == PoSTag.DT)
			return;

		int i = startIndex;	
		
		// look forward for DT? [A-Z]/* ((IN|DT|CC|POS{1,3} [A-Z]/*)+)*
		if (i < tokenCount && startIndex > 0 && document.getPoSTag(sentenceIndex, startIndex - 1) == PoSTag.DT) {
			i++;
		}
		
		int lastNPIndex = -1;
		while (i < tokenCount) {
			if (isNounPhraseToken(document, sentenceIndex, i, startIndex, tokenCount - 1, false)) {
				lastNPIndex = i;
				i++;
				for (int j = 0; j < 3 && i < tokenCount && PoSTagClass.classContains(PoSTagClass.FN, document.getPoSTag(sentenceIndex, i)); j++) {
	                i++;
	            }	
				
			} else {
				break;
			}
		}
        
		if (lastNPIndex < 0)
			return;
		
		if ((lastNPIndex > 0) && (
				(lastNPIndex < tokenCount - 1
					&& document.getTokenStr(sentenceIndex, lastNPIndex + 1).equals(".")
					&& this.suffixAbbreviation.contains(document.getTokenStr(sentenceIndex, lastNPIndex)))
				||
				(lastNPIndex < tokenCount - 1
					&& document.getTokenStr(sentenceIndex, lastNPIndex + 1).equals(".")
					&& this.prefixAbbreviation.contains(document.getTokenStr(sentenceIndex, lastNPIndex)))
				||
				(lastNPIndex < tokenCount - 2
					&& document.getTokenStr(sentenceIndex, lastNPIndex + 1).equals(",")
					&& this.suffixAbbreviation.contains(document.getTokenStr(sentenceIndex, lastNPIndex + 2)))
				||
				(startIndex > 0
					&& document.getTokenStr(sentenceIndex, startIndex - 1).equals(",")
					&& this.suffixAbbreviation.contains(document.getTokenStr(sentenceIndex, startIndex)))
			)) {
			return;
		}
		
		TokenSpan npSpan = new TokenSpan(document, sentenceIndex, startIndex, lastNPIndex + 1);
		if (spanContainsBadTokens(npSpan))
			return;
		
		// Segment complex np
		while (npSpan.getEndTokenIndex() - npSpan.getStartTokenIndex() > 1
				&& spanContainsFnWord(npSpan) 
				&& !this.nounPhrasePhraseDictionary.contains(npSpan.toString())) {
			int newEndTokenIndex = npSpan.getEndTokenIndex();
			// Chop off last capitalized piece
			for (; newEndTokenIndex > npSpan.getStartTokenIndex() + 1; newEndTokenIndex--)
				if (!Character.isUpperCase(document.getTokenStr(npSpan.getSentenceIndex(), newEndTokenIndex - 1).charAt(0)))
					break;
			// Chop off last uncapitalized piece
			for (; newEndTokenIndex > npSpan.getStartTokenIndex() + 1; newEndTokenIndex--)
				if (Character.isUpperCase(document.getTokenStr(npSpan.getSentenceIndex(), newEndTokenIndex - 1).charAt(0)))
					break;	
		
			npSpan = new TokenSpan(npSpan.getDocument(), npSpan.getSentenceIndex(), npSpan.getStartTokenIndex(), newEndTokenIndex);
		}
	    
	    if (npSpan.getStartTokenIndex() > 0 && document.getPoSTag(npSpan.getSentenceIndex(), npSpan.getStartTokenIndex() - 1) == PoSTag.DT)
	    	npSpan = new TokenSpan(npSpan.getDocument(), npSpan.getSentenceIndex(), npSpan.getStartTokenIndex()-1, npSpan.getEndTokenIndex());
	    
	    if (PoSTagClass.classContains(PoSTagClass.JJ, document.getPoSTag(npSpan.getSentenceIndex(), npSpan.getEndTokenIndex() - 1)))
	    	return;
	    
	    spans.add(npSpan);
	}

	private boolean spanContainsFnWord(TokenSpan tokenSpan) {
		DocumentNLP document = tokenSpan.getDocument();
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++)
			if (this.nounPhraseFnWord.contains(document.getTokenStr(tokenSpan.getSentenceIndex(), i)))
				return true;
		return false;
	}
	
	private boolean spanContainsBadTokens(TokenSpan tokenSpan) {
		String str = tokenSpan.toString();
		
		Set<String> badPrefixes = this.nounPhraseBadPrefix.getValues();
		Set<String> badSuffixes = this.nounPhraseBadSuffix.getValues();
		Set<String> badTokens = this.nounPhraseBadToken.getValues();
		
		for (String badPrefix : badPrefixes)
			if (str.startsWith(badPrefix))
				return true;
		
		for (String badSuffix : badSuffixes)
			if (str.endsWith(badSuffix))
				return true;
		
		for (String badToken : badTokens)
			if (str.contains(" " + badToken + " "))
				return true;
		
		return false;
	}
	
	private boolean isNounPhraseToken(DocumentNLP document, int sentenceIndex, int tokenIndex, int minIndex, int maxIndex, boolean checkStopWord) {
		// Changed logic here to look for common prefixes followed by
		// periods.
		return ((isTokenInNounPhrasePoSTagClass(document, sentenceIndex, tokenIndex) 
				&& (!checkStopWord || this.nounPhraseStopWord.contains(document.getTokenStr(sentenceIndex, tokenIndex))))
			||
				(tokenIndex < maxIndex && tokenIndex > minIndex && document.getTokenStr(sentenceIndex, tokenIndex).equals(".")
				&& this.prefixAbbreviation.contains(document.getTokenStr(sentenceIndex, tokenIndex - 1))
				&& Character.isUpperCase(document.getTokenStr(sentenceIndex, tokenIndex + 1).charAt(0)))
			||
				(tokenIndex < maxIndex && tokenIndex > minIndex && document.getTokenStr(sentenceIndex, tokenIndex).equals(",")
				&& this.suffixAbbreviation.contains(document.getTokenStr(sentenceIndex, tokenIndex + 1)))
			||
				(tokenIndex > minIndex && document.getTokenStr(sentenceIndex, tokenIndex).equals(".")
				&& this.suffixAbbreviation.contains(document.getTokenStr(sentenceIndex, tokenIndex - 1))));
	}
	
	private boolean isTokenInNounPhrasePoSTagClass(DocumentNLP document, int sentenceIndex, int tokenIndex) {
		PoSTag tag = document.getPoSTag(sentenceIndex, tokenIndex);
		
		return PoSTagClass.classContains(PoSTagClass.NNP, tag)
			|| PoSTagClass.classContains(PoSTagClass.NN, tag)
			|| PoSTagClass.classContains(PoSTagClass.JJ, tag)
			|| PoSTagClass.classContains(PoSTagClass.CD, tag);
	}
	
	private boolean isTokenInNonNNPNounPhrasePoSTagClass(DocumentNLP document, int sentenceIndex, int tokenIndex) {
		PoSTag tag = document.getPoSTag(sentenceIndex, tokenIndex);
		
		return PoSTagClass.classContains(PoSTagClass.NN, tag)
			|| PoSTagClass.classContains(PoSTagClass.JJ, tag)
			|| PoSTagClass.classContains(PoSTagClass.CD, tag);
	}
} 
