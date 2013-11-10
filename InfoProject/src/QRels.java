import java.io.*;
import java.util.*;

//Written explicitly for LETOR 3.0. Parses file into appropriate data structures
public class QRels {
	
	HashMap<SimplePair, ArrayList<Double>> featureMap; //map query and doc id pairs to feature vectors
	HashMap<SimplePair, Integer> relevanceMap;	//map query and doc id pairs to relevance
	HashMap<Integer, HashSet<SimplePair>> queryMap; //maps queries to simple pairs
	HashMap<Integer, HashSet<SimplePair>> docMap;  //maps docs to simple pairs

	
	public QRels(String inputFile)throws IOException{
		
		//initialize data structures
		relevanceMap=new HashMap<SimplePair, Integer>();
		featureMap=new HashMap<SimplePair, ArrayList<Double>>();
		queryMap=new HashMap<Integer,HashSet<SimplePair>>();
		docMap=new HashMap<Integer,HashSet<SimplePair>>();
		
		Scanner in=new Scanner(new FileReader(inputFile));//open file
		
		while(in.hasNextLine()){
			String line=in.nextLine();
			
			//extract components from the line
			String[] components=line.split(" ");
			int relevance=Integer.parseInt(components[0]);
			int qid=Integer.parseInt(components[1].split(":")[1]);
			int docid=Integer.parseInt(components[components.length-1]);
			ArrayList<Double> fv=new ArrayList<Double>();
			for(int i=2; i<=45; i++)
				fv.add(Double.parseDouble(components[i].split(":")[1]));
			

			//add to data structures
			SimplePair pair=new SimplePair(qid,docid);
			relevanceMap.put(pair,relevance);
			featureMap.put(pair,fv);
			if(!queryMap.containsKey(qid))
				queryMap.put(qid,new HashSet<SimplePair>());
			queryMap.get(qid).add(pair);
			if(!docMap.containsKey(docid))
				docMap.put(docid,new HashSet<SimplePair>());
			docMap.get(docid).add(pair);
		}
		
		in.close();//close file
	}
	
	
	public static void main(String[] args)throws IOException{
		new QRels("/host/TREC/TD2003/All/TD2003.txt");
	}
}
