package gov.lanl.cnls.linkedprocess.xmpp.lopfarm;

import gov.lanl.cnls.linkedprocess.LinkedProcess;
import org.apache.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * An object with an internal thread which is capable of executing scripts
 * (via ScriptEngine) within that thread, pausing them if they take longer
 * to execute than a given time slice, then resuming execution at a later
 * time.
 * Note: relies on the deprecated methods Thread.suspend and Thread.resume.
 * Deadlocks are avoided by preventing the internal thread from obtaining a
 * lock on any object apart from two special monitors, neither of which can
 * be locked at the time the thread is suspended.
 * <p/>
 * Author: josh
 * Date: Jun 24, 2009
 * Time: 2:15:41 PM
 */
public class VMWorker {
    private enum State {
        ACTIVE_INPROGRESS,
        ACTIVE_SUSPENDED,
        IDLE_WAITING,
        IDLE_FINISHED,
        TERMINATED
    }

    private static final Logger LOGGER = LinkedProcess.getLogger(VMWorker.class);

    private final LoPQueue<Job> jobQueue;
    private final VMScheduler.VMResultHandler resultHandler;
    private final ScriptEngine scriptEngine;
    private final Thread workerThread;
    private State state;

    private Job latestJob;
    private JobResult latestResult;

    private final Object
            timeoutMonitor = "",
            workerWaitMonitor = "";

    /**
     * Creates a new virtual machine worker.
     *
     * @param scriptEngine  the ScriptEngine with which to evaluate expressions
     * @param resultHandler a handler for job results
     */
    public VMWorker(final ScriptEngine scriptEngine,
                    final VMScheduler.VMResultHandler resultHandler) {
        this.scriptEngine = scriptEngine;
        this.resultHandler = resultHandler;

        int capacity = new Integer(LinkedProcess.getProperties().getProperty(
                LinkedProcess.MESSAGE_QUEUE_CAPACITY));
        jobQueue = new LoPQueue<Job>(capacity);

        workerThread = new Thread(new WorkerRunnable());
        workerThread.start();

        state = State.IDLE_WAITING;
    }

    /**
     * @return whether the worker currently has work to do (either a suspended
     *         job in progress, or pending jobs in the queue)
     */
    public synchronized boolean canWork() {
        switch (state) {
            case ACTIVE_SUSPENDED:
                // Still working on the last job.
                return true;
            case IDLE_WAITING:
                // Are there any pending jobs?
                return 0 != jobQueue.size();
            default:
                throw new IllegalArgumentException("can't check for new work in state: " + state);
        }
    }

    /**
     * Adds a job to the queue.
     * Note: this alone does not cause the worker to become active.
     *
     * @param job the job to add
     */
    public synchronized void addJob(final Job job) {
        switch (state) {
            case ACTIVE_SUSPENDED:
            case IDLE_WAITING:
                jobQueue.offer(job);
                break;
            default:
                throw new IllegalStateException("can't add jobs in state: " + state);
        }
    }

    /**
     * Works on the current job for at most a given window of time.  If the job
     * is finished during this time, its result will be handled.  Otherwise, the
     * job will be suspended, to be resumed on a subsequent call to work().
     * Note: This method should only be called when the value of canWork() is true.
     *
     * @param timeout the length of the time window
     */
    public synchronized void work(final long timeout) {
        switch (state) {
            case ACTIVE_SUSPENDED:
                state = State.ACTIVE_INPROGRESS;
                resumeWorkerThread();
                break;
            case IDLE_WAITING:
                if (0 == jobQueue.size()) {
                    throw new IllegalStateException("no jobs available. Call canWork() to avoid this condition.");
                }
                latestJob = jobQueue.poll();

                state = State.ACTIVE_INPROGRESS;
                notifyWorkerThread();
                break;
            default:
                throw new IllegalStateException("can't begin new work in state: " + state);
        }

        // Break out when the time slice has expired or the monitor has been notified.
        try {
            synchronized (timeoutMonitor) {
                // Check whether the job has already completed since it was
                // started or resumed above.  There is still a very slight
                // chance of a race condition in which which the thread finishes
                // and is then forced to wait.  The only consequence of this
                // would be a wasted execution window.
                if (State.ACTIVE_INPROGRESS == state) {
                    timeoutMonitor.wait(timeout);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("interrupted unexpectedly");
            System.exit(1);
        }

        // Suspend the thread immediately, regardless of what state we're in.
        suspendWorkerThread();

        switch (state) {
            case ACTIVE_INPROGRESS:
                state = State.ACTIVE_SUSPENDED;
                break;
            case IDLE_FINISHED:
                resultHandler.handleResult(latestResult);

                // Advance to the wait()
                state = State.IDLE_WAITING;
                resumeWorkerThread();
                break;
            default:
                throw new IllegalStateException("state should not occur at the end of a work window: " + state);
        }
    }

    /**
     * Terminates execution of the current job and prevents further jobs.
     * This method may be called at any time, in any thread.  If it is called
     * while a job is being executed, the job will continue executing for the
     * remainder of the window, but it will not complete normally unless it
     * does so within that window.  Nor will additional jobs be processed.
     */
    public synchronized void cancel() {
        switch (state) {
            case ACTIVE_SUSPENDED:
                // Cause the worker thread to die.
                state = State.TERMINATED;
                interruptWorkerThread();

                // Put the current job back in the queue to be cancelled along
                // with the others.
                jobQueue.offer(latestJob);
                break;
            case IDLE_WAITING:
                state = State.TERMINATED;
                notifyWorkerThread();
                break;
            case TERMINATED:
                // Been there, done that.
                return;
            default:
                throw new IllegalStateException("cannot cancel from state: " + state);
        }

        // Cancel all jobs in the queue.
        for (Job j : jobQueue.asCollection()) {
            JobResult cancelledJob = new JobResult(j);
            resultHandler.handleResult(cancelledJob);
        }
    }

    /**
     * Cancels a specific job.
     *
     * @param jobID the ID of the job to be cancelled
     * @throws ServiceRefusedException if the job can't be found
     */
    public synchronized void cancelJob(final String jobID) throws ServiceRefusedException {
        switch (state) {
            case ACTIVE_SUSPENDED:
                if (latestJob.getIQID().equals(jobID)) {
                    // Cause the worker thread to cease execution of the current
                    // job and wait.
                    state = State.IDLE_WAITING;
                    interruptWorkerThread();

                    // Put the current job in the queue to be discovered and
                    // cancelled.
                    jobQueue.offer(latestJob);
                }
                break;
            case IDLE_WAITING:
                // Nothing to do.
                break;
            default:
                throw new IllegalStateException("can't cancel jobs in state: " + state);
        }

        // Look for the job in the queue and remove it if present.
        // FIXME: inefficient
        for (Job j : jobQueue.asCollection()) {
            if (j.getIQID().equals(jobID)) {
                jobQueue.remove(j);
                return;
            }
        }

        throw new ServiceRefusedException("job not found: " + jobID);
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Causes the worker runnable to stop waiting.
     */
    private void notifyWorkerThread() {
        synchronized (workerWaitMonitor) {
            workerWaitMonitor.notify();
        }
    }

    /**
     * Causes the worker runnable to abandon execution of a script.
     */
    private void interruptWorkerThread() {
        workerThread.interrupt();
    }

    @SuppressWarnings({"deprecation"})
    private void suspendWorkerThread() {
        workerThread.suspend();
    }

    @SuppressWarnings({"deprecation"})
    private void resumeWorkerThread() {
        workerThread.resume();
    }

    ////////////////////////////////////////////////////////////////////////////

    private void evaluate(final Job request) {
        try {
            Object returnObject = scriptEngine.eval(request.getExpression());

            if (null != returnObject && !(returnObject instanceof String)) {
                LOGGER.error("object returned by ScriptEngine.eval is not of the expected type java.lang.String");
                System.exit(1);
            }

            String returnvalue = (null == returnObject)
                    ? "" : (String) returnObject;

            yieldResult(request, returnvalue);
        } catch (ScriptException e) {
            yieldError(request, e);
        }
    }

    private void yieldError(final Job job,
                            final ScriptException exception) {
        latestResult = new JobResult(job, exception);
    }

    private void yieldResult(final Job job,
                             final String expression) {
        latestResult = new JobResult(job, expression);
    }

    private class WorkerRunnable implements Runnable {

        public void run() {
            // Break out when the worker is terminated.
            while (State.TERMINATED != state) {
                try {
                    if (State.ACTIVE_INPROGRESS == state) {
                        evaluate(latestJob);
                        state = State.IDLE_FINISHED;

                        synchronized (timeoutMonitor) {
                            // Notify the parent thread that a result is available.
                            timeoutMonitor.notify();
                        }
                    }

                    synchronized (workerWaitMonitor) {
                        workerWaitMonitor.wait();
                    }
                } catch (InterruptedException e) {
                    // Ignore and continue.  The point was to break out of the
                    // body of the loop.
                } catch (Exception e) {
                    // TODO: stack trace
                    LOGGER.error("worker runnable died with error: " + e.toString());
                }
            }
        }
    }
}
