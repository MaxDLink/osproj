import java.util.concurrent.Semaphore;

public class SimpleProducerConsumer {

    // Shared buffer and its size
    private static int[] buffer;
    private static int in = 0; // Producer's index in the buffer
    private static int out = 0; // Consumer's index in the buffer

    // Semaphores for synchronization
    private static final Semaphore producerTurn = new Semaphore(1); // Start with producer's turn
    private static final Semaphore consumerTurn = new Semaphore(0); // Consumer waits for producer to finish

    // Function to insert an item into the buffer
    public static void insert_item(int item) {
        if (in < buffer.length) {
            buffer[in] = item; // Add the item to the buffer
            System.out.println("Producer produced: " + item + " at index " + in);
            in = (in + 1) % buffer.length; // Move to the next index circularly
        }
    }

    // Function to remove an item from the buffer
    public static void remove_item() {
        if (out < buffer.length && buffer[out] != 0) {
            System.out.println("Consumer consumed: " + buffer[out] + " from index " + out);
            buffer[out] = 0; // Clear the slot
            out = (out + 1) % buffer.length; // Move to the next index circularly
        }
    }

    // Producer thread
    static class Producer implements Runnable {
        @Override
        public void run() {
            int item = 0; // Item to produce
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    producerTurn.acquire(); // Wait for the producer's turn
                    System.out.println("Producer is running...");
                    for (int i = 0; i < 5; i++) { // Produce 5 items
                        item++;
                        insert_item(item);
                        Thread.sleep(500); // Simulate production time per item
                    }
                    System.out.println("Producer is sleeping...");
                    Thread.sleep(2000); // Sleep before handing control to the consumer
                    consumerTurn.release(); // Signal the consumer to run
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Consumer thread
    static class Consumer implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    consumerTurn.acquire(); // Wait for the consumer's turn
                    System.out.println("Consumer is running...");
                    for (int i = 0; i < 5; i++) { // Consume 5 items
                        remove_item();
                        Thread.sleep(500); // Simulate consumption time per item
                    }
                    System.out.println("Consumer is sleeping...");
                    Thread.sleep(2000); // Sleep before handing control to the producer
                    producerTurn.release(); // Signal the producer to run
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void main(String[] args) {

        // Initialize buffer in the main function
        buffer = new int[5]; // Fixed-size buffer of size 5

        // Create and start the producer thread
        Thread producerThread = new Thread(new Producer());
        producerThread.start();

        // Create and start the consumer thread
        Thread consumerThread = new Thread(new Consumer());
        consumerThread.start();

        // Main thread sleeps for 20 seconds
        try {
            Thread.sleep(20000); // Let the program run for 20 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Interrupt threads for graceful termination
        producerThread.interrupt();
        consumerThread.interrupt();

        System.out.println("Both producer and consumer have completed their tasks.");
    }
}
