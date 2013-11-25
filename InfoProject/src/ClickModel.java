import java.util.HashSet;
import java.util.Random;


public class ClickModel {
	
	double cR;	//p(c|R)
	double cNR;//p(c|NR)...and similarly for the other two
	double sR;
	double sNR;
	
	//works for opt="perfect", "navigational" or "informational"
	public ClickModel(String opt){
		if(opt.equals("perfect")){
				cR=1.0;
				cNR=0.0;
				sR=0.0;
				sNR=0.0;
		}
		else if(opt.equals("navigational")){
			cR=0.95;
			cNR=0.05;
			sR=0.9;
			sNR=0.2;
		}
		else if(opt.equals("informational")){
			cR=0.9;
			cNR=0.4;
			sR=0.5;
			sNR=0.1;
		}
	}
	
	//for arbitrary numbers. We won't do stringent checks on probabilities.
	public ClickModel(double...probabilities){
		cR=probabilities[0];
		cNR=probabilities[1];
		sR=probabilities[2];
		sNR=probabilities[3];
	}
	
	//returns true if click, otherwise false: rel indicates if document is relevant or not
	//this function provides a way of interpreting the probabilities to return a decision
	//LOGIC: generate two random numbers between 0 and val, where val is the (rounded up integer) inverse of the probability in question
	//The probability that the two numbers are equal is 1/val 
	public boolean clicks(boolean rel){
		//first deal with base case
		if(rel)
			if(cR==0.0)
				return false;
			else if(cR==1.0)
				return true;
		if(!rel)
			if(cNR==0.0)
				return false;
			else if(cNR==1.0)
				return true;
		
		Random p1=new Random((new Random()).nextInt());
		Random p2=new Random((new Random()).nextInt());
		
		
		if(rel){
			int val=(int)Math.floor(cR*100);
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
		else{
			int val=(int)Math.floor(cNR*100);
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
	}
	
	public boolean stops(boolean rel){
		//first deal with base case
		if(rel)
			if(sR==0.0)
				return false;
			else if(sR==1.0)
				return true;
		if(!rel)
			if(sNR==0.0)
				return false;
			else if(sNR==1.0)
				return true;
		
		Random p1=new Random((new Random()).nextInt());
		Random p2=new Random((new Random()).nextInt());
		
		
		if(rel){
			int val=(int)Math.floor(sR*100);
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
		else{
			int val=(int)Math.floor(sNR*100);
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
	}
	
	public static void main(String[] args){
		ClickModel c=new ClickModel("informational");
		double tcount=0.0;
		for(int i=0; i<100000; i++)
			if(c.clicks(true))
				tcount++;
		System.out.println(tcount/100000+" "+c.cR);
	}

}
