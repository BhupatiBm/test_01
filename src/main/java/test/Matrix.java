package test;

public class Matrix {
	public static void main(String[] args) {
		String matrix="abcodelmqwqezxyk";
		int n=4;
		String word="welcome";
		System.out.println(formWord(n, matrix, word));
		
	}
	public static Object formWord(int n,String str,String word) {
		char[] charArr = convertToCharArray(word);
		char s[][] = new char[n][n];
		int k=0;
		int total=0;
		for (int i = 0; i < n; i++) 
		{
			for (int j = 0; j < n; j++) 
			{
				if(k < str.length())
					s[i][j] = str.charAt(k);
				k++;
			}
		}

		for (int i = 0; i < n; i++) 
		{
			for (int j = 0; j < n; j++)
			{
				for (int chs = 0; k < charArr.length; k++) {
					if (s[i][j] == 0) 
					{
						break;
					}
					if (s[i][j] ==charArr[chs]) 
					{
						total++;
					}
				}
			}
		}
		if(total==charArr.length+1) {
			return word;
		}
		else 
			return false;

	}
	
	public static char[] convertToCharArray(String word) {
        char[] ch = new char[word.length()];
        for (int i = 0; i < word.length(); i++) 
            ch[i] = word.charAt(i);
		return ch; 
	}
}
