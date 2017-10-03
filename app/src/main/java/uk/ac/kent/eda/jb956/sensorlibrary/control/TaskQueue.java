package uk.ac.kent.eda.jb956.sensorlibrary.control;

import java.util.LinkedList;

import android.util.Log;


/**
 * Source: http://google-ukdev.blogspot.com/2009/01/crimes-against-code-and-using-threads.html
 *
 */
public class TaskQueue {
    private final LinkedList<Runnable> tasks;
    private Thread thread;
    private boolean running;
    private Runnable internalRunnable;

    public static TaskQueue getInstance() {
        if(instance == null)
            instance = new TaskQueue();
        return instance;
    }

    private static TaskQueue instance;

    private final String ME = "TaskQueue";

    private class InternalRunnable implements Runnable {
        public void run() {
            internalRun();
        }
    }

    public TaskQueue() {
        tasks = new LinkedList<Runnable>();
        internalRunnable = new InternalRunnable();
        start();
    }

    public void start() {
        if (!running) {
            thread = new Thread(internalRunnable);
            thread.setDaemon(true);
            running = true;
            thread.start();
        }
    }

    public void stop() {
        running = false;
    }

    public void addNewTask(Runnable task) {
        synchronized(tasks) {
            tasks.addLast(task);
            tasks.notify(); // notify any waiting threads
        }
    }

    private Runnable getNextTask() {
        synchronized(tasks) {
            if (tasks.isEmpty()) {
                try {
                    tasks.wait();
                } catch (InterruptedException e) {
                    Log.e(ME, "Task interrupted", e);
                    stop();
                }
            }
            return tasks.removeFirst();
        }
    }


    private void internalRun() {
        while(running) {
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