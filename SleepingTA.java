import java.util.ArrayList; // This import lets us make an array to keep track of & easily terminate our student threads 
import java.util.LinkedList; // This import lets us make our queue of type linked list 
import java.util.List; // This import lets us make a list of student threads in our producer thread 
import java.util.Queue; // This import lets us make a queue of type linked list so that we can move students in & out of the hallway 
import java.util.Random; // This import lets us make random numbers for work times and wait times for each student 
import java.util.concurrent.Semaphore; // This import lets us bring in semaphores to control access to shared resources among threads 
import java.util.concurrent.CountDownLatch; // Countdown latch makes student threads wait until producer thread is done producing all student threads 

public class SleepingTA { // main class to encapsulate all components of the simulation

    // Semaphores and shared state
    private static final Semaphore taSemaphore = new Semaphore(1); // Only one student can see the TA at a time so mark
    // TODO - is 1 only 1 resource? etc? // this semaphore with a 1 resource
    private static final Semaphore chairs = new Semaphore(3); // Three chairs in the hallway so this semaphore gets 3
    // TODO - why is this integer typed? // resources
    private static final Queue<Integer> waitingStudents = new LinkedList<>(); // Queue for waiting students of type
                                                                              // linked list & integer type for student
                                                                              // ID storage
    private static volatile int currentStudent = -1; // Tracks the student being helped (-1 means no one). Basic flag
                                                     // for who needs help
    private static volatile int totalStudentsHelped = 0; // Total number of students helped by the TA. Increments until
                                                         // equal to n students & then program terminates
    // CountDownLatch source:
    // https://stackoverflow.com/questions/4691533/java-wait-for-thread-to-finish
    private static final CountDownLatch startSignal = new CountDownLatch(1); // Countdown latch makes student threads
                                                                             // wait until producer thread is done
                                                                             // making student threads

    // TA thread
    static class TA implements Runnable { // Defines the TA as a runnable task to be executed by a thread
        @Override
        public void run() { // run method
            while (true) { // Main TA behavior encapsulated in an infinite loop
                try {
                    synchronized (waitingStudents) { // Exclusive access to waiting students queue. Only TA can access.
                                                     // Thread safe access to shared resources.
                                                     // waitingStudentsQueue
                        if (waitingStudents.isEmpty()) { // If the queue is empty then
                            System.out.println("TA is sleeping..."); // TA is sleeping because there are no students to
                                                                     // help
                            waitingStudents.wait(); // TA sleeps until notified by a student
                        }
                    }

                    // Acquire TA's own semaphore before helping a student. This ensures that the TA
                    // is not helping another student
                    // TA self checks availability
                    taSemaphore.acquire();

                    synchronized (waitingStudents) { // TA has exclusive access to waiting students queue. Thread safe
                                                     // access to shared resources.
                        currentStudent = waitingStudents.poll(); // Get the next student in line. Retrieves & removes
                                                                 // the head of waitingStudents queue. Assigns student
                                                                 // ID to currentStudent.
                        if (currentStudent != -1) { // currentStudent set with poll so it is not -1, someone needs help
                            chairs.release(); // if we are in this if statement, then we know a student is being helped,
                                              // so release a chair so that another student can take a seat.
                            System.out.println("TA is helping Student " + currentStudent + "...");
                            waitingStudents.notifyAll(); // Notify all students. This wakes up any student threads
                                                         // waiting on waitingStudent queue
                                                         // TODO - update the head of the queue???
                        } else { // currentStudent must be -1
                            // No student to help, release the semaphore to allow another student to be
                            // helped & continue with the loop
                            taSemaphore.release(); // TODO - why is the semaphore released here? What does releasing the
                                                   // semaphore do?
                            continue; // TODO - proceed with next loop iteration?
                        }
                    }

                    // Simulate time TA helps a student
                    Thread.sleep(2000);

                    synchronized (waitingStudents) { // TA has thread safe access to waitingStudents queue with
                                                     // synchronized
                        System.out.println("TA finished helping Student " + currentStudent + ".");
                        currentStudent = -1; // Reset flag after helping the currentStudent
                        totalStudentsHelped++; // Increment totalStudentsHelped so that program eventually terminates
                                               // when all students are helped
                        waitingStudents.notifyAll(); // Notify all students //TODO - update the head of the queue?
                    }

                    // Release TA's semaphore to allow another student to be helped
                    taSemaphore.release();

                } catch (InterruptedException e) { // if the TA thread is interrupted (E.G. Main terminates threads)
                                                   // then this handles the exception
                    System.out.println("TA was interrupted. Exiting...");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Student thread
    static class Student implements Runnable { // Student is a task to be executed by a thread
        private final int studentId; // students given an ID
        private int waitTime; // Random wait time assigned to each student. This is the maximum time the
                              // student is willing to wait for the TA before leaving & trying again later.

        public Student(int studentId, int waitTime) { // constructor to initialize student ID and waitTime
            this.studentId = studentId;
            this.waitTime = waitTime;
        }

        @Override
        public void run() {
            try {
                // Wait until the producer has finished creating all students
                startSignal.await(); // Each student thread waits until the latch value 1 is counted down by the
                                     // producer class

                while (true) { // student thread's behavior is encapsulated in a while loop
                    synchronized (waitingStudents) { // thread safe access to shared resource waitingStudents queue
                        if (studentId == currentStudent) { // if the current student is the student being helped then
                                                           // they wait on waitingStudents
                            // Student is being helped, skip working logic
                            waitingStudents.wait(); // TODO - what does this line do? Makes the queue wait? So the head
                                                    // does not update?
                        }
                    }
                    // If the student is not the current student being helped, then they are working
                    // on assignments
                    System.out.println("Student " + studentId + " is working on assignments...");
                    Thread.sleep((int) (Math.random() * 3000) + 2000); // Simulate working time with random numbers

                    // Student wants to see the TA
                    System.out.println("Student " + studentId + " wants to see the TA...");
                    if (chairs.tryAcquire()) { // Try to get a chair in the hallway: returns true if available, false
                                               // otherwise
                        synchronized (waitingStudents) { // Thread safe access to the waitingStudents queue
                            waitingStudents.add(studentId); // Add student's ID to queue because student found a chair
                            System.out.println("Student " + studentId + " is waiting in the hallway with patience "
                                    + waitTime + " ms"); // How long the student will wait in the halllway
                            waitingStudents.notify(); // Wake up the TA if they're sleeping //TODO - communicates with
                                                      // line 41?
                        }

                        synchronized (waitingStudents) { // Thread safe access to waitingStudents queue
                            long startTime = System.currentTimeMillis(); // Records the current system time to calculate
                                                                         // the waiting duration in milliseconds
                            long remainingWaitTime = waitTime; // Log the waitTime as remaining time
                            while (studentId != currentStudent && remainingWaitTime > 0) { // While the student is not
                                                                                           // being helped, they have a
                                                                                           // wait time. While the wait
                                                                                           // time is > 0 they will wait
                                waitingStudents.wait(remainingWaitTime); // Waits up to remaining wait time
                                long elapsedTime = System.currentTimeMillis() - startTime; // Grab time that has passed
                                remainingWaitTime = waitTime - elapsedTime; // subtract time passed from wait time to
                                                                            // get remaining wait time. This process
                                                                            // loops until student runs out of patience.
                            }

                            if (studentId == currentStudent) { // TODO - TA grabs student to help them because queue
                                                               // head has cycled to this student?
                                // Student is being helped
                                System.out.println("Student " + studentId + " is being helped by the TA...");
                                // Wait until TA finishes helping
                                waitingStudents.wait(); // TODO - waits until head of queue changes? How does this work?
                                // Exit the loop after being helped
                                break;
                            } else { // Student was not helped soon enough and left
                                // Timeout occurred
                                System.out.println(
                                        "Student " + studentId + " got tired of waiting and will try again later.");
                                waitingStudents.remove(Integer.valueOf(studentId)); // Remove this student id from the
                                                                                    // queue because he is not in a
                                                                                    // chair anymore
                                chairs.release(); // release their chair because they left
                                // Go back to working on assignments and try again later
                            }
                        }

                    } else { // This is the case where all chairs are full & the student cannot wait
                        System.out.println(
                                "Student " + studentId + " found no available chairs and will try again later.");
                        // Go back to working on assignments and try again later
                    }
                }
            } catch (InterruptedException e) { // This is for graceful termination when main function cleans up all
                                               // threads
                System.out.println("Student " + studentId + " was interrupted. Exiting...");
                Thread.currentThread().interrupt();
            }
        }
    }

    // Producer thread that creates and starts n student threads
    static class Producer implements Runnable { // producer is ran as a thread
        private int n; // Number of students to create
        private List<Thread> studentThreads; // List of student threads to easily produce & clean up student threads
                                             // later in main function // TODO - this list is filled out from main
                                             // passing its student thread list to producer class? Filled out in
                                             // producer constructor?

        public Producer(int n, List<Thread> studentThreads) { // producer constructor to initialize n and studentThreads
            this.n = n;
            this.studentThreads = studentThreads;
        }

        @Override
        public void run() {
            Random random = new Random(); // Random number generator

            for (int i = 1; i <= n; i++) { // loops from 1 to n to create & start each student thread

                // Generate a random wait time between 1000ms and 3000ms
                int waitTime = random.nextInt(2000) + 1000; // TODO - lower & upper bound here? What are these?

                Thread studentThread = new Thread(new Student(i, waitTime), "Student-" + i); // This creates the student
                                                                                             // thread with the wait
                                                                                             // time and ID
                studentThread.start(); // Starts the student Thread immediately. The student threads are told to hold
                                       // with CountDownLatch so they wait until producer is done. This simulates them
                                       // starting out of order which is more realistic.
                studentThreads.add(studentThread); // Add student thread to the list
                System.out.println("Producer created and started Student " + i);
                try {
                    Thread.sleep(500); // Simulate time between creating students
                } catch (InterruptedException e) { // exception for graceful termination
                    System.out.println("Producer was interrupted.");
                    Thread.currentThread().interrupt();
                    break; // Exit if interrupted
                }
            }
            System.out.println("Producer has created all " + n + " students."); // Producer is done creating students

            // Release the latch to allow student threads to proceed. They will proceed out
            // of order but this is okay.
            startSignal.countDown();
        }
    }

    public static void main(String[] args) {

        // Number of students
        int n = 10; // Reduced for demonstration purposes

        // Create and start the TA thread
        Thread taThread = new Thread(new TA(), "TA");
        taThread.start();
        // initailizes a list to keep track of student threads created by producer
        // thread for easy thread termination later
        List<Thread> studentThreads = new ArrayList<>();

        // Create and start the producer thread
        Producer producer = new Producer(n, studentThreads); // Creates a producer object
        Thread producerThread = new Thread(producer, "Producer"); // Wraps producer object in a new thread // TODO - why
                                                                  // do we wrap an object in a thread instead of just
                                                                  // starting a thread?
        producerThread.start(); // Start the producer thread

        try { // Join synces producer thread & main thread, so main waits for producer to be
              // finished before continuing
            producerThread.join(); // Waits for the producer thread to finish before continuing with main thread
        } catch (InterruptedException e) { // Interrupt for graceful termination if main quits producer thread.
            Thread.currentThread().interrupt();
        }

        System.out.println("All Student threads have been created.");

        // Wait until all students have been helped
        while (totalStudentsHelped < n) { // Main will wait until all students have been helped by the TA
            try { // Main sleeps for 1 second between checks to avoid busy waiting // TODO - isnt
                  // this busy waiting?
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) { // If main thread is interrupted during sleep
                Thread.currentThread().interrupt(); // main handles the exception
                break; // main exits the while loop
            }
        }

        // Interrupt all threads for graceful termination
        System.out.println("Students helped: " + totalStudentsHelped); // Have helped all students so it is time to
                                                                       // interrupt TA & student threads
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
