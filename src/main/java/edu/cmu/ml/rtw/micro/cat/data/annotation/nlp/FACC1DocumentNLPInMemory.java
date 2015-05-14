package edu.cmu.ml.rtw.micro.cat.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Language;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;

public class FACC1DocumentNLPInMemory extends DocumentNLPInMemory {
	public FACC1DocumentNLPInMemory(CatDataTools dataTools) {
		super(dataTools);
	}

	public FACC1DocumentNLPInMemory(CatDataTools dataTools, JSONObject json) {
		super(dataTools, json);
	}
	
	public FACC1DocumentNLPInMemory(CatDataTools dataTools, DocumentAnnotation documentAnnotation) {
		super(dataTools, documentAnnotation);
	}
	
	public FACC1DocumentNLPInMemory(CatDataTools dataTools, String jsonPath) {
		super(dataTools, jsonPath);
	}
	
	public FACC1DocumentNLPInMemory(CatDataTools dataTools,
									String name, 
									 Language language, 
									 String[][] tokens, 
									 PoSTag[][] posTags, 
									 String[] dependencyParseStrs,
									 String[][] nerEntities,
									 int[][] startBytes,
									 int[][] endBytes,
									 List<FACC1Annotation> facc1Annotations) {
		super(dataTools);
		
		this.name = name;
		this.language = language;
	
		
		this.tokens = new Token[tokens.length][];
		int charIndex = 0;
		for (int i = 0; i < tokens.length; i++) {
			this.tokens[i] = new Token[tokens[i].length];
			for (int j = 0; j < this.tokens[i].length; j++) {
				this.tokens[i][j] = new Token(this, tokens[i][j], charIndex, charIndex + tokens[i][j].length());
				charIndex += tokens[i][j].length() + 1;
			}
		}
		
		this.posTags = posTags;
		
		this.dependencyParses = new DependencyParse[dependencyParseStrs.length];
		for (int i = 0; i < dependencyParseStrs.length; i++) {
			this.dependencyParses[i] = DependencyParse.fromString(dependencyParseStrs[i], this, i);
		}
		this.constituencyParses = new ConstituencyParse[0];
		
		Collections.sort(facc1Annotations, FACC1Annotation.facc1StartByteComparator);
		List<TokenSpan> possibleFacc1TokenSpans = new ArrayList<TokenSpan>();
		int currentFacc1 = 0;
		boolean singleFacc1Alignment = true;
		
		this.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
		for (int i = 0; i < tokens.length; i++) {
			for (int j = 0; j < tokens[i].length; j++) {
				if (nerEntities[i][j] != null) {
					int endTokenIndex = j + 1;
					for (int k = j + 1; k < nerEntities[i].length; k++) {
						if (nerEntities[i][k] == null || !nerEntities[i][k].equals(nerEntities[i][j])) {
							endTokenIndex = k;
							break;
						}
						nerEntities[i][k] = null;
					}
					
					if (!this.ner.containsKey(i))
						this.ner.put(i, new ArrayList<Triple<TokenSpan, String, Double>>());
					this.ner.get(i).add(new Triple<TokenSpan, String, Double>(new TokenSpan(this, i, j, endTokenIndex), nerEntities[i][j], null));
				}
				
				for (int f = 0; f < facc1Annotations.size(); f++) {
					TokenSpan matchingFacc1Span = getMatchingFacc1SpanAt(facc1Annotations.get(f), i, j);
					if (matchingFacc1Span != null) {
						possibleFacc1TokenSpans.add(matchingFacc1Span);
						if (f != currentFacc1)
							singleFacc1Alignment = false;
						currentFacc1++;
						break;
					}
				}
			}
		}

		this.otherDocumentAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>>();
		this.otherTokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>>();		
		this.otherDocumentAnnotations.put(AnnotationTypeNLPCat.FAILED_FACC1, new Pair<Boolean, Double>(false, null));
		this.otherDocumentAnnotations.put(AnnotationTypeNLPCat.AMBIGUOUS_FACC1, new Pair<Boolean, Double>(false, null));
		this.otherTokenSpanAnnotations.put(AnnotationTypeNLPCat.FACC1, new HashMap<Integer, List<Triple<TokenSpan, ?, Double>>>());
		
		if (possibleFacc1TokenSpans.size() < facc1Annotations.size()) {
			this.otherDocumentAnnotations.put(AnnotationTypeNLPCat.FAILED_FACC1, new Pair<Boolean, Double>(true, null));
		} else if (singleFacc1Alignment) {
			Map<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceFacc1 = this.otherTokenSpanAnnotations.get(AnnotationTypeNLPCat.FACC1);
			
			for (int i = 0; i < facc1Annotations.size(); i++) {
				TokenSpan facc1Span = possibleFacc1TokenSpans.get(i);
				if (!sentenceFacc1.containsKey(facc1Span.getSentenceIndex()))
					sentenceFacc1.put(facc1Span.getSentenceIndex(), new ArrayList<Triple<TokenSpan, ?, Double>>());
				sentenceFacc1.get(facc1Span.getSentenceIndex()).add(new Triple<TokenSpan, FACC1Annotation, Double>(possibleFacc1TokenSpans.get(i), facc1Annotations.get(i), null));
			}
		} else {
			List<Pair<TokenSpan, FACC1Annotation>> alignment = getAlignment(facc1Annotations, possibleFacc1TokenSpans);
			if (alignment == null)
				this.otherDocumentAnnotations.put(AnnotationTypeNLPCat.FAILED_FACC1, new Pair<Boolean, Double>(true, null));
			else {
				Map<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceFacc1 = this.otherTokenSpanAnnotations.get(AnnotationTypeNLPCat.FACC1);
				for (Pair<TokenSpan, FACC1Annotation> annotation : alignment) {
					if (!sentenceFacc1.containsKey(annotation.getFirst().getSentenceIndex()))
						sentenceFacc1.put(annotation.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, ?, Double>>());
					sentenceFacc1.get(annotation.getFirst().getSentenceIndex()).add(new Triple<TokenSpan, FACC1Annotation, Double>(annotation.getFirst(), annotation.getSecond(), null));
				}
			}
		}
	}
	
	private List<Pair<TokenSpan, FACC1Annotation>> getAlignment(List<FACC1Annotation> facc1Annotations, List<TokenSpan> spans) {
		List<Pair<TokenSpan, FACC1Annotation>> alignment = new ArrayList<Pair<TokenSpan, FACC1Annotation>>(facc1Annotations.size());
		Map<Integer, Map<Integer, Integer>> facc1SpanMaxSuffix = new HashMap<Integer, Map<Integer, Integer>>();
		
		for (int i = 0; i < facc1Annotations.size(); i++)
			alignment.add(new Pair<TokenSpan, FACC1Annotation>(null, facc1Annotations.get(i)));
		
		int alignmentLength = getAlignmentHelper(spans, facc1Annotations, facc1Annotations.size(), spans.size(), facc1SpanMaxSuffix, alignment);
		if (alignmentLength < facc1Annotations.size()+1)
			return null;
		else
			return alignment;
	}
	
	private int getAlignmentHelper(List<TokenSpan> spans, List<FACC1Annotation> facc1Annotations, int facc1Index, int spansIndex, Map<Integer, Map<Integer, Integer>> facc1SpanMaxSuffix, List<Pair<TokenSpan, FACC1Annotation>> alignment) {
		if (facc1SpanMaxSuffix.containsKey(facc1Index) && facc1SpanMaxSuffix.get(facc1Index).containsKey(spansIndex))
			return facc1SpanMaxSuffix.get(facc1Index).get(spansIndex);
		
		int alignmentLength = 0;
		
		if (facc1Index >= 0 
				&& spansIndex >= 0 
				&& (facc1Index >= facc1Annotations.size() 
					|| spansIndex >= spans.size() 
					|| facc1MatchesSpan(facc1Annotations.get(facc1Index), spans.get(spansIndex)))) {
			int maxLength = 0;
			boolean ambiguousMax = false;
			for (int i = -1; i < spansIndex; i++) {
				int length = getAlignmentHelper(spans, facc1Annotations, facc1Index - 1, i, facc1SpanMaxSuffix, alignment);
				if (length > maxLength) {
					alignment.get(facc1Index-1).setFirst(spans.get(i));
					maxLength = length;
					ambiguousMax = false;
				} else if (length == maxLength && length > 0) {
					ambiguousMax = true;
				}
			}
			
			alignmentLength = 1 + maxLength;
			if (ambiguousMax)
				this.otherDocumentAnnotations.put(AnnotationTypeNLPCat.AMBIGUOUS_FACC1, new Pair<Boolean, Double>(true, null));
		}
		
		if (!facc1SpanMaxSuffix.containsKey(facc1Index))
			facc1SpanMaxSuffix.put(facc1Index, new HashMap<Integer, Integer>());
		facc1SpanMaxSuffix.get(facc1Index).put(spansIndex, alignmentLength);
			
		return alignmentLength;
	}
	
	private boolean facc1MatchesSpan(FACC1Annotation facc1, TokenSpan tokenSpan) {
		String[] facc1Parts = facc1.getPhrase().split("[\\s+]");
		StringBuilder facc1Glued = new StringBuilder();
		for (int i = 0; i < facc1Parts.length; i++)
			facc1Glued.append(facc1Parts[i]);
		String facc1GluedStr = facc1Glued.toString();
		
		StringBuilder tokensGlued = new StringBuilder();
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++) {
			tokensGlued.append(this.tokens[tokenSpan.getSentenceIndex()][i]);
		}
		String tokensGluedStr = tokensGlued.toString();
		return tokensGluedStr.equals(facc1GluedStr);
	}
	
	private TokenSpan getMatchingFacc1SpanAt(FACC1Annotation facc1, int sentenceIndex, int startTokenIndex) {
		String[] facc1Parts = facc1.getPhrase().split("[\\s+]");
		StringBuilder facc1Glued = new StringBuilder();
		for (int i = 0; i < facc1Parts.length; i++)
			facc1Glued.append(facc1Parts[i]);
		char[] facc1GluedStr = facc1Glued.toString().toCharArray();
		int facc1CharIndex = 0;
		for (int tokenIndex = startTokenIndex; tokenIndex < this.tokens[sentenceIndex].length; tokenIndex++) {
			char[] tokenChars = this.tokens[sentenceIndex][tokenIndex].getStr().toCharArray();
			for (int i = 0; i < tokenChars.length; i++) {
				if (facc1CharIndex >= facc1GluedStr.length || tokenChars[i] != facc1GluedStr[facc1CharIndex])
					return null;
				facc1CharIndex++;
			}
			
			if (facc1CharIndex >= facc1GluedStr.length) {
				return new TokenSpan(this, sentenceIndex, startTokenIndex, tokenIndex + 1);
			}
		}
		
		return null; // reached end of sentence before end of facc1 phrase
	}
	
	public boolean isAmbiguousFacc1Alignment() {
		return (Boolean)this.otherDocumentAnnotations.get(AnnotationTypeNLPCat.AMBIGUOUS_FACC1).getFirst();
	}
	
	public boolean isFailedFacc1Alignment() {
		return (Boolean)this.otherDocumentAnnotations.get(AnnotationTypeNLPCat.FAILED_FACC1).getFirst();
	}
}
