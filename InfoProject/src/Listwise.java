import java.util.*;
import java.lang.*;
import java.util.regex.Pattern;

public class Listwise {

	QRels data;
	private double alpha;																		//edit
	private double delta;
	private double kappa;
	ArrayList<Double> weight;
	ArrayList<ArrayList<Double>> previousWeights;
	List<Double> unitvector;
	ClickModel click;
	
	public Listwise(QRels data, int featsize, ClickModel click){
		this.data=data;
		this.click=click;
		alpha=0.01;
		delta=1.0;
		kappa=0.5;
		weight=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			weight.add(0.0);
		unitvector=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			unitvector.add(0.0);
		
	}
	
	public void setParameters(double alpha, double delta, ArrayList<Double> weight){
		this.alpha = alpha;
		this.delta=delta;
		this.weight=weight;
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
			unitvector = getUnifRandUnitVector(unitvector);
			
			//step 4: set w'_t = w_t + delta*unitvector
			
			for(int i=0; i<unitvector.size(); i++){
				weight2.add(weight.get(i) + delta*unitvector.get(i));
			}
			Double weight2norm = Math.sqrt(sum(weight2));
			for(int i=0; i<unitvector.size(); i++){
				weight2.set(i, weight2.get(i)/weight2norm);
			}
			
			//step 4.a1: extract scores for weight
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight)))
					scores.put(computeDotProduct(data.featureMap.get(qdoc), weight),new HashSet<Integer>());
				scores.get(computeDotProduct(data.featureMap.get(qdoc), weight)).add(qdoc.getItem2());
			}
			//step 4.a2: generate exploitative sorted list
			ArrayList<Integer> sortedList=constructFullSortedList(scores);

			
			//step 4.b1: extract scores for weight2
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!exploratoryscores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight2)))
					exploratoryscores.put(computeDotProduct(data.featureMap.get(qdoc), weight2),new HashSet<Integer>());
				exploratoryscores.get(computeDotProduct(data.featureMap.get(qdoc), weight2)).add(qdoc.getItem2());
			}
			
			//step 4.b2: generate exploratory sorted list based on these scores
			ArrayList<Integer> exploratoryList=constructFullSortedList(exploratoryscores);
			
			//step 5: interleave (balanced and probabilistic)
			LinkedHashMap<String, Integer> balancedList = balancedInterleave(sortedList, exploratoryList, kappa);
			List<Integer> balancedListValues = new ArrayList<Integer>(balancedList.values()); 
			List<String> balancedListKeys = new ArrayList<String>(balancedList.keySet());
				
			LinkedHashMap<String, Integer> probabilisticList = probabilisticInterleave(sortedList, exploratoryList, kappa);
			List<Integer> probabilisticListValues = new ArrayList<Integer>(probabilisticList.values()); 
			List<String> probabilisticListKeys = new ArrayList<String>(probabilisticList.keySet());
			
			//step 6-8: assuming click model on first 20 entries, get labeled triples
			//
			//
			
			boolean[] clicks = getClicks(query, balancedListValues); //interchangeable with probabilisticListValues
			
			ArrayList<Integer> listsClicks = computeListClicks(clicks, balancedListKeys); //interchangeable with probabilisticListKeys
			
			if(listsClicks.get(1) < listsClicks.get(0))
				continue;
			else if(listsClicks.get(1) > listsClicks.get(0))
				updateWeightVector(query);
		
		}
		//step 12 not explicit: the final weight vector should be in 'weight'
	}
	
	public void updatePreviousWeights(List<Double> weight2, ArrayList<Integer> listsClicks, ArrayList<ArrayList<Double>> previousWeights){
		if(listsClicks.get(1) > listsClicks.get(0))
			if(previousWeights.size()>=20)
				previousWeights.remove(0);	//this should remove the oldest value		
			previousWeights.add((ArrayList)weight2);
	}
	
	public List<Double> movingAverage(ArrayList<Double> weight, List<Double> unitvector, ArrayList<ArrayList<Double>> previousWeights){
		List<Double> weight2 = new ArrayList<Double>();
		//step 4: set w'_t = w_t + delta*unitvector
		for(int i=0; i<unitvector.size(); i++){
			if(previousWeights.size()>0)
				weight2.add(weight.get(i) + sum(listTranspose(previousWeights).get(i))/(listTranspose(previousWeights).get(i).size()) + unitvector.get(i)/(listTranspose(previousWeights).get(i).size()));
			weight2.add(weight.get(i) + unitvector.get(i));
		}	//note: the previous line does not consider sigma but rather the size of the previousWeights matrix.  some discussion to be had here...
		
		return weight2;
	}
	
	//will use current weight to return a ranked list for this query
	public ArrayList<Integer> returnRankedList(int query, ArrayList<Double> weight){									//here?
		HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>();
		for(SimplePair qdoc:data.queryMap.get(query)){
			if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc), weight)))
				scores.put(computeDotProduct(data.featureMap.get(qdoc), weight),new HashSet<Integer>());
			scores.get(computeDotProduct(data.featureMap.get(qdoc), weight)).add(qdoc.getItem2());
		}
		return constructFullSortedList(scores);
	}
	
	@SuppressWarnings("unchecked")															//9-11 loop
	private void updateWeightVector(int query){
		ArrayList<Double> nextweight=new ArrayList<Double>(weight);
		for(int i=0; i<unitvector.size(); i++){
			nextweight.add(weight.get(i) + (alpha)*unitvector.get(i));
		}	
		weight=new ArrayList<Double>(nextweight);
	}

	private ArrayList<Integer> computeListClicks(boolean[] clicks, List<String> mixedListKeys){ 
	ArrayList<Integer> listsClicks = new ArrayList<Integer>();
	for(int i=0; i<mixedListKeys.size(); i++){
		if(mixedListKeys.get(i) == "exploit")
			if(clicks[i] == true)
				listsClicks.set(0, listsClicks.get(0)+1);
			else
				continue;
		if(mixedListKeys.get(i) == "explore")
			if(clicks[i] == true)
				listsClicks.set(1, listsClicks.get(1)+1);
			else
				continue;
	}
	return listsClicks;
	}
	
	private boolean[] getClicks(int query, List<Integer> mixedList){
	boolean[] clicks=new boolean[20];
	for(int i=0; i<mixedList.size(); i++){
		boolean rel=false;
		if(data.relevanceMap.get(new SimplePair(query,(int)mixedList.get(i)))>0)
			rel=true;					//access relevance map to get t/f values
		clicks[i]=click.clicks(rel); //true or false for each document in the list
	}
	return clicks;
	}
	
	private LinkedHashMap<String, Integer> balancedInterleave(ArrayList<Integer> list1, ArrayList<Integer> list2, Double k){
		LinkedHashMap<String, Integer> interleavedData = new LinkedHashMap<String, Integer>();
		if(Math.random() > k)
			interleavedData.put("exploit", list1.get(0));
		else
			interleavedData.put("explore", list2.get(0));
		for(int i=1; i<list1.size(); i++){	//do we do this out of the whole list or out of 10? 20? ...
			double val = interleavedData.size()/2;
			String sval = String.valueOf(val);
			if(interleavedData.entrySet().iterator().next().getKey()=="exploit") 
				if(sval.matches("\\D+"))
					if(!interleavedData.containsValue(list2.get(i)))
						interleavedData.put("explore", list2.get(i));
					else
						continue;
				else 
					if(!interleavedData.containsValue(list1.get(i)))
						interleavedData.put("exploit", list1.get(i));
					else
						continue;
			else if(interleavedData.entrySet().iterator().next().getKey()=="explore")
				if(sval.matches("\\D+"))
					if(!interleavedData.containsValue(list1.get(i)))
						interleavedData.put("exploit", list1.get(i));
					else
						continue;
				else
					if(!interleavedData.containsValue(list2.get(i)))
						interleavedData.put("explore", list2.get(i));
					else
						continue;
			if(interleavedData.size() >= 20)
				break;
		}
		return interleavedData;
	}
	
	private LinkedHashMap<String, Integer> probabilisticInterleave(ArrayList<Integer> list1, ArrayList<Integer> list2, Double k){
		LinkedHashMap<String, Integer> interleavedData = new LinkedHashMap<String, Integer>();
		if(Math.random() > k)
			interleavedData.put("exploit", list1.get(0));
		else
			interleavedData.put("explore", list2.get(0));
		for(int i=1; i<list1.size(); i++){	//do we do this out of the whole list or out of 10? 20? ...
			if(Math.random() < k) 
				if(!interleavedData.containsValue(list2.get(i)))
					interleavedData.put("explore", list2.get(i));
				else
					continue;
			else 
				if(!interleavedData.containsValue(list1.get(i)))
					interleavedData.put("exploit", list1.get(i));
				else
					continue;
			if(interleavedData.size() >= 20)
				break;
		}
		return interleavedData;
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
	private double computeDotProduct(ArrayList<Double> array, ArrayList<Double> weight){
		double result=0.0;
		for(int i=0; i<array.size(); i++)
			result+=(array.get(i)*weight.get(i));
		return result;
	}
	
	private List<Double> getUnifRandUnitVector(List<Double> unitvector){
		Random r = new Random();
		List<Double> unitCreator = new ArrayList<Double>();
		List<Double> unitCreator2 = new ArrayList<Double>();
		for(int i=0; i<unitvector.size(); i++){
			Double n = r.nextGaussian();
			unitCreator2.set(i,n);	//set the actual random values, uC2[i] = ith value
			n = n*n;
			unitCreator.set(i,n);	//the squared values used during normalization
		}
		Double mag = Math.sqrt(sum(unitCreator)); //the normalization constant
		for(int i=0; i<unitvector.size(); i++){
			unitvector.set(i, unitCreator2.get(i)/mag); //uniformly random 'featsize'-dimensional unit vector
		}
		return unitvector;
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
	public ArrayList<ArrayList<Double>> listTranspose(ArrayList<ArrayList<Double>> inputList) {
		ArrayList<ArrayList<Double>> table = new ArrayList<ArrayList<Double>>();
		for(int i=0;i<inputList.size();i++){
			List<Double> iValue = inputList.get(i);	//1, 2, 3, 4, ..., 43, 44
			for(int j=0;j<iValue.size();j++){
				table.get(j).add(iValue.get(j));		//hope this works...
		}
		}
		return table;
	    }
}
