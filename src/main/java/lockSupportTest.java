import java.util.concurrent.locks.LockSupport;

public class lockSupportTest {

    static Object lock = new Object();


    public static void main(String[] args) {
        LockSupport.unpark(Thread.currentThread());
        LockSupport.park();
        System.out.println("aaa");
        LockSupport.park();
        System.out.println("bbb");
    }
}
