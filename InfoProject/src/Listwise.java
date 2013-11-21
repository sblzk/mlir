import java.util.*;
import java.lang.*;

public class Listwise {

	QRels data;
	private double alpha;
	private double delta;
	private double sigma;
	ArrayList<Double> weight;
	List<Double> unitvector;
	ClickModel click;
	ArrayList<ArrayList> previousWeights = new ArrayList<ArrayList>();
	
	public Listwise(QRels data, int featsize, ClickModel click){
		this.data=data;
		this.click=click;
		alpha=0.05;
		delta=0.0;
		sigma=0.2;
		weight=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			weight.add(0.0);
		unitvector=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			unitvector.add(0.0);
		
	}
	
	public void setParameters(double alpha, double delta, double sigma, ArrayList<Double> weight){
		this.alpha=alpha;
		this.delta=delta;
		this.weight=weight;
		this.sigma=sigma;
	}
	
	public Double sum(List<Double> list) {
	     Double sum= 0.0; 
	     for (Double i:list)
	         sum = sum + i;
	     return sum;
	}
	
	//weight will get updated after all queries are run. Please refer to Hoffman for pseudocode of steps
	public void runBaseline(){
		//step 2: outer for loop
		for(int query:data.queryMap.keySet()){
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			HashMap<Double,HashSet<Integer>> exploratoryscores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			ArrayList<Double> weight2 = new ArrayList<Double>();
			
			//step 3: sample unit vector uniformly
			Random r = new Random();
			List<Double> unitCreator = new ArrayList<Double>();
			List<Double> unitCreator2 = new ArrayList<Double>();
			for(int i=0; i<unitvector.size(); i++){
				Double n = r.nextGaussian();
				unitCreator2.set(i,n);
				n = n*n;
				unitCreator.set(i, n);
			}
			Double mag = Math.sqrt(sum(unitCreator));
			for(int i=0; i<unitvector.size(); i++){
				unitvector.set(i, delta*unitCreator2.get(i)/mag);
			}
			
			//step 3.a: extract scores
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight)))
					scores.put(computeDotProduct(data.featureMap.get(qdoc), weight),new HashSet<Integer>());
				scores.get(computeDotProduct(data.featureMap.get(qdoc), weight)).add(qdoc.getItem2());
			}
			//step 3.b: sort entire list
			ArrayList<Integer> sortedList=constructFullSortedList(scores);
			
			
			//step 4: set w'_t = w_t + delta*unitvector
			for(int i=0; i<unitvector.size(); i++){
				if(previousWeights.size()>0)
					weight2.add(weight.get(i) + sum(listTranspose(previousWeights).get(i))/(listTranspose(previousWeights).get(i).size()) + unitvector.get(i)/(listTranspose(previousWeights).get(i).size()));
				weight2.add(weight.get(i) + unitvector.get(i));
			}	//note: the previous line does not consider sigma but rather the size of the previousWeights matrix.  some discussion to be had here...
			
			//step 4.a: product of weight2 and scores
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!exploratoryscores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight2)))
					exploratoryscores.put(computeDotProduct(data.featureMap.get(qdoc), weight2),new HashSet<Integer>());
				exploratoryscores.get(computeDotProduct(data.featureMap.get(qdoc), weight2)).add(qdoc.getItem2());
			}
			
			//step 4.b: generate sorted list based on these scores
			ArrayList<Integer> exploratoryList=constructFullSortedList(exploratoryscores);
			
			//step 5: perform interleaving of lists TO DO
			ArrayList<Integer> balancedList = balancedInterleave(sortedList, exploratoryList);

			//step 6-8: assuming click model on first ten entries, get labeled triples
//			boolean[] clicks=new boolean[];
//			for(int i=0; i<10; i++){
//				boolean rel=false;
//				if(data.relevanceMap.get(new SimplePair(query,(int)sortedList.get(i)))>0)
//					rel=true;
//				clicks[i]=click.clicks(rel);
//			}
			
//			ArrayList<SimpleTriple> training=getImplicitFeedback(clicks,sortedList);
//			System.out.println(training.size());
			
			//select the correct complete list, based on the balancedList output (or a boolean list analyzer from that)
			ArrayList<Integer> nextList = new ArrayList<Integer>();
			//some means of selecting here (balancedList output, perhaps?)
			
			if(nextList == exploratoryList)
				if(previousWeights.size()>=20)
					previousWeights.remove(0);	//this should remove the oldest value	
				else	
					continue;
				previousWeights.add(weight2);
		}
	}	
	
	public ArrayList<ArrayList> listTranspose(ArrayList<ArrayList> inputList) {
		ArrayList<ArrayList> table = new ArrayList<ArrayList>();
		for(int i=0;i<inputList.size();i++){
			List<Integer> iValue = inputList.get(i);	//1, 2, 3, 4, ..., 43, 44
			for(int j=0;j<iValue.size();j++){
				table.get(j).add(iValue.get(j));		//hope this works...
		}
		}
		return table;
	    }

	//will use current weight to return a ranked list for this query
	public ArrayList<Integer> returnRankedList(int query){
		HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>();
		for(SimplePair qdoc:data.queryMap.get(query)){
			if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight)))
				scores.put(computeDotProduct(data.featureMap.get(qdoc), weight),new HashSet<Integer>());
			scores.get(computeDotProduct(data.featureMap.get(qdoc), weight)).add(qdoc.getItem2());
		}
		
		return constructFullSortedList(scores);
	}
	
	@SuppressWarnings("unchecked")															//9-11 loop
	private void updateWeightVector(ArrayList<SimpleTriple> training, int query){
		ArrayList<Double> nextweight=new ArrayList<Double>(weight);
		for(int i=0; i<unitvector.size(); i++){
			nextweight.add(weight.get(i) + (alpha/delta)*unitvector.get(i));
		}	
		weight=new ArrayList<Double>(nextweight);
	}

	private ArrayList<Integer> balancedInterleave(ArrayList<Integer> list1, ArrayList<Integer> list2){
		ArrayList<Integer> interleavedList = new ArrayList<Integer>();
		for(int i=0; i<list1.size(); i++){
			//do something with list1 and list2
		}
		return interleavedList;
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
	
	//with current weight vector
	private double computeDotProduct(ArrayList<Double> array, ArrayList<Double> weightlist){
		double result=0.0;
		
		for(int i=0; i<array.size(); i++)
			result+=(array.get(i)*weightlist.get(i));
		
		
		return result;
	}
	
	private ArrayList<Double> MultMinus(ArrayList<Double> f1, ArrayList<Double> f2, double mult){
		ArrayList<Double> result=new ArrayList<Double>(f1);
		
			for(int i=0; i<f1.size(); i++)
				if(f2!=null)
					result.set(i,(result.get(i)-f2.get(i))*mult);
				else
					result.set(i,result.get(i)*mult);
		return result;
	}
	
	
	private ArrayList<Double> sumVectors(ArrayList<Double>...vectors){
		ArrayList<Double> result=new ArrayList<Double>();
		
		for(int i=0; i<vectors[0].size(); i++){
			double sum=0.0;
			for(int j=0; j<vectors.length; j++)
				sum+=vectors[j].get(i);
			result.add(sum);
		}
		return result;
	} 
	
	private ArrayList<SimpleTriple> getImplicitFeedback(boolean[] clicks, ArrayList<Integer> docids){
		ArrayList<SimpleTriple> result=new ArrayList<SimpleTriple>();
		for(int i=9; i>0; i--){
			if(clicks[i]){
				for(int j=i-1;j>=0; j--)
					if(!clicks[j])
						result.add(new SimpleTriple(docids.get(j),docids.get(i),-1));
			}
		}
		return result;
	}
}
