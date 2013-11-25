
import java.util.*;

public class Metrics {
	
	QRels data;
	int qid;
	ArrayList<Integer> docids; //ranked in descending order of importance
	
	ArrayList<Integer> relevance;
	
	
	public Metrics(QRels data, int qid, ArrayList<Integer> rankedList){
		this.data=data;
		this.qid=qid;
		docids=rankedList;
		
		relevance=new ArrayList<Integer>();
		for(int i=0; i<docids.size(); i++){
			int docid=docids.get(i);
			SimplePair m=new SimplePair(qid,docid);
			relevance.add(data.relevanceMap.get(m));
		}
		
	}
	
	//NDCG@k
	public double NDCG(int k){
		ArrayList<Integer> ideal=new ArrayList<Integer>(relevance);
		Collections.sort(ideal); //ascending order by default: careful
		ideal=reverse(ideal,k);	//will have some overhead but shouldn't be much
		
		double dcg=relevance.get(0);
		for(int i=1; i<k && i<relevance.size(); i++)
			dcg+=(relevance.get(i)*1.0/(Math.log(i+1)/Math.log(2)));
		
		double idcg=ideal.get(0);
		for(int i=1; i<k && i<ideal.size(); i++)
			idcg+=(ideal.get(i)*1.0/(Math.log(i+1)/Math.log(2)));
		
		if(idcg==0.0)
			return 1.0;
		else
			return dcg/idcg;
					
	}
	
	//Average precision
	public double AP(){
		ArrayList<Integer> ideal=new ArrayList<Integer>(relevance);
		Collections.sort(ideal); //ascending order by default: careful
		ideal=reverse(ideal,-1);	//will have some overhead but shouldn't be much
		
		int rcount=0;
		double psum=0.0;
		for(int i=0; i<relevance.size(); i++)
			if(relevance.get(i)>0){
				rcount++;
				psum+=(sum(relevance,i+1)*1.0/sum(ideal,i+1));
			}
		if(rcount==0)
			return 1;
		else
			return psum/rcount;
		
	}
	
	//Precision@k
	public double Precision(int k){
		ArrayList<Integer> ideal=new ArrayList<Integer>(relevance);
		Collections.sort(ideal); //ascending order by default: careful
		ideal=reverse(ideal,k);	//will have some overhead but shouldn't be much
		
		int isum=sum(ideal,k);
		int dsum=sum(relevance,k);
		if(isum==0)
			return 1.0;
		else
			return dsum*1.0/isum;
		
	}
	
	private int sum(ArrayList<Integer> p, int k){
		if(p.size()<k || k==-1)
			k=p.size();
		int sum=0;
		for(int i=0; i<k; i++)
			sum+=p.get(i);
		return sum;
	}
	
	//if k is -1, reverse full list, otherwise reverse only up to k elements from the end
	private ArrayList<Integer> reverse(ArrayList<Integer> p, int k){
		if(p.size()<k || k==-1)
			k=p.size();
		ArrayList<Integer> result=new ArrayList<Integer>();
		for(int i=p.size()-1; i>=p.size()-k; i--)
			result.add(p.get(i));
		return result;
	}

}
