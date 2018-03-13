package uk.ac.kent.eda.jb956.sensorlibrary.control;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Source: http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html
 */
public class TaskQueue {
    private final BlockingQueue<Runnable> tasks;
    private boolean running;
    private final Runnable internalRunnable;

    public static TaskQueue getInstance() {
        if (instance == null)
            instance = new TaskQueue();
        return instance;
    }

    private static TaskQueue instance;

    private final String ME = "TaskQueue";

    private class InternalRunnable implements Runnable {
        public void run() {
            try {
                internalRun();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private TaskQueue() {
        tasks = new LinkedBlockingQueue<>();
        internalRunnable = new InternalRunnable();
        start();
    }

    private void start() {
        if (!running) {
            Thread thread = new Thread(internalRunnable);
            thread.setDaemon(true);
            running = true;
            thread.start();
        }
    }

    private void stop() {
        running = false;
    }

    public void addNewTask(Runnable task) {
        synchronized (tasks) {
            try {
                tasks.put(task);
                tasks.notify(); // notify any waiting threads
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private Runnable getNextTask() throws InterruptedException {
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                try {
                    tasks.wait();
                } catch (InterruptedException e) {
                    Log.e(ME, "Task interrupted", e);
                    stop();
                }
            }
            return tasks.take();
        }
    }


    private void internalRun() throws InterruptedException {
        while (running) {
            Runnable task = getNextTask();
            try {
                task.run();
                Thread.yield();
            } catch (Throwable t) {
                Log.e(ME, "Task threw an exception", t);
            }
        }
    }
}