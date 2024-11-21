import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class ta {

    // Semaphores and shared state
    private static final Semaphore taSemaphore = new Semaphore(1); // Only one student can see the TA at a time
    private static final Semaphore chairs = new Semaphore(3); // Three chairs in the hallway
    private static final Queue<Integer> waitingStudents = new LinkedList<>(); // Queue for waiting students
    private static volatile int currentStudent = -1; // Tracks the student being helped (-1 means no one)

    // TA thread
    static class TA implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // Wait for a student to wake the TA
                    synchronized (waitingStudents) {
                        if (waitingStudents.isEmpty()) {
                            System.out.println("TA is sleeping...");
                            waitingStudents.wait(); // Sleep until notified by a student
                        }
                    }

                    // Help the next student
                    taSemaphore.acquire();
                    synchronized (waitingStudents) {
                        currentStudent = waitingStudents.poll(); // Get the next student in line
                        System.out.println("TA is helping Student " + currentStudent + "...");
                    }
                    Thread.sleep(2000); // Simulate helping time
                    System.out.println("TA finished helping Student " + currentStudent + ".");
                    currentStudent = -1; // Reset after helping
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

        public Student(int studentId) {
            this.studentId = studentId;
        }

        @Override
        public void run() {
            try {
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
                            System.out.println("Student " + studentId + " is waiting in the hallway...");
                            waitingStudents.notify(); // Wake up the TA if they're sleeping
                        }

                        taSemaphore.acquire(); // Wait for TA's availability
                        System.out.println("Student " + studentId + " is being helped by the TA...");
                        taSemaphore.release();

                    } else {
                        System.out.println("Student " + studentId + " found no available chairs and left.");
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Student " + studentId + " was interrupted. Exiting...");
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {

        // Create and start the TA thread
        Thread taThread = new Thread(new TA());
        taThread.start();

        // Create and start multiple student threads
        Thread student1 = new Thread(new Student(1));
        Thread student2 = new Thread(new Student(2));
        Thread student3 = new Thread(new Student(3));
        Thread student4 = new Thread(new Student(4));
        Thread student5 = new Thread(new Student(5));

        student1.start();
        student2.start();
        student3.start();
        student4.start();
        student5.start();

        // Main thread sleeps for 20 seconds
        try {
            Thread.sleep(20000); // Allow threads to run for 20 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Interrupt all threads for graceful termination
        System.out.println("Main thread interrupting all threads...");
        taThread.interrupt();
        student1.interrupt();
        student2.interrupt();
        student3.interrupt();
        student4.interrupt();
        student5.interrupt();

        System.out.println("Program terminated.");
    }
}
