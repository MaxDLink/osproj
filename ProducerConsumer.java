import java.util.concurrent.Semaphore;
import java.util.Random;

public class ProducerConsumer {

    // Shared buffer and its size
    private static int[] buffer;
    private static int in = 0; // Producer's index in the buffer
    private static int out = 0; // Consumer's index in the buffer
    private static final int RandMax = 100; // Maximum value for random items

    // Semaphores for synchronization
    private static final Semaphore empty = new Semaphore(5); // Tracks empty slots in the buffer
    private static final Semaphore full = new Semaphore(0); // Tracks filled slots in the buffer
    private static final Semaphore mutex = new Semaphore(1); // Ensures mutual exclusion when accessing the buffer

    // Function to insert an item into the buffer
    public static void insert_item(int item) {
        try {
            mutex.acquire(); // Lock the buffer
            buffer[in] = item; // Add the item to the buffer
            System.out.println("Producer produced: " + item + " at index " + in);
            in = (in + 1) % buffer.length; // Move to the next index circularly
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release(); // Unlock the buffer
            full.release(); // Signal a filled slot
        }
    }

    // Function to remove an item from the buffer
    public static void remove_item() {
        try {
            mutex.acquire(); // Lock the buffer
            int item = buffer[out]; // Remove the item from the buffer
            System.out.println("Consumer consumed: " + item + " from index " + out);
            buffer[out] = 0; // Clear the slot (optional for clarity)
            out = (out + 1) % buffer.length; // Move to the next index circularly
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release(); // Unlock the buffer
            empty.release(); // Signal an empty slot
        }
    }

    // Producer thread
    static class Producer implements Runnable {
        private final Random random = new Random(); // Random number generator

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("Producer is waiting for empty buffer...");
                    empty.acquire(5); // Wait until buffer has 5 empty slots
                    System.out.println("Producer is running...");
                    for (int i = 0; i < 5; i++) { // Produce 5 items
                        int item = random.nextInt(RandMax + 1); // Generate random item (0 to RandMax)
                        insert_item(item);
                        Thread.sleep(500); // Simulate production time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println("Producer sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime);
                    // The producer will block here on the next empty.acquire(5) if the buffer isn't
                    // empty
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Consumer thread
    static class Consumer implements Runnable {
        private final Random random = new Random(); // Random number generator

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("Consumer is waiting for full buffer...");
                    full.acquire(5); // Wait until buffer has 5 full slots
                    System.out.println("Consumer is running...");
                    for (int i = 0; i < 5; i++) { // Consume 5 items
                        remove_item();
                        Thread.sleep(500); // Simulate consumption time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println("Consumer sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime);
                    // The consumer will block here on the next full.acquire(5) if the buffer isn't
                    // full
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
