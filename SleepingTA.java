import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CountDownLatch; // Countdown latch makes student threads wait until producer thread is done producing all student threads 

public class SleepingTA {

    // Semaphores and shared state
    private static final Semaphore taSemaphore = new Semaphore(1); // Only one student can see the TA at a time
    private static final Semaphore chairs = new Semaphore(3); // Three chairs in the hallway
    private static final Queue<Integer> waitingStudents = new LinkedList<>(); // Queue for waiting students
    private static volatile int currentStudent = -1; // Tracks the student being helped (-1 means no one)
    private static volatile int totalStudentsHelped = 0; // Total number of students helped by the TA
    // CountDownLatch source:
    // https://stackoverflow.com/questions/4691533/java-wait-for-thread-to-finish
    private static final CountDownLatch startSignal = new CountDownLatch(1);

    // TA thread
    static class TA implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (waitingStudents) {
                        if (waitingStudents.isEmpty()) {
                            System.out.println("TA is sleeping...");
                            waitingStudents.wait(); // Sleep until notified by a student
                        }
                    }

                    // Acquire TA's own semaphore before helping a student
                    taSemaphore.acquire();

                    synchronized (waitingStudents) {
                        currentStudent = waitingStudents.poll(); // Get the next student in line
                        if (currentStudent != -1) {
                            chairs.release(); // Release the chair
                            System.out.println("TA is helping Student " + currentStudent + "...");
                            waitingStudents.notifyAll(); // Notify all students
                        } else {
                            // No student to help
                            taSemaphore.release();
                            continue;
                        }
                    }

                    // Simulate helping time (you can randomize this)
                    Thread.sleep(2000);

                    synchronized (waitingStudents) {
                        System.out.println("TA finished helping Student " + currentStudent + ".");
                        currentStudent = -1; // Reset after helping
                        totalStudentsHelped++; // Increment totalStudentsHelped
                        waitingStudents.notifyAll(); // Notify all students
                    }

                    // Release TA's semaphore
                    taSemaphore.release();

                } catch (InterruptedException e) {
                    System.out.println("TA was interrupted. Exiting...");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Student thread
    static class Student implements Runnable {
        private final int studentId;
        private int waitTime; // Random wait time assigned to each student

        public Student(int studentId, int waitTime) {
            this.studentId = studentId;
            this.waitTime = waitTime;
        }

        @Override
        public void run() {
            try {
                // Wait until the producer has finished creating all students
                startSignal.await(); // Each student thread waits until the latch value 1 is counted down by the
                                     // producer

                while (true) {
                    synchronized (waitingStudents) {
                        if (studentId == currentStudent) {
                            // Student is being helped, skip working logic
                            waitingStudents.wait();
                        }
                    }

                    System.out.println("Student " + studentId + " is working on assignments...");
                    Thread.sleep((int) (Math.random() * 3000) + 2000); // Simulate working time

                    System.out.println("Student " + studentId + " wants to see the TA...");
                    if (chairs.tryAcquire()) { // Try to get a chair in the hallway
                        synchronized (waitingStudents) {
                            waitingStudents.add(studentId);
                            System.out.println("Student " + studentId + " is waiting in the hallway with patience "
                                    + waitTime + " ms");
                            waitingStudents.notify(); // Wake up the TA if they're sleeping
                        }

                        synchronized (waitingStudents) {
                            long startTime = System.currentTimeMillis();
                            long remainingWaitTime = waitTime;
                            while (studentId != currentStudent && remainingWaitTime > 0) {
                                waitingStudents.wait(remainingWaitTime);
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                remainingWaitTime = waitTime - elapsedTime;
                            }

                            if (studentId == currentStudent) {
                                // Student is being helped
                                System.out.println("Student " + studentId + " is being helped by the TA...");
                                // Wait until TA finishes helping
                                waitingStudents.wait();
                                // Exit the loop after being helped
                                break;
                            } else {
                                // Timeout occurred
                                System.out.println(
                                        "Student " + studentId + " got tired of waiting and will try again later.");
                                waitingStudents.remove(Integer.valueOf(studentId));
                                chairs.release();
                                // Go back to working on assignments and try again later
                            }
                        }

                    } else {
                        System.out.println(
                                "Student " + studentId + " found no available chairs and will try again later.");
                        // Go back to working on assignments and try again later
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Student " + studentId + " was interrupted. Exiting...");
                Thread.currentThread().interrupt();
            }
        }
    }

    // Producer thread that creates and starts n student threads
    static class Producer implements Runnable {
        private int n; // Number of students to create
        private List<Thread> studentThreads;

        public Producer(int n, List<Thread> studentThreads) {
            this.n = n;
            this.studentThreads = studentThreads;
        }

        @Override
        public void run() {
            Random random = new Random(); // Random number generator

            for (int i = 1; i <= n; i++) {
                // Generate a random wait time between 1000ms and 3000ms
                int waitTime = random.nextInt(2000) + 1000;

                Thread studentThread = new Thread(new Student(i, waitTime), "Student-" + i);
                studentThread.start();
                studentThreads.add(studentThread); // Add student thread to the list
                System.out.println("Producer created and started Student " + i);
                try {
                    Thread.sleep(500); // Optional: Simulate time between creating students
                } catch (InterruptedException e) {
                    System.out.println("Producer was interrupted.");
                    Thread.currentThread().interrupt();
                    break; // Exit if interrupted
                }
            }
            System.out.println("Producer has created all " + n + " students.");

            // Release the latch to allow student threads to proceed
            startSignal.countDown();
        }
    }

    public static void main(String[] args) {

        // Number of students
        int n = 10; // Reduced for demonstration purposes

        // Create and start the TA thread
        Thread taThread = new Thread(new TA(), "TA");
        taThread.start();

        List<Thread> studentThreads = new ArrayList<>();

        // Create and start the producer thread
        Producer producer = new Producer(n, studentThreads);
        Thread producerThread = new Thread(producer, "Producer");
        producerThread.start();

        try {
            producerThread.join(); // Waits for the producer thread to finish before continuing with main thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("All Student threads have been created.");

        // Wait until all students have been helped
        while (totalStudentsHelped < n) { // Main will wait until all students have been helped
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Interrupt all threads for graceful termination
        System.out.println("Students helped: " + totalStudentsHelped);
        System.out.println("Main thread interrupting all threads...");
        // Interrupt TA thread
        taThread.interrupt();
        // Interrupt all student threads for graceful termination
        System.out.println("Main thread interrupting all student threads...");
        for (Thread studentThread : studentThreads) {
            studentThread.interrupt();
        }

    }

}
