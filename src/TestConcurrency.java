/**
 * 
 *
 */
public class TestConcurrency {

    public static void main(String [] a) {
    	testMTQ();
    }

    public static void p(String f, Object ...o) {
        System.out.printf(f, o);
    }

    public static void pl(Object o) {
        System.out.println(o);
    }

    public static void testMTQ() {
        MTQ mtq = new MTQ();
        mtq.start();
    }
    
    public static void testMTPool() {
    	
    }
}
