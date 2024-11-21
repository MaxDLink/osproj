public class SimpleProducerConsumer {

    // Producer thread
    static class Producer implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) { // while lets thread run continously & checks for
                                                              // interrupt from main
                System.out.println("Producer is running...");
                System.out.println("Producer produced an item!");
            }
        }
    }

    // Consumer thread
    static class Consumer implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Consumer is running...");
                System.out.println("Consumer consumed an item!");
            }
        }
    }

    public static void main(String[] args) {

        // initialize buffer
        // fixed size buffer of size 5
        int[] buffer = new int[5];

        // Create and start the producer thread
        Thread producerThread = new Thread(new Producer()); // makes producer
        producerThread.start();

        // Create and start the consumer thread
        Thread consumerThread = new Thread(new Consumer()); // makes consumer
        consumerThread.start();

        // Wait for threads to finish execution
        try { // joins make main wait until threads done, but we are replacing with sleep
              // producerThread.join();
              // consumerThread.join();
            Thread.sleep(5000); // main function is sleeping for 5000 milliseconds
        } catch (InterruptedException e) { // if the join does not work, do interrupt
            Thread.currentThread().interrupt();
        }

        // interrupt
        producerThread.interrupt();
        consumerThread.interrupt();

        System.out.println("Both producer and consumer have completed their tasks."); // alert that threads are done
    }
}
