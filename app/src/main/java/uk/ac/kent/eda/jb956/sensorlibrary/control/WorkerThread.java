package uk.ac.kent.eda.jb956.sensorlibrary.control;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * Created by ce208 on 06/04/2017.
 */

public class WorkerThread extends HandlerThread {
    private static WorkerThread thread;

    private Handler workerHandler;
    private final Object lock = new Object();

    private WorkerThread() {
        super("WorkerThread");
    }
    static private final String _TAG = "WorkerThread";


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

    public void postTask(Runnable runnable) {
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

    public void removeDelayedTask(Runnable task) {
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

    public void postDelayedTask(Runnable task, long delay) {
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

    public void clearTasks() {
        workerHandler.removeCallbacksAndMessages(null);
    }
}

