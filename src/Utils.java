import java.util.Random;

public class Utils {

   Random random = null;
   public final static String charsetDigits = "0123456789";
   public final static String charsetAlpha = "ABCDEFGHJIKLMNOPQRSTUVWXYZ";
   public final static String charsetAlphaNum = charsetDigits + charsetAlpha;

   public Utils() {
        random = new Random();
   }
   
   public int getInt(int min, int max) {
       int diff = max - min;
       int v = random.nextInt(diff) + min;
       return v;
   }

   public char getChar() {
       return getChar(charsetAlphaNum);
   }
   
   public char getChar(String charset) {
       int i = getInt(0, charset.length());
       return charset.charAt(i);
   }
   
   public String getString(String charset, int sz) {
       StringBuilder sb = new StringBuilder();
       for(int i = 0; i < sz; i++) {
           sb.append(getChar(charset));
       }
       final String s = sb.toString();
       return s;
   }

   public String getString(int sz) {
       return getString(charsetAlphaNum, sz);
   }

}