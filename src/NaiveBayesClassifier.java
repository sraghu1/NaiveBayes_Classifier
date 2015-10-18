import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;


public class NaiveBayesClassifier {
	private static String mTestDataDirectory="";
	private static String mTrainingDataDirectory="C:/Users/raghusab/Desktop/MachineLearning/Training Dataset";
	private static boolean mInMemoryFlag=false; //By Default operations are executed by File IO rather than in memory objects.
	private HashMap<String,List<File>> mFileLists=new HashMap<String,List<File>>(); 
	private ArrayList<String> mClassNames;
	private StopWordAnalyzer mAnalyzer;
	private PorterStemmer mStemmer;
	private ArrayList<MemoryFile> mMemFiles=new ArrayList<MemoryFile>();
	
	public static void main(String args[]) throws Exception
	{
	NaiveBayesClassifier classifier=new NaiveBayesClassifier();
	if(args.length==3)
	{
	mTrainingDataDirectory=args[0];
	mTestDataDirectory=args[1];
	if(args[2].equalsIgnoreCase("true"))
	{
		mInMemoryFlag=true;
	}
	else
	{
		mInMemoryFlag=false;
	}
	
	classifier.classify();
	
	}
	else
	{
		System.out.println("Correct Usage: java NaiveBayesClassifier <TrainingDataDirectory> <TestDataDirectory> <InMemoryFlag>\nAlso Please make sure you do not have any white spaces in your Directory names");
	}
	Runtime.getRuntime().freeMemory(); //Mark the objects for GC's Mark & Sweep.
	}

	public void classify() throws Exception
	{
		mAnalyzer=new StopWordAnalyzer();
		mStemmer=new PorterStemmer();
		ArrayList<OccuranceProbabilties> op=new ArrayList<OccuranceProbabilties>();
		File[] listOfTestFiles=new File(mTestDataDirectory).listFiles();
		File[] listOfTrainingFiles=new File(mTrainingDataDirectory).listFiles();
		mClassNames=new ArrayList<String>();
		for(File f: listOfTrainingFiles)
		{
			mClassNames.add(f.getName());
			OccuranceProbabilties oc=new OccuranceProbabilties();
			oc.setClassName(f.getName());
			oc.setOccuranceMap(new HashMap<String,Double>());
			op.add(oc);
			File classTraining[]=new File(mTrainingDataDirectory+"/"+f.getName()).listFiles();
			mFileLists.put(f.getName(), (List<File>) Arrays.asList(classTraining));
		}
		
		if(mInMemoryFlag==true)
		{
			System.out.println("Loading Data in to memory.... May take a while depending upon the size of the data");
			loadIntoMemory();
		}
		
		for(File f:listOfTestFiles)
		{
			TreeMap<String,BigDecimal> probabilities=new TreeMap<String,BigDecimal>();
			System.out.println("---------------Computing for Test File:"+f.getName()+"-----------");
			for(String currentClass:mClassNames)
			{
				TestRecord testRecord=readOneTestFile(f);
				BigDecimal prob=new BigDecimal(""+1.0);
				for(String word:testRecord.getWords())
				{
					double termProbInClass=0.0;
					if(getFromExistingProbability(word,op,currentClass)!=0.0)
					{
						termProbInClass=getFromExistingProbability(word,op,currentClass);
					}
					else
					{
						if(mInMemoryFlag==true)
							termProbInClass=calculateProbabilityInMemory(word,op,currentClass);
						else
							termProbInClass=calculateProbability(word,op,currentClass);
						for(OccuranceProbabilties oc:op)
						{
							if(oc.getClassName().equalsIgnoreCase(currentClass))
							{
								oc.getOccuranceMap().put(word, termProbInClass);
								break;
							}
						}
					}
				prob=prob.multiply(new BigDecimal(""+termProbInClass));
				}
				probabilities.put(currentClass, prob);
			}
			Entry<String,BigDecimal> maxEntry = null;

			for(Entry<String,BigDecimal> entry : probabilities.entrySet()) {
			    if (maxEntry == null || entry.getValue().compareTo( maxEntry.getValue())>0) {
			        maxEntry = entry;
			    }
			}
		//System.out.println(probabilities);	
			
		System.out.println(f.getName()+"--->"+maxEntry.getKey());
		//probabilities.clear();
		}	
	}
	
	/*
	 * Load the training data in to memory
	 */
	private void loadIntoMemory() throws IOException
	{
		for(String s:mClassNames)
		{
			List<File> classTraining=mFileLists.get(s);
			MemoryFile memfile=new MemoryFile();
			ArrayList<String> words=new ArrayList<String>();
			memfile.setClassname(s);
			for(File f:classTraining)
			{
				
				String fileAsString=FileUtils.readFileToString(f);
				fileAsString=fileAsString.replaceAll("\\P{L}", " ").toLowerCase().replaceAll("\n"," ");
				fileAsString=fileAsString.replaceAll("\\s+", " ");
				String[] lines=fileAsString.split("\\s+");
				for(String eachWord:lines){
					eachWord=mAnalyzer.removeStopWords(eachWord);
					String processedWord=mStemmer.stem(eachWord);
					if(processedWord.length()>1)
					{
						words.add(processedWord);
					}
				}
				
			}
			memfile.setContent(words);
			mMemFiles.add(memfile);
		}
	}
	
	/**
	 * Get the probability value of a particular word in a particular class. Calculates them from in memory stored objects
	 * @param word
	 * @param op
	 * @param currentClass
	 * @return probability value of the word in the class
	 */
	private double calculateProbabilityInMemory(String word,ArrayList<OccuranceProbabilties> op, String currentClass)
	{
		double prob=0.0;
		int count=0;
		int occurances=0;
		for(MemoryFile memFile:mMemFiles)
		{
			if(memFile.getClassname().equals(currentClass))
			{
				occurances+=Collections.frequency(memFile.getContent(), word)*50; //Giving more weightage to occurances rather than just incrementing by 1.
				count+=memFile.getContent().size();
			}
		}
		prob=(double)((double)occurances+50.0)/(double)((double)count+100.0); // Normalizing the value to avoid return of 0.0
		return prob;
	}
	
	/**
	 * Get the probability value of a particular word in a particular class. Calculates them from File search. 
	 * @param word
	 * @param op
	 * @param currentClass
	 * @return probability value of the word in the class
	 * @throws Exception
	 */
	private double calculateProbability(String word,
			ArrayList<OccuranceProbabilties> op, String currentClass) throws Exception {
		// TODO Auto-generated method stub
		double probability=0.0;
		List<File> classTraining=mFileLists.get(currentClass);
		ArrayList<String> words=new ArrayList<String>();
		double count=0.0;
		for(File f:classTraining)
		{
			String fileAsString=FileUtils.readFileToString(f).replaceAll("\\P{L}", " ").toLowerCase().replaceAll("\n"," ");
			fileAsString=fileAsString.replaceAll("\\s+", " ");
			String[] lines=fileAsString.split("\\s+");
			for(String eachWord:lines){
				
					eachWord=mAnalyzer.removeStopWords(eachWord);
					String processedWord=mStemmer.stem(eachWord);
					if(processedWord.length()>1)
					{
						words.add(processedWord);
						if(processedWord.equalsIgnoreCase(word))
						{
							count+=20;
						}
					}
			}
		}
		probability=(double)((double)count+50.0)/(double)((double)words.size()+100.0);
		return probability;
	}
	
	/**
	 * Get from cached probabilities.
	 * @param word
	 * @param probabilties
	 * @param className
	 * @return the cached probabilities
	 */
	public double getFromExistingProbability(String word, ArrayList<OccuranceProbabilties>probabilties, String className)
	{
		double value=0.0;
		for(OccuranceProbabilties op:probabilties)
		{
			if(op.getClassName().equals(className))
			{
				Set<String> myKeys=op.getOccuranceMap().keySet();
				for(String s:myKeys)
				{
					if(op.getOccuranceMap().get(s) != null&&s.equals(word))
					{
						value=op.getOccuranceMap().get(s);
					}
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Reads one test file and stores it in Object.
	 * @param f
	 * @return
	 */
	public TestRecord readOneTestFile(File f)
	{
		TestRecord record=new TestRecord();
		String currentLine;
		ArrayList<String> words=new ArrayList<String>();
		try{
		BufferedReader br=new BufferedReader(new FileReader(f));
		while((currentLine=br.readLine())!=null)
		{
			currentLine=currentLine.toLowerCase(); //convert everyword to lower case
			currentLine=currentLine.replaceAll("\\P{L}", " "); //only alphabets
			currentLine=currentLine.replaceAll("\n"," ");
			currentLine=currentLine.replaceAll("\\s+", " ").trim();
			String lineWords[]=currentLine.split("\\s+");
			for(String eachWord:lineWords)
			{
				eachWord=mAnalyzer.removeStopWords(eachWord);
				String processedWord=mStemmer.stem(eachWord);
				if(processedWord.length()>1)
				{
				words.add(processedWord);
				}
				
			}
		}
		record.setRecordId(Integer.parseInt(f.getName().replaceAll("\\D+","")));
		record.setWords(words);
		br.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return record;
	}
}
