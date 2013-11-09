//intended to store queryID in item1 and docID in item2
public class SimplePair {
	private int item1;
	private int item2;
	
	public SimplePair(int a, int b){
		item1=a;
		item2=b;
	}
	
	  @Override public boolean equals(Object aThat) {
		    //check for self-comparison
		    if ( this == aThat ) return true;
		    
		    SimplePair that=(SimplePair) aThat;
		    return(item1==that.getItem1() && item2==that.getItem2());
	  }
	  
	  @Override public int hashCode(){
		  return new Integer(item1+2*item2).hashCode();
	  }

	
	public void setItem1(int a){
		item1=a;
		
		
	}
	
	public void setItem2(int a){
		item2=a;
		
		
	}
	
	public int getItem1(){
		return item1;
	}
	
	public int getItem2(){
		return item2;
	}
	
	public static void main(String[] args){
		//let's test these hashcodes really work
		
		SimplePair a1=new SimplePair(2,3);
		SimplePair a2=new SimplePair(3,2);
		SimplePair a3=new SimplePair(5,5);
		SimplePair a4=new SimplePair(2,3);
		System.out.println(a1.equals(a2));
		System.out.println(a4.equals(a1));
		System.out.println(a1.hashCode()+" "+a2.hashCode()+" "+a3.hashCode()+" "+a4.hashCode()+" ");
		
	}

}
