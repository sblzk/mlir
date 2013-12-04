import java.util.*;

public class Pairwise {

	QRels data;
	private double eta;
	private double lambda;
	ArrayList<Double> weight;
	ClickModel click;
	double r;
	
	public Pairwise(QRels data, int featsize, ClickModel click){
		this.data=data;
		this.click=click;
		eta=0.05;
		lambda=0.0;
		weight=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			weight.add(0.0);
		
	}
	
	//r is 1-epsilon. r=0 is purely exploitative
	public Pairwise(QRels data, int featsize, ClickModel click, double r){
		this.data=data;
		this.click=click;
		eta=0.05;
		lambda=0.0;
		weight=new ArrayList<Double>();
		for(int i=0; i<featsize; i++)
			weight.add(0.0);
		this.r=r;
		
	}
	
	//assumes default featsize 5
	public Pairwise(){
		eta=0.001;
		lambda=0.0;
		weight=new ArrayList<Double>();
		for(int i=0; i<5; i++)
			weight.add((double)i);
	}
	
	public void setParameters(double eta, double lambda, ArrayList<Double> weight){
		this.eta=eta;
		this.lambda=lambda;
		this.weight=weight;
	}
	
	//weight will get updated after all queries are run. Please refer to Hoffman for pseudocode of steps
	public void runBaseline(){
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		iterationcount=1000/iterationcount;
		do{
		for(int query:data.queryMap.keySet()){
			
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			//steps 3-4: extract scores
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc))))
					scores.put(computeDotProduct(data.featureMap.get(qdoc)),new HashSet<Integer>());
				scores.get(computeDotProduct(data.featureMap.get(qdoc))).add(qdoc.getItem2());
			}
			//step 5: sort entire list: this is where the difference between all the algorithms come in
			ArrayList<Integer> sortedList=constructFullSortedList(scores);
			
			//step 6-8: assuming click model on first ten entries, get labeled triples
			boolean[] clicks=new boolean[10];
			for(int i=0; i<10; i++){
				boolean rel=false;
				if(data.relevanceMap.get(new SimplePair(query,(int)sortedList.get(i)))>0)
					rel=true;
				clicks[i]=click.clicks(rel);
			}
			
			ArrayList<SimpleTriple> training=getImplicitFeedback(clicks,sortedList);
			//System.out.println(training.size());
			
			//steps 9-11: update the weight vector
			if(training.size()==0)
				continue;
			updateWeightVector(training, query);
		
		}
		iterationcount--;
		}while(iterationcount>0);
		
		//step 12 not explicit: the final weight vector should be in 'weight'
	}
	
	//weight will get updated after all queries are run. Please refer to Hoffman for pseudocode of steps
	public void runEpsilonGreedy(){
		//step 2: outer for loop
		int iterationcount=data.queryMap.keySet().size();
		//iterationcount=1000/iterationcount;
		//do{
		for(int query:data.queryMap.keySet()){
			
			HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
			//steps 3-4: extract scores
			for(SimplePair qdoc:data.queryMap.get(query)){
				if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc))))
					scores.put(computeDotProduct(data.featureMap.get(qdoc)),new HashSet<Integer>());
				scores.get(computeDotProduct(data.featureMap.get(qdoc))).add(qdoc.getItem2());
			}
			//step 5: sort entire list: this is where the difference between all the algorithms come in
			ArrayList<Integer> exploitList=constructFullSortedList(scores);
			ArrayList<Integer> sortedList=new ArrayList<Integer>();
			
			int iter=0;
			int exploit=0;
			while(iter<10){
				if(isExploratory()){
					Random k=new Random(4324324+iter+iterationcount);
					int m=k.nextInt(exploitList.size());
					while(sortedList.contains(exploitList.get(m)))
						m=k.nextInt(exploitList.size());
					
					sortedList.add(exploitList.get(m));	
				}
				else{
					while(sortedList.contains(exploitList.get(exploit)))
						exploit++;
					sortedList.add(exploitList.get(exploit));
				}
				
				iter++;
			}
			
			//step 6-8: assuming click model on first ten entries, get labeled triples
			boolean[] clicks=new boolean[10];
			for(int i=0; i<10; i++){
				boolean rel=false;
				if(data.relevanceMap.get(new SimplePair(query,(int)sortedList.get(i)))>0)
					rel=true;
				clicks[i]=click.clicks(rel);
			}
			
			ArrayList<SimpleTriple> training=getImplicitFeedback(clicks,sortedList);
			//System.out.println(training.size());
			
			//steps 9-11: update the weight vector
			if(training.size()==0)
				continue;
			updateWeightVector(training, query);
		
		}
		//iterationcount--;
		//}while(iterationcount>0);
		
		//step 12 not explicit: the final weight vector should be in 'weight'
	}
	
	//weight will get updated after all queries are run. Please refer to Hoffman for pseudocode of steps
		public void runRLAL(){
			//step 2: outer for loop
			//int iterationcount=data.queryMap.keySet().size();
			//iterationcount=1000/iterationcount;
			//do{
			for(int query:data.queryMap.keySet()){
				
				HashMap<Double,HashSet<Integer>> scores=new HashMap<Double,HashSet<Integer>>(); //for this query and weight, scores hashed to docid set
				//steps 3-4: extract scores
				for(SimplePair qdoc:data.queryMap.get(query)){
					if(!scores.containsKey(computeDotProduct(data.featureMap.get(qdoc))))
						scores.put(computeDotProduct(data.featureMap.get(qdoc)),new HashSet<Integer>());
					scores.get(computeDotProduct(data.featureMap.get(qdoc))).add(qdoc.getItem2());
				}
				//step 5: sort entire list: this is where the difference between all the algorithms come in
				ArrayList<Integer> exploitList=constructFullSortedList(scores);
				ArrayList<Integer> exploreList=getActiveLearningVector(exploitList);
				ArrayList<Integer> sortedList=new ArrayList<Integer>();
				
				int iter=0;
				int exploit=0;
				int explore=0;
				while(iter<10){
					if(isExploratory()){
						
						while(sortedList.contains(exploreList.get(explore)))
							explore++;
						
						
						
						sortedList.add(exploreList.get(explore));	
					}
					else{
						while(sortedList.contains(exploitList.get(exploit)))
							exploit++;
						sortedList.add(exploitList.get(exploit));
					}
					
					iter++;
				}
				
				//step 6-8: assuming click model on first ten entries, get labeled triples
				boolean[] clicks=new boolean[10];
				for(int i=0; i<10; i++){
					boolean rel=false;
					if(data.relevanceMap.get(new SimplePair(query,(int)sortedList.get(i)))>0)
						rel=true;
					clicks[i]=click.clicks(rel);
				}
				
				ArrayList<SimpleTriple> training=getImplicitFeedback(clicks,sortedList);
				//System.out.println(training.size());
				
				//steps 9-11: update the weight vector
				if(training.size()==0)
					continue;
				updateWeightVector(training, query);
			
			}
			//iterationcount--;
			//}while(iterationcount>0);
			
			//step 12 not explicit: the final weight vector should be in 'weight'
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
	
	private boolean isExploratory(){
		
			if(r==1.0)
				return true;
			else if(r==0.0)
				return false;
			
			Random p1=new Random((new Random()).nextInt());
			Random p2=new Random((new Random()).nextInt());
			
			
			
				int val=(int)Math.floor(r*100);
				HashSet<Integer> forbidden=new HashSet<Integer>();
				int count=0;
				while(count<100-val){
					int q1=p1.nextInt(100);
					if(!forbidden.contains(q1)){
						forbidden.add(q1);
						count++;
					}
						
				}
				
				int q2=p2.nextInt(100);
				if(forbidden.contains(q2))
					return false;
				else
					return true;
			
			
		}
	
	private ArrayList<Integer> getActiveLearningVector(ArrayList<Integer> sortedList){
		ArrayList<Integer> result=new ArrayList<Integer>();
		if(sortedList==null||sortedList.size()==0)
			return null;
		int middle=sortedList.size()/2;
		int i=1;
		result.add(sortedList.get(middle));
		while(true){
			if(middle-i<0)
				break;
			result.add(sortedList.get(middle-i));
			if(middle+i>=sortedList.size())
				break;
			result.add(sortedList.get(middle+i));
			i++;
		}
		
		return result;
	}
	
	
	@SuppressWarnings("unchecked")
	private void updateWeightVector(ArrayList<SimpleTriple> training, int query){
		ArrayList<Double> nextweight=new ArrayList<Double>(weight);
		for(int i=0; i<training.size(); i++){
			ArrayList<Double> f1=data.featureMap.get(new SimplePair(query,training.get(i).x1));
			ArrayList<Double> f2=data.featureMap.get(new SimplePair(query,training.get(i).x2));
			ArrayList<Double> diff=MultMinus(f1,f2,training.get(i).y);
			if(computeDotProduct(diff)<1.0)
				nextweight=sumVectors(weight,MultMinus(diff,null,eta),MultMinus(weight,null,-1.0*eta*lambda));
			
			
		}
		weight=new ArrayList<Double>(nextweight);
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
	private double computeDotProduct(ArrayList<Double> array){
		double result=0.0;
		
		for(int i=0; i<array.size(); i++)
			result+=(array.get(i)*weight.get(i));
		
		
		return result;
	}
	
	//computes either (f1-f2)mult or f1mult
	private ArrayList<Double> MultMinus(ArrayList<Double> f1, ArrayList<Double> f2, double mult){
		ArrayList<Double> result=new ArrayList<Double>(f1);
		
			for(int i=0; i<f1.size(); i++)
				if(f2!=null)
					result.set(i,(result.get(i)-f2.get(i))*mult);
				else
					result.set(i,result.get(i)*mult);
		return result;
	}
	
	
	private ArrayList<Double> sumVectors(@SuppressWarnings("unchecked") ArrayList<Double>...vectors){
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
	
	//will generate integers cast as doubles: used for testing
	private static ArrayList<Integer> generateIntegerVector(int size){
		ArrayList<Integer> m=new ArrayList<Integer>();
		for(int i=0; i<size; i++){
			
			m.add(i);
		}
		return m;
	}
	
	//will generate integers cast as doubles: used for testing
		@SuppressWarnings("unused")
		private static ArrayList<Double> generateDoubleVector(int size){
			ArrayList<Double> m=new ArrayList<Double>();
			for(int i=0; i<size; i++){
				Random p=new Random();
				m.add((double)p.nextInt(size));
			}
			return m;
		}
	
	//lets test
	public static void main(String[] args){
		
		Pairwise p=new Pairwise();
		ArrayList<Integer> a=generateIntegerVector(10);
		System.out.println(a);
		boolean[] click={true,true,false,true,false,false,false,true,false,true};
		ArrayList<SimpleTriple> b=p.getImplicitFeedback(click,a);
		for(int i=0; i<b.size(); i++)
			System.out.println(b.get(i).x1+" "+b.get(i).x2+" "+b.get(i).y);
		
		
		
	}
}

