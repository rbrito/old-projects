package Temp;

public class Temp  {
   private static java.util.Dictionary temp = new java.util.Hashtable();
   private static int count;
   private int num;
   public Temp() {num=count++; temp.put(new Integer(num),this);}
   public int getNum() {return num;}
   public String toString() {return "t" + num;}

   public static int getCount() {return count;}
   public static Temp getTemp (int num) {return (Temp)temp.get(new Integer(num));}
}

