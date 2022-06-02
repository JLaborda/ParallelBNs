package consensusBN.circularFusion;


public class Convergence {
    
    public static final Object convergence = new Object();
    private static boolean hasConverged;

    public synchronized void converged() {
        synchronized(convergence) {
            hasConverged = true;
            convergence.notifyAll();
        }
    }

    public void waitConverge() {
        synchronized(convergence) {
            while (!hasConverged) {
                try {
                    convergence.wait();
                } catch (InterruptedException ex) {}
            }
        }
    }
} 