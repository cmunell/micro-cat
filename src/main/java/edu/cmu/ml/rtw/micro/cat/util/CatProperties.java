package edu.cmu.ml.rtw.micro.cat.util;

import edu.cmu.ml.rtw.generic.util.Properties;

public class CatProperties extends Properties {
	private String googleApiKey;
	private String clueWeb09Facc1DirPath;
	private String clueWeb09Facc1PlusTypesDirPath;
	private String contextInputDirPath;
	private String experimentOutputDirPath;
	private String cregDataDirPath;
	private String cregCommandPath;
	private String freebaseNELLCategoryGazetteerPath;
	private String freebaseTypeTopicGazetteerPath;
	private String clueWeb09FilterPattern;
	private String polysemousPhraseGazetteerPath;
	private String hazyFacc1DataDirPath;
	private String hazyFacc1SentenceDataDirPath;
	private String polysemyDataFileDirPath;
	private String nounPhraseNELLCategoryGazetteerPath;
	private String NELLCategoryGeneralizationGazetteerPath;
	private String NELLCategoryMutexGazetteerPath;
	private String NELLPrefixAbbreviationGazetteerPath;
	private String NELLSuffixAbbreviationGazetteerPath;
	private String NELLNounPhraseBadPrefixGazetteerPath;
	private String NELLNounPhraseBadSuffixGazetteerPath;
	private String NELLNounPhraseBadTokenGazetteerPath;
	private String NELLNounPhrasePhraseDictionaryGazetteerPath;
	private String NELLNounPhraseFnWordGazetteerPath;
	private String NELLNounPhraseStopWordGazetteerPath;
	private String bagOfWordsFeatureStopWordGazetteerPath;
	private String NELLDataFileDirPath;
	
	public CatProperties() {
		this(null);
	}
	
	public CatProperties(String path) {
		super( new String[] { (path == null) ? "cat.properties" : path } );
		
		this.googleApiKey = loadProperty("googleApiKey");
		this.clueWeb09Facc1DirPath = loadProperty("clueWeb09Facc1DirPath");
		this.clueWeb09Facc1PlusTypesDirPath = loadProperty("clueWeb09Facc1PlusTypesDirPath");
		this.contextInputDirPath = loadProperty("contextInputDirPath");
		this.experimentOutputDirPath = loadProperty("experimentOutputDirPath");
		this.cregDataDirPath = loadProperty("cregDataDirPath");
		this.cregCommandPath = loadProperty("cregCommandPath");
		this.freebaseNELLCategoryGazetteerPath = loadProperty("freebaseNELLCategoryGazetteerPath");
		this.freebaseTypeTopicGazetteerPath = loadProperty("freebaseTypeTopicGazetteerPath");
		this.clueWeb09FilterPattern = loadProperty("clueWeb09FilterPattern");
		this.polysemousPhraseGazetteerPath = loadProperty("polysemousPhraseGazetteerPath");
		this.hazyFacc1DataDirPath = loadProperty("hazyFacc1DataDirPath");
		this.hazyFacc1SentenceDataDirPath = loadProperty("hazyFacc1SentenceDataDirPath");
		this.polysemyDataFileDirPath = loadProperty("polysemyDataFileDirPath");
		this.nounPhraseNELLCategoryGazetteerPath = loadProperty("nounPhraseNELLCategoryGazetteerPath");
		this.NELLCategoryGeneralizationGazetteerPath = loadProperty("NELLCategoryGeneralizationGazetteerPath");
		this.NELLCategoryMutexGazetteerPath = loadProperty("NELLCategoryMutexGazetteerPath");
		this.NELLPrefixAbbreviationGazetteerPath = loadProperty("NELLPrefixAbbreviationGazetteerPath");
		this.NELLSuffixAbbreviationGazetteerPath = loadProperty("NELLSuffixAbbreviationGazetteerPath");
		this.NELLNounPhraseBadPrefixGazetteerPath = loadProperty("NELLNounPhraseBadPrefixGazetteerPath");
		this.NELLNounPhraseBadSuffixGazetteerPath = loadProperty("NELLNounPhraseBadSuffixGazetteerPath");
		this.NELLNounPhraseBadTokenGazetteerPath = loadProperty("NELLNounPhraseBadTokenGazetteerPath");
		this.NELLNounPhrasePhraseDictionaryGazetteerPath = loadProperty("NELLNounPhrasePhraseDictionaryGazetteerPath");
		this.NELLNounPhraseFnWordGazetteerPath = loadProperty("NELLNounPhraseFnWordGazetteerPath");
		this.NELLNounPhraseStopWordGazetteerPath = loadProperty("NELLNounPhraseStopWordGazetteerPath");
		this.bagOfWordsFeatureStopWordGazetteerPath = loadProperty("bagOfWordsFeatureStopWordGazetteerPath");
		this.NELLDataFileDirPath = loadProperty("NELLDataFileDirPath");
	}
	
	public String getGoogleApiKey() {
		return this.googleApiKey;
	}
	
	public String getClueWeb09Facc1DirPath() {
		return this.clueWeb09Facc1DirPath;
	}
	
	public String getClueWeb09Facc1PlusTypesDirPath() {
		return this.clueWeb09Facc1PlusTypesDirPath;
	}
	
	public String getContextInputDirPath() {
		return this.contextInputDirPath;
	}
	
	public String getExperimentOutputDirPath() {
		return this.experimentOutputDirPath;
	}
	
	public String getCregDataDirPath() {
		return this.cregDataDirPath;
	}
	
	public String getCregCommandPath() {
		return this.cregCommandPath;
	}
	
	public String getFreebaseNELLCategoryGazetteerPath() {
		return this.freebaseNELLCategoryGazetteerPath;
	}
	
	public String getFreebaseTypeTopicGazetteerPath() {
		return this.freebaseTypeTopicGazetteerPath;
	}
	
	public String getClueWeb09FilterPattern() {
		return this.clueWeb09FilterPattern;
	}
	
	public String getPolysemousPhraseGazetteerPath() {
		return this.polysemousPhraseGazetteerPath;
	}
	
	public String getHazyFacc1DataDirPath() {
		return this.hazyFacc1DataDirPath;
	}
	
	public String getHazyFacc1SentenceDataDirPath() {
		return this.hazyFacc1SentenceDataDirPath;
	}
	
	public String getPolysemyDataFileDirPath() {
		return this.polysemyDataFileDirPath;
	}
	
	public String getNounPhraseNELLCategoryGazetteerPath() {
		return this.nounPhraseNELLCategoryGazetteerPath;
	}
	
	public String getNELLCategoryGeneralizationGazetteerPath() {
		return this.NELLCategoryGeneralizationGazetteerPath;
	}
	
	public String getNELLCategoryMutexGazetteerPath() {
		return this.NELLCategoryMutexGazetteerPath;
	}
	
	public String getNELLPrefixAbbreviationGazetteerPath() {
		return this.NELLPrefixAbbreviationGazetteerPath;
	}
	
	public String getNELLSuffixAbbreviationGazetteerPath() {
		return this.NELLSuffixAbbreviationGazetteerPath;
	}
	
	public String getNELLNounPhraseBadPrefixGazetteerPath() {
		return this.NELLNounPhraseBadPrefixGazetteerPath;
	}
	
	public String getNELLNounPhraseBadSuffixGazetteerPath() {
		return this.NELLNounPhraseBadSuffixGazetteerPath;
	}
	
	public String getNELLNounPhraseBadTokenGazetteerPath() {
		return this.NELLNounPhraseBadTokenGazetteerPath;
	}
	
	public String getNELLNounPhrasePhraseDictionaryGazetteerPath() {
		return this.NELLNounPhrasePhraseDictionaryGazetteerPath;
	}
	
	public String getNELLNounPhraseFnWordGazetteerPath() {
		return this.NELLNounPhraseFnWordGazetteerPath;
	}
	
	public String getNELLNounPhraseStopWordGazetteerPath() {
		return this.NELLNounPhraseStopWordGazetteerPath;
	}
	
	public String getBagOfWordsFeatureStopWordGazetteerPath() {
		return this.bagOfWordsFeatureStopWordGazetteerPath;
	}
	
	public String getNELLDataFileDirPath() {
		return this.NELLDataFileDirPath;
	}
}
