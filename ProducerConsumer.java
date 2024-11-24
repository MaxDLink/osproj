import java.util.concurrent.Semaphore; // Import Semaphore to control access to shared resouces in the program 
import java.util.Random; // Import random to make random sleep times for producer and consumer threads 

public class ProducerConsumer { // Define the main class ProducerConsumer that contains all components of the
                                // simulation

    // Shared buffer and its size
    private static int[] buffer; // Declare the buffer
    private static int in = 0; // Producer's index in the buffer. Defined as explanatory in variable for easy
                               // reading
    private static int out = 0; // Consumer's index in the buffer. Defined as explanaotry out variable for easy
                                // reading
    private static final int RandMax = 100; // Maximum value for random items. Items will have values between 0 and 100
                                            // inclusive. The values will be 0 - 100 because RandMax is inclusive not
                                            // exclusive

    // Buffer size locked at 5 maximum
    private static final int BUFFER_SIZE = 5;

    // Semaphores for synchronization
    private static Semaphore empty; // Tracks empty slots in the buffer
    private static Semaphore full; // Tracks filled slots in the buffer
    private static final Semaphore mutex = new Semaphore(1); // Ensures mutual exclusion when accessing the buffer with
                                                             // a mutex semaphore

    // Additional semaphores to ensure only one producer or consumer produces or
    // consumes at a time
    private static final Semaphore producerTurn = new Semaphore(1); // Ensures only one producer produces at a time
                                                                    // May have >1 producer thread running at once.
    private static final Semaphore consumerTurn = new Semaphore(1); // Ensures only one consumer consumes at a time
                                                                    // May have >1 consumer thread running at once.

    // Function to insert an item into the buffer
    public static void insert_item(int item) {
        try {
            mutex.acquire(); // Lock the buffer. If another thread holds the mutex, the current thread waits
                             // until it can acquire the mutex. This ensures exclusive access to the buffer.
            buffer[in] = item; // Add the item to the buffer so producer is using in index value
            // Log the producer event including producer thread name, the integer produced,
            // & index where int was placed.
            System.out.println(Thread.currentThread().getName() + " produced: " + item + " at index " + in);
            // Updates the producers index in to the next index in the buffer_size without
            // going over special method with % cited with stack overflow.
            // % wraps over the buffer size to avoid going over
            in = (in + 1) % buffer.length; // Move to the next index circularly. Found from stack overflow link:
                                           // https://stackoverflow.com/questions/35680026/circular-buffer-in-java
        } catch (InterruptedException e) { // Interruption for graceful termination from Main function
            Thread.currentThread().interrupt();
        } finally { // always happens at the end
            mutex.release(); // Unlock the buffer
            full.release(); // Signal a filled slot because production has completed
        }
    }

    // Function to remove an item from the buffer
    public static void remove_item() {
        try {
            mutex.acquire(); // Lock the buffer for exclusive access
            int item = buffer[out]; // Remove the item from the buffer with out index
            // Log what was removed. Notify thread name, consumed item,
            // and which index it was consumed from
            System.out.println(Thread.currentThread().getName() + " consumed: " + item + " from index " + out);
            buffer[out] = 0; // Clear the buffer slot for clarity
            out = (out + 1) % buffer.length; // Move to the next index circularly without going over with % trick
                                             // Found from stack overflow link:
                                             // https://stackoverflow.com/questions/35680026/circular-buffer-in-java
        } catch (InterruptedException e) { // Graceful termination from main function
            Thread.currentThread().interrupt();
        } finally { // always will happen, consumption complete
            mutex.release(); // Unlock the buffer
            empty.release(); // Signal an empty slot
        }
    }

    // Producer thread
    static class Producer implements Runnable { // Producer thread is ran as a task
        private final Random random = new Random(); // Random number generator

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) { // Producer thread runs continuously until it is
                                                              // interrupted
                try {
                    producerTurn.acquire(); // Ensure only one producer produces at a time. Semaphore logic. If another
                                            // producer holds
                                            // the permit, then the current producer waits.
                    // Log that producer is waiting for the buffer to be empty
                    System.out.println(Thread.currentThread().getName() + " is waiting for empty buffer...");
                    // Empty semaphore to wait until buffer has empty slots
                    empty.acquire(BUFFER_SIZE); // Wait until buffer has BUFFER_SIZE empty slots
                    // Log which producer thread is producing
                    System.out.println(Thread.currentThread().getName() + " is producing...");
                    // Produce as many items as there is space in the empty buffer
                    for (int i = 0; i < BUFFER_SIZE; i++) { // Produce BUFFER_SIZE items
                        int item = random.nextInt(RandMax + 1); // Generate random item (0 to RandMax)
                        insert_item(item);
                        Thread.sleep(500); // Simulate production time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println(
                            // Log random sleep for producer
                            Thread.currentThread().getName() + " sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime); // Sleep producer thread for random amount of time
                } catch (InterruptedException e) { // Interrupt thread for graceful termination by main function
                    Thread.currentThread().interrupt();
                } finally { // always happens, release the producerTurn semaphore to allow other producers
                            // to produce
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
            while (!Thread.currentThread().isInterrupted()) { // Loops until interrupted
                try {
                    consumerTurn.acquire(); // Ensure only one consumer consumes at a time
                    System.out.println(Thread.currentThread().getName() + " is waiting for full buffer...");
                    full.acquire(BUFFER_SIZE); // Wait until buffer has BUFFER_SIZE full slots.
                                               // Only consume once slots are full
                    // Log that thread is consuming
                    System.out.println(Thread.currentThread().getName() + " is consuming...");
                    for (int i = 0; i < BUFFER_SIZE; i++) { // Consume BUFFER_SIZE items
                        remove_item();
                        Thread.sleep(500); // Simulate consumption time per item
                    }
                    // Sleep for a random amount of time between 1 to 3 seconds
                    int sleepTime = random.nextInt(2000) + 1000;
                    System.out.println(
                            Thread.currentThread().getName() + " sleeping for " + sleepTime + " milliseconds.");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) { // For graceful termination from main function
                    Thread.currentThread().interrupt();
                } finally { // always happens, allow other consumers be releasing consumerTurn semaphore
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
        // command line argument variables initialized to zero to start
        int sleepTime = 0;
        int numProducers = 0;
        int numConsumers = 0;

        try { // command line argument variables set to command line argument values
            sleepTime = Integer.parseInt(args[0]);
            numProducers = Integer.parseInt(args[1]);
            numConsumers = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) { // catch for invalid or no command line arguments
            System.out.println(
                    "Invalid arguments. Please enter integers for sleep time, number of producers, and number of consumers.");
            System.exit(1);
        }

        if (sleepTime <= 0 || numProducers <= 0 || numConsumers <= 0) { // another check for
                                                                        // incorrect command line arguments
            System.out.println("Arguments must be positive integers.");
            System.exit(1);
        }

        // Initialize buffer and semaphores defined on lines 21 - 23
        buffer = new int[BUFFER_SIZE]; // Fixed-size buffer
        empty = new Semaphore(BUFFER_SIZE); // Initially, buffer is empty
        full = new Semaphore(0); // No items in the buffer initially

        // Create and start producer threads
        // Producers created from command line argument value
        // Creates an array to hold references to producer threads
        Thread[] producerThreads = new Thread[numProducers];
        for (int i = 0; i < numProducers; i++) {
            Producer producer = new Producer(); // Creates a new producer object
            Thread producerThread = new Thread(producer); // Wraps the producer object in a new thread
            producerThread.setName("Producer-" + (i + 1)); // Assigns a name to producer for easy log identification
            producerThread.start(); // Start producer thread
            producerThreads[i] = producerThread; // Store the producer thread for later use in the array
                                                 // for easy termination etc.
        }

        // Create and start consumer threads. Same idea as above for producer threads
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
        } catch (InterruptedException e) { // If the main thread is interrupted during sleep, it handles the exception
            Thread.currentThread().interrupt();
        }

        // Interrupt threads for graceful termination
        for (Thread t : producerThreads) { // Interrupt producer threads
            t.interrupt();
        }
        for (Thread t : consumerThreads) { // Interrupt consumer threads
            t.interrupt();
        }

        System.out.println("Main thread has terminated the application."); // Alert user program is done
    }
}
