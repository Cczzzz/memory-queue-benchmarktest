import com.a.eye.datacarrier.DataCarrier;
import com.a.eye.datacarrier.buffer.BufferStrategy;
import com.a.eye.datacarrier.consumer.IConsumer;
import com.a.eye.datacarrier.partition.ProducerThreadPartitioner;
import com.a.eye.datacarrier.partition.SimpleRollingPartitioner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class DataCarrierTest {

    static CountDownLatch countDownLatch;

    public static void main(String[] args) throws InterruptedException {
        DataCarrier<SampleData> carrier = new DataCarrier<SampleData>(5, 5000);
        carrier.setBufferStrategy(BufferStrategy.IF_POSSIBLE);
        carrier.setPartitioner(new SimpleRollingPartitioner<SampleData>());
        final int cont = 1000 * 10000;
        int producer = 10;
        int consumer = 5;

        countDownLatch = new CountDownLatch(consumer);
        SampleData data = new SampleData();
        data.setStr("DataCarrier benchmark test");
        Semaphore semaphore = new Semaphore(producer);

        carrier.consume(MyIConsumer.class, consumer);

        for (int i = 0; i < producer; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < cont; j++) {
                    carrier.produce(data);
                }
                System.out.println(Thread.currentThread().getName() + " producer finish");
                SampleData finish = new SampleData();
                data.setStr("finish");
                while (carrier.produce(finish)) {
                }
            }).start();
        }
        Thread.sleep(1000);
        semaphore.release(producer);
        long start = System.currentTimeMillis();
        countDownLatch.await();
        carrier.shutdownConsumers();
        long end = System.currentTimeMillis();
        double tps = (cont * producer / (end - start)) * 1000;
        System.out.println("--------------------tps---------------  " + tps);
    }

    public static class MyIConsumer implements IConsumer<SampleData> {
        CountDownLatch countDownLatch;

        public void init() {
            this.countDownLatch = DataCarrierTest.countDownLatch;
        }

        public void consume(List<SampleData> list) {
            for (SampleData data : list) {
                Thread.yield();
                int hashCode = data.getStr().hashCode();
                if (data.getStr().equals("finish")) {
                    countDownLatch.countDown();
                    return;
                }
            }
        }

        public void onError(List<SampleData> list, Throwable throwable) {

        }

        public void onExit() {
            System.out.println(Thread.currentThread().getName() + " consumer finish");

        }
    }
}
