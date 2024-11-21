public class ta {

    // TA thread
    static class TA implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) { // Allow TA to run continuously
                try {
                    System.out.println("TA is sleeping...");
                    Thread.sleep(5000); // TA sleeps for 5 seconds
                } catch (InterruptedException e) {
                    System.out.println("TA was interrupted. Exiting...");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void main(String[] args) {

        // Create and start the TA thread
        Thread taThread = new Thread(new TA());
        taThread.start();

        // Main thread sleeps for 15 seconds before interrupting the TA
        try {
            Thread.sleep(15000); // Allow the TA thread to sleep a few cycles
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Interrupt the TA thread for graceful termination
        System.out.println("Main thread interrupting the TA...");
        taThread.interrupt();

        System.out.println("Program terminated.");
    }
}
