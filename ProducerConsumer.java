import java.util.concurrent.Semaphore;
import java.util.Random;

public class ProducerConsumer {

    // Shared buffer and its size
    private static int[] buffer;
    private static int in = 0; // Producer's index in the buffer
    private static int out = 0; // Consumer's index in the buffer
    private static final int RandMax = 100; // Maximum value for random items

    // Buffer size
    private static final int BUFFER_SIZE = 5;

    // Semaphores for synchronization
    private static Semaphore empty; // Tracks empty slots in the buffer
    private static Semaphore full; // Tracks filled slots in the buffer
    private static final Semaphore mutex = new Semaphore(1); // Ensures mutual exclusion when accessing the buffer

    // Additional semaphores to ensure only one producer or consumer produces or consumes at a time
    private static final Semaphore producerTurn = new Semaphore(1); // Ensures only one producer produces at a time
    private static final Semaphore consumerTurn = new Semaphore(1); // Ensures only one consumer consumes at a time

    // Function to insert an item into the buffer
    public static void insert_item(int item) {
        try {
            mutex.acquire(); // Lock the buffer
            buffer[in] = item; // Add the item to the buffer
            System.out.println(Thread.currentThread().getName() + " produced: " + item + " at index " + in);
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
            System.out.println(Thread.currentThread().getName() + " consumed: " + item + " from index " + out);
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
                    producerTurn.acquire(); // Ensure only one producer produces at a time
                    System.out.println(Thread.currentThread().getName() + " is waiting for empty buffer...");
                    empty.acquire(BUFFER_SIZE); // Wait until buffer has BUFFER_SIZE empty slots
                    System.out.println(Thread.currentThread().getName() + " is producing...");
                    for (int i = 0; i < BUFFER_SIZE; i++) { // Produce BUFFER_SIZE items
                        int item = random.nextInt(RandMax + 1); // Generate random item (0 to RandMax)
                        insert_item(item);
                        Thread.sleep(500); // Simulate production time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println(Thread.currentThread().getName() + " sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producerTurn.release(); // Allow other producers to produce
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
                    consumerTurn.acquire(); // Ensure only one consumer consumes at a time
                    System.out.println(Thread.currentThread().getName() + " is waiting for full buffer...");
                    full.acquire(BUFFER_SIZE); // Wait until buffer has BUFFER_SIZE full slots
                    System.out.println(Thread.currentThread().getName() + " is consuming...");
                    for (int i = 0; i < BUFFER_SIZE; i++) { // Consume BUFFER_SIZE items
                        remove_item();
                        Thread.sleep(500); // Simulate consumption time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println(Thread.currentThread().getName() + " sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumerTurn.release(); // Allow other consumers to consume
                }
            }
        }
    }

    public static void main(String[] args) {

        // Validate and parse command-line arguments
        if (args.length != 3) {
            System.out.println("Usage: java ProducerConsumer <sleep_time> <num_producers> <num_consumers>");
            System.exit(1);
        }

        int sleepTime = 0;
        int numProducers = 0;
        int numConsumers = 0;

        try {
            sleepTime = Integer.parseInt(args[0]);
            numProducers = Integer.parseInt(args[1]);
            numConsumers = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments. Please enter integers for sleep time, number of producers, and number of consumers.");
            System.exit(1);
        }

        if (sleepTime <= 0 || numProducers <= 0 || numConsumers <= 0) {
            System.out.println("Arguments must be positive integers.");
            System.exit(1);
        }

        // Initialize buffer and semaphores
        buffer = new int[BUFFER_SIZE]; // Fixed-size buffer
        empty = new Semaphore(BUFFER_SIZE); // Initially, buffer is empty
        full = new Semaphore(0); // No items in the buffer initially

        // Create and start producer threads
        Thread[] producerThreads = new Thread[numProducers];
        for (int i = 0; i < numProducers; i++) {
            Producer producer = new Producer();
            Thread producerThread = new Thread(producer);
            producerThread.setName("Producer-" + (i + 1));
            producerThread.start();
            producerThreads[i] = producerThread;
        }

        // Create and start consumer threads
        Thread[] consumerThreads = new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            Consumer consumer = new Consumer();
            Thread consumerThread = new Thread(consumer);
            consumerThread.setName("Consumer-" + (i + 1));
            consumerThread.start();
            consumerThreads[i] = consumerThread;
        }

        // Main thread sleeps for the specified time
        try {
            Thread.sleep(sleepTime * 1000); // Convert seconds to milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Interrupt threads for graceful termination
        for (Thread t : producerThreads) {
            t.interrupt();
        }
        for (Thread t : consumerThreads) {
            t.interrupt();
        }

        System.out.println("Main thread has terminated the application.");
    }
}
