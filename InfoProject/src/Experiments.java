import java.util.*;
import java.io.*;

//use this class for running detailed experiments. Keep all methods static
public class Experiments {

	static String defaultpath="/home/mayankkejriwal/IR/mlir/InfoProject/src";
	
	public static void main(String[] args)throws IOException {
		
		runFullPairwiseBaseline(44,"perfect",defaultpath+
				"/host/TREC/TD2004/Data/Fold1/trainingset.txt",
				defaultpath+"/host/TREC/TD2004/Data/All/TD2004.txt");
//		runFullListwiseBaseline("./host/TREC/TD2003/Data/All/TD2003.txt");
	}
	
	public static void runFullPairwiseRLAL(double r,int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Pairwise p=new Pairwise(traindata,featsize,click,r);
		//System.out.println(p.weight);
		p.runRLAL();
		//System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		System.out.println("***");
		System.out.println("Evaluation on train data");
		HashSet<Integer> queries=new HashSet<Integer>(traindata.queryMap.keySet());
		Test test=new Test(traindata,p.weight);
		for(int query: queries){
			ArrayList<Integer> rankedList=test.returnRankedList(query);		//here?
			Metrics m=new Metrics(traindata,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries.size();
		precision/=queries.size();
		ap/=queries.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		System.out.println();
		System.out.println("***");
		System.out.println("Evaluation on test data");
		
		HashSet<Integer> queries1=new HashSet<Integer>(testdata.queryMap.keySet());
		Test test1=new Test(testdata,p.weight);
		for(int query: queries1){
			ArrayList<Integer> rankedList=test1.returnRankedList(query);		//here?
			Metrics m=new Metrics(testdata,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries1.size();
		precision/=queries1.size();
		ap/=queries1.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		
		
	}

	public static void runFullPairwiseReinforcement(double r,int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Pairwise p=new Pairwise(traindata,featsize,click,r);
		//System.out.println(p.weight);
		p.runEpsilonGreedy();
		//System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		System.out.println("***");
		System.out.println("Evaluation on train data");
		HashSet<Integer> queries=new HashSet<Integer>(traindata.queryMap.keySet());
		Test test=new Test(traindata,p.weight);
		for(int query: queries){
			ArrayList<Integer> rankedList=test.returnRankedList(query);		//here?
			Metrics m=new Metrics(traindata,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries.size();
		precision/=queries.size();
		ap/=queries.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		System.out.println();
		System.out.println("***");
		System.out.println("Evaluation on test data");
		
		HashSet<Integer> queries1=new HashSet<Integer>(testdata.queryMap.keySet());
		Test test1=new Test(testdata,p.weight);
		for(int query: queries1){
			ArrayList<Integer> rankedList=test1.returnRankedList(query);		//here?
			Metrics m=new Metrics(testdata,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries1.size();
		precision/=queries1.size();
		ap/=queries1.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		
		
	}
	
	//our sanity check: do the numbers look like those in Table 2?
	public static void runFullPairwiseBaseline(int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Pairwise p=new Pairwise(traindata,featsize,click);
		//System.out.println(p.weight);
		p.runBaseline();
		//System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		System.out.println("***");
		System.out.println("Evaluation on train data");
		HashSet<Integer> queries=new HashSet<Integer>(traindata.queryMap.keySet());
		Test test=new Test(traindata,p.weight);
		for(int query: queries){

			
			ArrayList<Integer> rankedList=p.returnRankedList(query);
			Metrics m=new Metrics(traindata,query,rankedList);

			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries.size();
		precision/=queries.size();
		ap/=queries.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		System.out.println();
		System.out.println("***");
		System.out.println("Evaluation on test data");
		
		HashSet<Integer> queries1=new HashSet<Integer>(testdata.queryMap.keySet());
		Test test1=new Test(testdata,p.weight);
		for(int query: queries1){
			ArrayList<Integer> rankedList=test1.returnRankedList(query);		//here?
			Metrics m=new Metrics(testdata,query,rankedList);
			ndcg+=m.NDCG(10);
			precision+=m.Precision(10);
			ap+=m.AP();
		}
		ndcg/=queries1.size();
		precision/=queries1.size();
		ap/=queries1.size();
		
		System.out.printf("NDCG@10: %f  Precision@10:  %f  MAP:  %f",ndcg,precision,ap);
		
		
	}
	
	
	public static void runFullListwiseBaseline(String inputfile)throws IOException{		//needs editing!
		QRels data=new QRels(inputfile);
		ClickModel click=new ClickModel("perfect");
		int featsize=44;
		Listwise l=new Listwise(data,featsize,click);
		System.out.println(l.weight);
		l.runBaseline();
		System.out.println(l.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		HashSet<Integer> queries=new HashSet<Integer>(data.queryMap.keySet());
		for(int query: queries){
			ArrayList<Integer> rankedList=l.returnRankedList(query, l.weight);	//is this what is needed here?
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
