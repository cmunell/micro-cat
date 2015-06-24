package edu.cmu.ml.rtw.micro.cat.data;

import java.io.File;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import edu.cmu.ml.rtw.generic.cluster.ClustererStringAffix;
import edu.cmu.ml.rtw.generic.cluster.ClustererTokenSpanString;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Stemmer;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.AnnotationTypeNLPCat;
import edu.cmu.ml.rtw.micro.cat.util.CatProperties;

/**
 * CatDataTools contains definitions of cleaning 
 * functions, gazetteers, and other tools used by
 * features in the categorization models.
 * 
 * @author Bill McDowell
 *
 */
public class CatDataTools extends DataTools {
	private CatProperties properties;
	private DocumentSet<DocumentNLP> documentSet;
	
	public CatDataTools() {
		this(new CatProperties());
		
	}
	
	public CatDataTools(CatProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public CatDataTools(OutputWriter outputWriter, CatDataTools dataTools) {
		this(outputWriter, dataTools.properties, dataTools.getDocumentSet());
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	public CatDataTools(OutputWriter outputWriter, CatProperties properties) {
		this(outputWriter, properties, null);
	}
	
	public CatDataTools(OutputWriter outputWriter, CatProperties properties, DocumentSet<DocumentNLP> documentSet) {
		super(outputWriter);
		
		this.properties = properties;
		this.documentSet = documentSet;
		this.addPath("CregCmd", new Path("CregCmd", properties.getCregCommandPath()));
	
		// For cleaning strings, and replacing all white space with "_"
		this.addCleanFn(new DataTools.StringTransform() {
			private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>();
			
			public String toString() {
				return "CatDefaultCleanFn";
			}
			
			@Override
			public String transform(String str) {
				if (!this.cache.containsKey(str)) {
					String transformed = CatDataTools.cleanString(str, false, false, false);
					this.cache.put(str, transformed);
					return transformed;
				} else {
					return this.cache.get(str);
				}
			}
		});
		
		this.addCleanFn(new DataTools.StringTransform() {
			private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>();
			
			public String toString() {
				return "CatStemCleanFn";
			}
			
			@Override
			public String transform(String str) {
				if (!this.cache.containsKey(str)) {
					String transformed = CatDataTools.cleanString(str, true, true, true);
					this.cache.put(str, transformed);
					return transformed;
				} else {
					return this.cache.get(str);
				}
			}
		});
		
		this.addCleanFn(new DataTools.StringTransform() {
			public String toString() {
				return "CatBagOfWordsFeatureCleanFn";
			}
			
			@Override
			public String transform(String str) {
				Gazetteer stopWords = getGazetteer("BagOfWordsFeatureStopWord");
				if (stopWords.contains(str))
					return "";
				return CatDataTools.cleanString(str, true, true, true);
			}
		});
		
		this.addCleanFn(new DataTools.StringTransform() {
			public String toString() {
				return "Trim";
			}
			
			public String transform(String str) {
				return str.trim();
			}
		});
		
		this.addCleanFn(new DataTools.StringTransform() {
			public String toString() {
				return "TrimToLower";
			}
			
			public String transform(String str) {
				return str.trim().toLowerCase();
			}
		});
		
		this.addStringClusterer(new ClustererStringAffix("AffixMaxLength5", 5, this.getCleanFn("CatDefaultCleanFn")));
		this.addTokenSpanClusterer(new ClustererTokenSpanString(getStringClusterer("AffixMaxLength5")));
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.FACC1);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.FAILED_FACC1);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.AMBIGUOUS_FACC1);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.NELL_CATEGORY);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.FREEBASE_TYPE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.FREEBASE_TOPIC);
		this.addAnnotationTypeNLP(AnnotationTypeNLPCat.WIKI_URL);
	}
	
	public static String cleanString(String str, boolean stem, boolean toLower, boolean removeSymbols) {
		str = str.trim();
		
		if (removeSymbols)
			str = str.replaceAll("[\\W&&[^\\s]]+", " "); // replaces all non-alpha-numeric (differs from http://qwone.com/~jason/writing/loocv.pdf)
		else {
			// Just remove ,()=
			str = str.replace(',', ' ');
			str = str.replace('(', ' ');
			str = str.replace(')', ' ');
			str = str.replace('=', ' ');
		}
				
		str = str.replaceAll("\\d+", "[D]") 
				 .replaceAll("_", " ")
				 .trim();
		
		if (toLower)
			str = str.toLowerCase();
		
		String[] parts = str.split("\\s+");
		StringBuilder retStr = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() > 30) // remove long tokens
				continue;
			
			if (stem)
				parts[i] = Stemmer.stem(parts[i]);
			retStr = retStr.append(parts[i]).append("_");
		}
		
		if (retStr.length() > 0)
			retStr.delete(retStr.length() - 1, retStr.length());
	
		if (retStr.length() == 0)
			return str;
		else
			return retStr.toString().trim();
	}
	
	/**
	 * Get path by name given in experiment configuration file.  This
	 * allows the experiments to refer to paths without being machine
	 * specific.  
	 * 
	 * Paths starting with 'CregModel' refer to serialized creg models
	 * stored in the directory specified by 'cregDataDirPath' in
	 * cat.properties.
	 */
	public synchronized Path getPath(String name) {
		if (name == null)
			return null;
		if (!name.startsWith("CregModel"))
			return super.getPath(name);
		
		String modelName = name.substring("CregModel/".length());
		String modelPath = (new File(this.properties.getCregDataDirPath(), modelName)).getAbsolutePath();
		return new Path(name, modelPath);
	}
	
	public synchronized Gazetteer getGazetteer(String name) {
		if (this.gazetteers.containsKey(name))
			return this.gazetteers.get(name);
		
		if (name.equals("FreebaseNELLCategory")) {
			this.addGazetteer(
				new Gazetteer(name, 
				this.properties.getFreebaseNELLCategoryGazetteerPath(),
				this.getCleanFn("TrimToLower"))
			);
		} else if (name.equals("FreebaseTypeTopic")) {
			this.addGazetteer(
					new Gazetteer(name, 
					this.properties.getFreebaseTypeTopicGazetteerPath(),
					this.getCleanFn("Trim"))
				);
		} else if (name.equals("PolysemousPhrase")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getPolysemousPhraseGazetteerPath(),
					this.getCleanFn("Trim"))
				);
		} else if (name.equals("NounPhraseNELLCategory")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNounPhraseNELLCategoryGazetteerPath(),
					this.getCleanFn("Trim"),
					true)
				);
		} else if (name.equals("NELLCategoryGeneralization")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLCategoryGeneralizationGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLCategoryMutex")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLCategoryMutexGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLPrefixAbbreviation")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLPrefixAbbreviationGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLSuffixAbbreviation")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLSuffixAbbreviationGazetteerPath(),
					this.getCleanFn("Trim"))
			);		
		} else if (name.equals("NELLNounPhraseBadPrefix")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhraseBadPrefixGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLNounPhraseBadSuffix")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhraseBadSuffixGazetteerPath(),
					this.getCleanFn("Trim"))
			);		
		} else if (name.equals("NELLNounPhraseBadToken")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhraseBadTokenGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLNounPhrasePhraseDictionary")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhrasePhraseDictionaryGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLNounPhraseFnWord")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhraseFnWordGazetteerPath(),
					this.getCleanFn("Trim"))
			);
		} else if (name.equals("NELLNounPhraseStopWord")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getNELLNounPhraseStopWordGazetteerPath(),
					this.getCleanFn("TrimToLower"))
			);
		} else if (name.equals("BagOfWordsFeatureStopWord")) {
			this.addGazetteer(
					new Gazetteer(name,
					this.properties.getBagOfWordsFeatureStopWordGazetteerPath(),
					this.getCleanFn("TrimToLower"))
			);
 		} else {
			return null;
		}
		
		return this.gazetteers.get(name);
	}
	
	public CatProperties getProperties() {
		return this.properties;
	}
	
	public DocumentSet<DocumentNLP> getDocumentSet() {
		return this.documentSet;
	}
	
	public boolean setDocumentSet(DocumentSet<DocumentNLP> documentSet) {
		this.documentSet = documentSet;
		return true;
	}

}
