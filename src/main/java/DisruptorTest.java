import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class DisruptorTest {

    public static void main(String[] args) throws InterruptedException {
        final int cont = 1000 * 10000;
        int producer = 5;
        int consumer = 5;
        EventFactory<DisruptorEvent> eventFactory = DisruptorEvent::new;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        Disruptor<DisruptorEvent> disruptor = new Disruptor<DisruptorEvent>(eventFactory, 1048576, threadFactory, ProducerType.MULTI, new BusySpinWaitStrategy());
        RingBuffer<DisruptorEvent> ringBuffer = disruptor.getRingBuffer();

        EventHandler<DisruptorEvent>[] consumerList = new EventHandler[consumer];
        for (int i = 0; i < consumer; i++) {
            consumerList[i] = new MyEventHandler();
        }
        disruptor.handleEventsWith(consumerList);

        CountDownLatch countDownLatch = new CountDownLatch(producer);
        SampleData data = new SampleData();
        data.setStr("Disruptor benchmark test");

        Semaphore semaphore = new Semaphore(producer);

        for (int i = 0; i < producer; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (int j = 0; j < cont; j++) {
                    long next = ringBuffer.next();
                    DisruptorEvent disruptorEvent = ringBuffer.get(next);
                    disruptorEvent.setMessage(data);
                    ringBuffer.publish(next);
                }
                countDownLatch.countDown();
                System.out.println("down");
            }).start();
        }
        Thread.sleep(1000);
        semaphore.release(producer);
        long start = System.currentTimeMillis();
        countDownLatch.await();
        long end = System.currentTimeMillis();
        double tps = cont * producer / (end - start) * 1000;
        System.out.println("--------------------tps---------------  " + tps);
    }


    public static class MyEventHandler implements EventHandler<DisruptorEvent> {
        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) throws Exception {

        }
    }

    @Getter
    @Setter
    public static class DisruptorEvent {

        private SampleData message;

    }
}
