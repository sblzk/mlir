import java.util.*;
import java.lang.*;
import java.util.regex.Pattern;

public class Listwise {

	QRels data;
	private double alpha;																		//edit
	private double delta;
	private double kappa;
	private double sigma;
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
		sigma=0.5;
		weight=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			weight.add(0.0);
		unitvector=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			unitvector.add(0.0);
		previousWeights = new ArrayList<ArrayList<Double>>();
		
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
	
	public void runBaseline(){
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		iterationcount=1000/iterationcount;
		do{
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
			System.out.println(balancedListKeys);
			//step 6-8: assuming click model on first 20 entries, get labeled triples
			//
			//
			
			List<Integer> clicks = getClicks(query, balancedListValues); //interchangeable with probabilisticListValues
			
			ArrayList<Integer> listsClicks = computeListClicks(clicks, balancedListKeys); //interchangeable with probabilisticListKeys
			System.out.println(clicks);
			System.out.println(listsClicks);
			
			if(listsClicks.get(1) < listsClicks.get(0))
				continue;
			else if(listsClicks.get(1) > listsClicks.get(0))
				updateWeightVector(query, weight2, weight);
		
		}

		iterationcount--;
		}while(iterationcount>0);
		
	}
	
	public void runProbabilisticBaseline(){
		//step 2: outer for loop
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		iterationcount=1000/iterationcount;
		do{
		for(int query:data.queryMap.keySet()){
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			HashMap<Double,HashSet<Integer>> exploratoryscores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			ArrayList<Double> weight2 = new ArrayList<Double>();
			ArrayList<Double> weight3 = new ArrayList<Double>();
			//step 3: sample unit vector uniformly
			unitvector = getUnifRandUnitVector(unitvector);
			//System.out.println(unitvector);
			//step 4: set w'_t = w_t + delta*unitvector
			for(int i=0; i<unitvector.size(); i++){
				Double val = weight.get(i) + delta*unitvector.get(i);
				weight2.add(val);
				weight3.add(val*val);
			}
			Double weight2norm = Math.sqrt(sum(weight3));
			for(int i=0; i<unitvector.size(); i++){
				Double weight2val = weight2.get(i)/weight2norm;
				weight2.set(i, weight2val);
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
			ArrayList<String> probabilisticListKeys = probILKeys(sortedList, exploratoryList, kappa);
			ArrayList<Integer> probabilisticListValues = probILVals(sortedList, exploratoryList, probabilisticListKeys); 
			
			//step 6-8: assuming click model on first 20 entries, get labeled triples		
			List<Integer> clicks = getClicks(query, probabilisticListValues); 
			ArrayList<Integer> listsClicks = computeListClicks(clicks, probabilisticListKeys); 
			//System.out.println(listsClicks);
			
			if(listsClicks.get(1) < listsClicks.get(0))
				continue;
			else if(listsClicks.get(1) > listsClicks.get(0))
				updateWeightVector(query, weight2, weight);
		}
		iterationcount--;
		}while(iterationcount>0);
		
	}
	
	public void runBalancedMA(){
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		iterationcount=1000/iterationcount;
		do{
		for(int query:data.queryMap.keySet()){
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			HashMap<Double,HashSet<Integer>> exploratoryscores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			ArrayList<Double> weight2 = new ArrayList<Double>();
			
			//step 3: sample unit vector uniformly
			unitvector = getUnifRandUnitVector(unitvector);
			
			//step 4: set w'_t = w_t + delta*unitvector
			if(previousWeights.size()==0){
				for(int i=0; i<unitvector.size(); i++){
				weight2.add(weight.get(i) + delta*unitvector.get(i));
				}
			}else{
			weight2 = movingAverage(weight, unitvector, previousWeights, delta, sigma);
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
			
			//step 6-8: assuming click model on first 20 entries, get labeled triples
			List<Integer> clicks = getClicks(query, balancedListValues); //interchangeable with probabilisticListValues
			ArrayList<Integer> listsClicks = computeListClicks(clicks, balancedListKeys); //interchangeable with probabilisticListKeys
			
			if(listsClicks.get(1) < listsClicks.get(0))
				continue;
			else if(listsClicks.get(1) > listsClicks.get(0))
				updateWeightVector(query, weight2, weight);
			
			updatePreviousWeights(weight2, listsClicks, previousWeights);
		}
		iterationcount--;
		}while(iterationcount>0);
		
	}
	
	public void runBalancedMMA(){
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		iterationcount=1000/iterationcount;
		do{
		for(int query:data.queryMap.keySet()){
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			HashMap<Double,HashSet<Integer>> exploratoryscores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			ArrayList<Double> weight2 = new ArrayList<Double>();
			
			//step 3: sample unit vector uniformly
			unitvector = getUnifRandUnitVector(unitvector);
			
			//step 4: set w'_t = w_t + delta*unitvector
			weight2 = movingAverage(weight, unitvector, previousWeights, delta, sigma);
			
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
			System.out.println(balancedListKeys);
			
			//step 6-8: assuming click model on first 20 entries, get labeled triples
			List<Integer> clicks = getClicks(query, balancedListValues); //interchangeable with probabilisticListValues
			ArrayList<Integer> listsClicks = computeListClicks(clicks, balancedListKeys); //interchangeable with probabilisticListKeys
			
			if(listsClicks.get(1) < listsClicks.get(0)){
				if(sigma != 0.0){
					sigma -= 0.05;
				}else if(sigma == 0.0){
					continue;
				}
				continue;
			}else if(listsClicks.get(1) > listsClicks.get(0)){
				if(sigma != 1.0){
					sigma += 0.05;
				}else if(sigma == 1.0){
					continue;
				}
				updateWeightVector(query, weight2, weight);
			}		
			updatePreviousWeights(weight2, listsClicks, previousWeights);
		}
		iterationcount--;
		}while(iterationcount>0);
		
	}
	
	public void updatePreviousWeights(List<Double> weight2, ArrayList<Integer> listsClicks, ArrayList<ArrayList<Double>> previousWeights){
		if(listsClicks.get(1) > listsClicks.get(0))
			if(previousWeights.size()>=20)
				previousWeights.remove(0);	//this should remove the oldest value		
			previousWeights.add((ArrayList)weight2);
	}
	
	public ArrayList<Double> movingAverage(ArrayList<Double> weight, List<Double> unitvector, ArrayList<ArrayList<Double>> previousWeights, Double delta, Double sigma){
		ArrayList<Double> weight2 = new ArrayList<Double>();
		ArrayList<Double> previousNonNormalizedWeights = new ArrayList<Double>();
		//step 4: set w'_t = w_t + delta*unitvector
		for(int i=0; i<unitvector.size(); i++){

													//					sum(each column) / size(each column)								+	(coefficient) UnifRandUnitVector
				previousNonNormalizedWeights.set(i, sum(listTranspose(previousWeights).get(i))/(listTranspose(previousWeights).get(i).size()) + sigma*unitvector.get(i));
				Double lag = Math.sqrt(sum(previousNonNormalizedWeights));
				weight2.add(weight.get(i)+previousNonNormalizedWeights.get(i)/lag);
		}
		for(int i=0; i<unitvector.size(); i++){
			Double mag = Math.sqrt(sum(weight2)); //the normalization constant
			weight2.set(i, weight2.get(i)/mag); //uniformly random 'featsize'-dimensional unit vector
		}	
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
	private void updateWeightVector(int query, List<Double> weight2, List<Double> weight){
		ArrayList<Double> nextweight=new ArrayList<Double>();
		for(int i=0; i<unitvector.size(); i++){
			nextweight.add(weight.get(i) + 10*(alpha)*weight2.get(i));
		}
		System.out.println("next weight:");
		System.out.println(nextweight);
		weight = nextweight;
		System.out.println("new weight:");
		System.out.println(weight);
	}

	private ArrayList<Integer> computeListClicks(List<Integer> clicks, List<String> mixedListKeys){ 
	ArrayList<Integer> listsClicks = new ArrayList<Integer>();
	listsClicks.add(0);
	listsClicks.add(0);
	for(int i=0; i<100; i++){
		if(mixedListKeys.get(i) == "exploit")
			if(clicks.get(i) == 1){
				int new0val = listsClicks.get(0)+1;
				listsClicks.set(0, new0val);
			}else{
				continue;
			}
		if(mixedListKeys.get(i) == "explore"){
			if(clicks.get(i) == 1){
				int new1val = listsClicks.get(1)+1;
				listsClicks.set(1, new1val);
			}else{
				continue;
			}
		}
	}
	return listsClicks;
	}
	
	private List<Integer> getClicks(int query, List<Integer> mixedList){
	List<Integer> clickdata = new ArrayList<Integer>();
	for(int i=0; i<100; i++){
		if(data.relevanceMap.get(new SimplePair(query,mixedList.get(i)))>0){
			clickdata.add(1);
		}else if(data.relevanceMap.get(new SimplePair(query,mixedList.get(i)))==0){
			clickdata.add(0);
		}
	}
	System.out.println(clickdata);
	return clickdata;
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
	
	private ArrayList<String> probILKeys(ArrayList<Integer> list1, ArrayList<Integer> list2, Double k){
		ArrayList<String> interleavedKeys = new ArrayList<String>();
		for(int i=0; i<list1.size(); i++){	
			if(Math.random() > k){
				interleavedKeys.add("exploit");
			}else{
				interleavedKeys.add("explore");
			}
		}
		return interleavedKeys;
	}
	
	private ArrayList<Integer> probILVals(ArrayList<Integer> list1, ArrayList<Integer> list2, ArrayList<String> interleavedKeys){
		ArrayList<Integer> interleavedVals = new ArrayList<Integer>();
		for(int i=0; i<list1.size(); i++){
			if(interleavedKeys.get(i) == "explore"){
				if(!interleavedVals.contains(list2.get(i))){
					interleavedVals.add(list2.get(i));
				}else if(interleavedVals.contains(list2.get(i))){
					for(int j=1; i< list1.size()-i; j++){
						if(!interleavedVals.contains(list2.get(i+j))){
							interleavedVals.add(list2.get(i+j));
							break;
						}else{
							continue;
						}
					}
				}
			}else if(interleavedKeys.get(i) == "exploit"){
				if(!interleavedVals.contains(list1.get(i))){
					interleavedVals.add(list1.get(i));
				}else if(interleavedVals.contains(list1.get(i))){
					for(int j=1; i< list1.size()-i; j++){
						if(!interleavedVals.contains(list1.get(i+j))){
							interleavedVals.add(list1.get(i+j));
							break;
						}else{
							continue;
						}
					}
				}
			}
		}
		return interleavedVals;
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
			unitCreator2.add(n);	//set the actual random values, uC2[i] = ith value
			n = n*n;
			unitCreator.add(n);	//the squared values used during normalization
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
			List<Double> iValue = inputList.get(i);	//1, 2, 3, 4, ..., n (44 values for each [1])
			for(int j=0;j<iValue.size();j++){
				table.get(j).add(iValue.get(j));		//hope this works...
		}
		}
		return table;
	    }
}
