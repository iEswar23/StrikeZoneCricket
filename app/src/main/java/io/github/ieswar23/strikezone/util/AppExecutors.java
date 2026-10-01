package io.github.ieswar23.strikezone.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background (disk / network) and main-thread executors. Injected so tests can swap in a
 * synchronous executor.
 */
public class AppExecutors {
    private final Executor diskIO;
    private final Executor mainThread;

    public AppExecutors(Executor diskIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors create() {
        ExecutorService io = Executors.newFixedThreadPool(2, runnable -> {
            Thread t = new Thread(runnable, "strikezone-io");
            t.setDaemon(true);
            return t;
        });
        Handler handler = new Handler(Looper.getMainLooper());
        return new AppExecutors(io, handler::post);
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }
}
