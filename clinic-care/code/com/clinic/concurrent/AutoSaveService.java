package com.clinic.concurrent;

import com.clinic.exception.StorageException;
import com.clinic.storage.DataStore;
import com.clinic.storage.Repository;

/**
 * Writes the in-memory data back to storage every few seconds, but only when
 * something actually changed. A power cut then costs at most one interval of
 * work instead of the whole session.
 *
 * This class EXTENDS Thread while {@link ReminderService} IMPLEMENTS Runnable -
 * the two ways of creating a thread in Java, one of each, on purpose.
 *
 * Syllabus concepts: extending Thread, daemon threads, sleep, interrupt,
 * synchronized access to shared data, exception handling in threads.
 */
public final class AutoSaveService extends Thread {

    private final DataStore store;
    private final Repository repository;
    private final long intervalMillis;

    private volatile boolean running = true;
    private volatile int saveCount = 0;
    private volatile String lastError = null;

    public AutoSaveService(DataStore store, Repository repository, long intervalMillis) {
        super("auto-save");          // naming a thread makes debugging far easier
        this.store = store;
        this.repository = repository;
        this.intervalMillis = intervalMillis;
        setDaemon(true);             // must never keep the JVM alive on exit
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(intervalMillis);
                saveIfDirty();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    /** Saves only when the DataStore reports unsaved changes. */
    public boolean saveIfDirty() {
        if (!store.isDirty()) {
            return false;
        }
        try {
            // synchronizing on the store stops a write happening mid-save
            synchronized (store) {
                repository.saveAll(store);
            }
            saveCount++;
            lastError = null;
            return true;
        } catch (StorageException ex) {
            lastError = ex.displayMessage();
            return false;
        }
    }

    public int getSaveCount() {
        return saveCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void shutdown() {
        running = false;
        interrupt();     // wake it out of sleep immediately
    }
}
