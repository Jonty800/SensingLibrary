package uk.ac.kent.eda.jb956.sensorlibrary.control;

/*
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by ce208 on 06/04/2017.
 * Modified by jb956 on 02/02/2018.
 */

public class WorkerThread extends HandlerThread {
    private static WorkerThread thread;

    private Handler workerHandler;
    private final Object lock = new Object();

    private WorkerThread() {
        super(_TAG);
    }

    static private final String _TAG = "WorkerThread";


    /**
     * Create the worker thread and start if needed
     * @return The worker thread for this instance
     */
    public static WorkerThread create() {
        if (thread == null) {
            thread = new WorkerThread();
        }
        if (!thread.isAlive()) {
            thread.start();
        }
        return thread;
    }

    public static WorkerThread getInstance() {
        return thread;
    }

    /**
     * Stops the worker thread
     */
    public void close() {
        clearTasks();
        if (isAlive()) {
            quit();
        }
        thread = null;
    }

    @Override
    protected void onLooperPrepared() {
        // Lock and notify when workHandler is assigned a value
        synchronized (lock) {
            workerHandler = new Handler(getLooper());
            // Notify workHandler has a value
            lock.notify();
        }
    }

    /**
     * Posts a task immediately
     * @param runnable The task to post
     */
    public void postNow(Runnable runnable) {
        // Check if workerHandler has a value
        synchronized (lock) {
            while (workerHandler == null) {
                try {
                    // Block until notified by the looper
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        workerHandler.post(runnable);
    }

    /**
     * Removes a task callback from the stack
     * @param task The task to remove
     */
    public void removeTask(Runnable task) {
        // Check if workerHandler has a value
        synchronized (lock) {
            while (workerHandler == null) {
                try {
                    // Block until notified by the looper
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        workerHandler.removeCallbacks(task);
    }

    /**
     * Posts a task with a delay
     * @param task Task to post
     * @param delay Delay in milliseconds
     */
    public void postDelayed(Runnable task, long delay) {
        // Check if workerHandler has a value
        synchronized (lock) {
            while (workerHandler == null) {
                try {
                    // Block until notified by the looper
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        workerHandler.postDelayed(task, delay);
    }

    /**
     * Posts a task at a certain timestamp
     * @param task Task to post
     * @param timestamp Timestamp in milliseconds
     */
    public void postAtTime(Runnable task, long timestamp) {
        // Check if workerHandler has a value
        synchronized (lock) {
            while (workerHandler == null) {
                try {
                    // Block until notified by the looper
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        workerHandler.postAtTime(task, timestamp);
    }

    /**
     * Clears all tasks from the stack
     */
    public void clearTasks() {
        workerHandler.removeCallbacksAndMessages(null);
    }
}

