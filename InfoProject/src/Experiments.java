import java.text.DecimalFormat;
import java.util.*;
import java.io.*;

//use this class for running detailed experiments. Keep all methods static
public class Experiments {

	//static String defaultpath="/home/mayankkejriwal/IR/mlir/InfoProject/src";
	static String defaultpath="./";
	
	public static void main(String[] args)throws IOException {
		/*
		runFullPairwiseBaseline(44,"perfect",defaultpath+
				"/host/TREC/TD2004/Data/Fold1/trainingset.txt",
				defaultpath+"/host/TREC/TD2004/Data/All/TD2004.txt");
		runProbabilisticListwiseBaseline(44,"perfect",
				"./host/TREC/TD2004/Data/Fold1/trainingset.txt",
				"./host/TREC/TD2004/Data/All/TD2004.txt");
	*/
		
		ArrayList<double[][]> res=PairwiseExperimentsTREC(defaultpath+
				"/host/TREC/TD2003/Data/Fold5/trainingset.txt",
				defaultpath+
				"/host/TREC/TD2003/Data/Fold5/testset.txt");
		printResults(res.get(0));
		printResults(res.get(1));
		printResults(res.get(2));
	
	}
	
	public static void printResults(double[][] m){
		DecimalFormat q=new DecimalFormat("0.000");
		for(int i=0; i<m.length; i++){
			for(int j=0; j<m[i].length; j++)
				System.out.print(q.format(m[i][j])+"  ");
			System.out.println();
			}
	}
	
	
	public static ArrayList<double[][]>  PairwiseExperimentsTREC(String trainfile, String testfile) throws IOException{
		ArrayList<double[][]> results=new ArrayList<double[][]>();
		
		String clickmodel="perfect";
		double[][] perfect=new double[3][13];
		double[] base=returnFullPairwiseBaseline(44,clickmodel,trainfile,testfile);
		fillColumn(perfect,base,0);
		 int i=1;
		 for(double r=0.0; r<=1.0; r+=0.2){
			 fillColumn(perfect,returnFullPairwiseReinforcement(r,44,clickmodel,trainfile,testfile),i);
			 fillColumn(perfect,returnFullPairwiseRLAL(r,44,clickmodel,trainfile,testfile),i+6);
			 i++;
		 }
		 results.add(perfect);
		 
		 clickmodel="navigational";
			double[][] nav=new double[3][13];
			base=returnFullPairwiseBaseline(44,clickmodel,trainfile,testfile);
			fillColumn(nav,base,0);
			i=1;
			 for(double r=0.0; r<=1.0; r+=0.2){
				 fillColumn(nav,returnFullPairwiseReinforcement(r,44,clickmodel,trainfile,testfile),i);
				 fillColumn(nav,returnFullPairwiseRLAL(r,44,clickmodel,trainfile,testfile),i+6);
				 i++;
			 }
			 results.add(nav);
			 
			 clickmodel="informational";
				double[][] info=new double[3][13];
				base=returnFullPairwiseBaseline(44,clickmodel,trainfile,testfile);
				fillColumn(info,base,0);
				 i=1;
				 for(double r=0.0; r<=1.0; r+=0.2){
					 fillColumn(info,returnFullPairwiseReinforcement(r,44,clickmodel,trainfile,testfile),i);
					 fillColumn(info,returnFullPairwiseRLAL(r,44,clickmodel,trainfile,testfile),i+6);
					 i++;
				 }
				 results.add(info);
				 
				 return results;
		
	}
	
	private static void fillColumn(double[][] matrix, double[] column, int columnindex){
		for(int i=0; i<column.length; i++){
			matrix[i][columnindex]=column[i];
		}
	}
	
	public static double[][] averageMatrices(ArrayList<double[][]> matrices){
		double[][] result=new double[matrices.get(0).length][matrices.get(0)[0].length];
		for(int i=0; i<matrices.size(); i++)
			sumMatrices(result,matrices.get(i));
		
		for(int i=0; i<result.length; i++)
			for(int j=0; j<result[i].length; j++)
				result[i][j]/=matrices.size();
		
		return result;
	}
	
	private static void sumMatrices(double[][] p, double[][] q){
		for(int i=0; i<p.length; i++)
			for(int j=0; j<p[i].length; j++)
				p[i][j]+=q[i][j];
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
		System.out.println(p.weight);
		p.runBaseline();
		System.out.println(p.weight);
		
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
	
	
	public static void runFullListwiseBaseline(int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Listwise p=new Listwise(traindata,featsize,click);
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

			
			ArrayList<Integer> rankedList=p.returnRankedList(query, p.weight);
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
	
	public static void runProbabilisticListwiseBaseline(int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Listwise p=new Listwise(traindata,featsize,click);
		System.out.println(p.weight);
		p.runProbabilisticBaseline();
		System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		System.out.println("***");
		System.out.println("Evaluation on train data");
		HashSet<Integer> queries=new HashSet<Integer>(traindata.queryMap.keySet());
		Test test=new Test(traindata,p.weight);
		for(int query: queries){

			
			ArrayList<Integer> rankedList=p.returnRankedList(query, p.weight);
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
	
	public static void runBalancedMA(int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
		QRels traindata=new QRels(trainfile);
		QRels testdata=new QRels(testfile);
		ClickModel click=new ClickModel(clickmodel);
		
		Listwise p=new Listwise(traindata,featsize,click);
		System.out.println(p.weight);
		p.runBalancedMA();
		System.out.println(p.weight);
		
		double ndcg=0;
		double precision=0;
		double ap=0;
		
		System.out.println("***");
		System.out.println("Evaluation on train data");
		HashSet<Integer> queries=new HashSet<Integer>(traindata.queryMap.keySet());
		Test test=new Test(traindata,p.weight);
		for(int query: queries){

			
			ArrayList<Integer> rankedList=p.returnRankedList(query, p.weight);
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

	private static double[] returnFullPairwiseRLAL(double r,int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
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
		
		double[] result=new double[3];
		result[0]=ndcg;
		result[1]=precision;
		result[2]=ap;
		return result;
		
		
	}

	private static double[] returnFullPairwiseReinforcement(double r,int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
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
		
		double[] result=new double[3];
		result[0]=ndcg;
		result[1]=precision;
		result[2]=ap;
		return result;
		
		
	}

	//our sanity check: do the numbers look like those in Table 2?
	private static double[] returnFullPairwiseBaseline(int featsize, String clickmodel, String trainfile, String testfile)throws IOException{
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
		
		double[] result=new double[3];
		result[0]=ndcg;
		result[1]=precision;
		result[2]=ap;
		return result;
		
		
		
		
	}

}
