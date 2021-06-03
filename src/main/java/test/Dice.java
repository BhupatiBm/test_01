package test;

public class Dice {
	public static void main(String[] args) {
		int[] i= {1,2,3,4,5,6};
		System.out.println(combination(10, i));	
	}
	
	public static int combination(int n,int[] i) {
		int total=0;
		for (int j : i) {
			for (int k : i) {
				if(j+k==n)
					total++;
			}
		}
		return total;
	}

}
