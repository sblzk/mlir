
import java.util.*;

public class Test {

	
	QRels data;
	ArrayList<Double> weight;
	
	public Test(QRels q, ArrayList<Double> weight){
		data=q;
		this.weight=weight;
	}
	
	//will use current weight to return a ranked list for this query
		public ArrayList<Integer> returnRankedList(int query){
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>();
			
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc))))
					scores.put(computeDotProduct(data.featureMap.get(qdoc)),new HashSet<Integer>());
				scores.get(computeDotProduct(data.featureMap.get(qdoc))).add(qdoc.getItem2());
			}
			
			return constructFullSortedList(scores);
		}
		
		//with current weight vector
		private double computeDotProduct(ArrayList<Double> array){
			double result=0.0;
			
			for(int i=0; i<array.size(); i++)
				result+=(array.get(i)*weight.get(i));
			
			
			return result;
		}
		
		private ArrayList<Integer> constructFullSortedList(HashMap<Double,HashSet<Integer>> scores){
			ArrayList<Integer> result=new ArrayList<Integer>();
			ArrayList<Double> s=new ArrayList<Double>(scores.keySet());
			Collections.sort(s);
			for(int i=s.size()-1;i>=0; i--){
				double score=s.get(i);
				ArrayList<Integer> vals=new ArrayList<Integer>(scores.get(score));
				Collections.sort(vals);
				for(int j=0; j<vals.size(); j++)
					result.add(vals.get(j));
			}
			return result;
		}
}
