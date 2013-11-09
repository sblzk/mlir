import java.util.*;
import java.io.*;

//use this class for running detailed experiments. Keep all methods static
public class Experiments {

	
	public static void main(String[] args)throws IOException {
		
		runFullPairwiseBaseline("/host/TREC/TD2003/All/TD2003.txt");
	}
	
	//our sanity check: do the numbers look like those in Table 2?
	public static void runFullPairwiseBaseline(String inputfile)throws IOException{
		QRels data=new QRels(inputfile);
		ClickModel click=new ClickModel("perfect");
		int featsize=44;
		Pairwise p=new Pairwise(data,featsize,click);
		System.out.println(p.weight);
		p.runBaseline();
		System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		HashSet<Integer> queries=new HashSet<Integer>(data.queryMap.keySet());
		for(int query: queries){
			ArrayList<Integer> rankedList=p.returnRankedList(query);
			Metrics m=new Metrics(data,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries.size();
		precision/=queries.size();
		ap/=queries.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		
	}

}
