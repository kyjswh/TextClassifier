package TextClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class NaiveBayesClassifier {
	private ArrayList<String> classes;
	private HashMap<String, String[]> classKeyWords; //keywords asssociated with each class
	private HashMap<String, Double[]> priorProbabilities; //keyword prior probabilities associated with each class
	private HashMap<String, Double> classProbabilities; //class probability for each document
	
	//Constructor
	public NaiveBayesClassifier(){
		classes = new ArrayList<String>();
		classKeyWords = new HashMap<String,String[]>();
		classProbabilities = new HashMap<String,Double>();
		priorProbabilities = new HashMap<String, Double[]>();
	}
	
	
	//get all the class labels, input is the file address string of the class label text file
	//the text file should have the following format:
	//NUM_CLASSES
	//CLASS1
	//CLASS2
	//CLASS3
	//.....
	public void getClasses(String classFileDir) throws FileNotFoundException,IOException{
		BufferedReader br = new BufferedReader(new FileReader(classFileDir));
		
		int numClasses = Integer.parseInt(br.readLine());
		
		
		
		for(int i = 0; i < numClasses; i++ ){
			classes.add(br.readLine());
		}
		
		br.close();
	}
	
	
	//Get all the keywords associated with each class, input is the file address of class keywords text file
	//The text file should be formatted as:
	//class1Label,keyword1,keyword2,....
	//class2Label,keyword1,keyword2,....
	//.....
	public void getClassKeyWords(String keyWordsFileDir) throws FileNotFoundException,IOException{
		BufferedReader br = new BufferedReader(new FileReader(keyWordsFileDir));
		
		
		
		String line;
		String[] addedEntry;
		while((line=br.readLine())!=null){
			String[] wordsList = line.split("\t");
			addedEntry = new String[wordsList.length-1];
			
			for(int i = 0; i < addedEntry.length; i++){
				addedEntry[i] = wordsList[i+1];
			}
			
			classKeyWords.put(wordsList[0],addedEntry);	
		}
		
		br.close();
		
	}
	
	
	//Calculate all the prior probabilities p(d|c) and p(c), input is a directory address containing the
	//document indexes produced by lucene
	public void getPriorProbabilities(String indexDir) throws Exception{
		Directory dir = FSDirectory.open(new File(indexDir),null);
		IndexSearcher is = new IndexSearcher(dir);
		IndexReader reader = is.getIndexReader();
		int NumDocs = is.maxDoc();
		
		//Occurrence of each keywords in each class
		HashMap<String, int[]> wordClassFrequency = new HashMap<String,int[]>();
		HashMap<String, Integer> distinctWords = new HashMap<String,Integer>();
		HashMap<String, Integer> numWordsContinent = new HashMap<String, Integer>();
		
		//Occurrence of each 
		
		//Total number of distinct words in the whole corpus, accumulate 1 if it needs
		
		
		for(int i = 0; i < NumDocs; i++){
			
			TermFreqVector tf = reader.getTermFreqVector(i, "Capital_Text");
			String continent = reader.document(i).get("Continent");
			
			if(classProbabilities.containsKey(continent)){
				classProbabilities.put(continent, classProbabilities.get(continent)+1.0/NumDocs);
			}
			else{
				classProbabilities.put(continent,1.0/NumDocs);
			}
			
			
			
			String[] words = tf.getTerms();
			
			//If any word contains a digit, remove it
			Pattern p = Pattern.compile(".*\\d.*");
			Matcher m;
			int[] addedArray;
			for(int j = 0; j < words.length;j++){
				m = p.matcher(words[j]);
				if(!m.matches()){
					if(!wordClassFrequency.containsKey(words[j])){
						addedArray = new int[classes.size()];
						addedArray[classes.indexOf(continent)]  = 1;
						wordClassFrequency.put(words[j], addedArray);
					}
					else{
						addedArray = wordClassFrequency.get(words[j]);
						addedArray[classes.indexOf(continent)]++;
						wordClassFrequency.put(words[j], addedArray);
					}
					if(!distinctWords.containsKey(words[j])){
						distinctWords.put(words[j], 0);
					}
					if(!numWordsContinent.containsKey(continent)){
						numWordsContinent.put(continent, 1);
					}
					else{
						numWordsContinent.put(continent,numWordsContinent.get(continent)+1);
					}
				}
				
			}
		
		}
		
		int vocabulary = distinctWords.size();
		System.out.println(vocabulary);
		
	
		
	
		
		//populate prior-probabilities dictionary
		String continent;
		String[] keyWords;
		Double prob;
		
		for(int i = 0; i < classes.size(); i++){
			continent = classes.get(i);
			keyWords = classKeyWords.get(continent);
			
			for(int j = 0; j < keyWords.length;j++){
				
				String keyWord = keyWords[j];
				for(int k = 0; k < classes.size();k++){
					String c = classes.get(k);
					prob = (wordClassFrequency.get(keyWord)[k] + 1.0)/(numWordsContinent.get(c)+vocabulary);
					
					
					if(!priorProbabilities.containsKey(keyWord)){
							Double[] addedArr = new Double[classes.size()];
							//System.out.println(Arrays.toString(addedArr));
							addedArr[classes.indexOf(c)] = prob;
							priorProbabilities.put(keyWord, addedArr);
							//System.out.println(Arrays.toString(addedArr));
					}
					else{
							Double[] addedArr = priorProbabilities.get(keyWord);
							addedArr[classes.indexOf(c)] = prob;
							priorProbabilities.put(keyWord, addedArr);
							//System.out.println(Arrays.toString(addedArr));
					}
				}
			}
				
		} 
		
		is.close();
}
	
	//Given a document term vector classify it to a class
	private String classify(TermFreqVector t){
		String[] words = t.getTerms();
		ArrayList<Double> probs = new ArrayList<Double>();
		
		Double curProb;
		
		for(int i = 0; i < classes.size(); i++){
			curProb = classProbabilities.get(classes.get(i));
			for(int j = 0; j < words.length;j++){
				if(priorProbabilities.containsKey(words[j])){
					
					curProb = curProb*(priorProbabilities.get(words[j]))[i];
				}
			}
			probs.add(curProb);
		}
		
		
		Double max = Collections.max(probs);
		int maxIndex = probs.indexOf(max);
		
		System.out.println(Arrays.toString(probs.toArray()));
	
		return classes.get(maxIndex);
		
	}
	
	//Classify documents, return a confusion matrix. input is a directory address containing
	//document indexes produced by lucene
	public int[][] Classify(String Dir) throws IOException{
		Directory dir = FSDirectory.open(new File(Dir),null);
		IndexSearcher is = new IndexSearcher(dir);
		IndexReader reader = is.getIndexReader();
		int NumDocs = is.maxDoc();
		
		int[][] confusion = new int[classes.size()][classes.size()];
		for(int i = 0 ; i < classes.size(); i++){
			for(int j = 0; j < classes.size(); j++){
				confusion[i][j] = 0;
			}
		}
		
		for(int i = 0; i < NumDocs;i++){
			TermFreqVector tf = reader.getTermFreqVector(i, "Capital_Text");
			String actual = reader.document(i).get("Continent");
			String predicted = classify(tf);
			confusion[classes.indexOf(predicted)][classes.indexOf(actual)]++;
		}
		
		is.close();
		
		return confusion;
	}
	
	
	//Print all the categories
	public void printCategories(){
		System.out.println(Arrays.toString(classes.toArray()));
	}
	
	//Print all the keywords for each category
	public void printCategoryKeyWords(){
		for(String c:(String[]) classes.toArray()){
			System.out.println(c+": "+ classKeyWords.get(c));
		}
	}
	

}