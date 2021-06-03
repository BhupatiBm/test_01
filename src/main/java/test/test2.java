package test;

import java.util.ArrayList;
import java.util.List;

public class test2 {

	public static void printSeries(int i,int j) {
		List<String> list=new ArrayList<String>();	
		for (int k = 0; k < j; k++) {
			if(k==0)
			list.add(String.valueOf(i));
			if(k==1)
				list.add(String.valueOf(i*2)+" "+String.valueOf(i*2+1));		
			else if (k>1) {
				String[] split = list.get(k-1).split(" ");
				int firstElement = Integer.valueOf(split[0]);
				int lastElement = Integer.valueOf(split[split.length-1]);
				int startArray=0;
				int endArray=0;
				if(k==2) {
					startArray=lastElement*2+1;
					endArray=firstElement*2;
				}
				else {
					startArray=firstElement*2+1;
					endArray=lastElement*2;
				}
				
				String val=String.valueOf(startArray);
				int count=1;
				while (startArray!=endArray) {
					startArray--;
					val=val+" "+String.valueOf(startArray);	
				}
				//val=val+" "+endArray;
				list.add(val);
			}	
		}
		System.out.println(list);
	}
	public static void main(String[] args) {

printSeries(7, 4);

	}

}
