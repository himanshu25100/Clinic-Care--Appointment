package com.clinic.concurrent;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.storage.DataStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Background worker that scans for appointments happening tomorrow and queues
 * a reminder for each one.
 *
 * It deliberately does NOT print straight to the console - that would scribble
 * over whatever menu the user is reading. Instead reminders go into a shared
 * list which the UI drains at a safe moment. That shared list is the piece of
 * state two threads touch, so every access to it is synchronized.
 *
 * Syllabus concepts: Runnable interface, Thread creation, thread life cycle,
 * sleep(), interrupt handling, synchronized blocks, volatile flag.
 */
public class ReminderService implements Runnable {

    private final DataStore store;
    private final long intervalMillis;

    private final List<String> pending = new ArrayList<>();
    private final Set<String> alreadySent = new LinkedHashSet<>();
    private final Object lock = new Object();

    /** volatile: the change made by the main thread is seen by this thread. */
    private volatile boolean running = true;

    private int scanCount = 0;

    public ReminderService(DataStore store, long intervalMillis) {
        this.store = store;
        this.intervalMillis = intervalMillis;
    }

    /**
     * The thread's life: RUNNABLE -> TIMED_WAITING (sleep) -> RUNNABLE -> ...
     * until stop() flips the flag or the thread is interrupted, then TERMINATED.
     */
    @Override
    public void run() {
        while (running) {
            try {
                scanOnce();
                Thread.sleep(intervalMillis);
            } catch (InterruptedException ex) {
                // good practice: restore the flag and leave the loop
                Thread.currentThread().interrupt();
                running = false;
            } catch (RuntimeException ex) {
                // a background thread must never die silently from a bug
                synchronized (lock) {
                    pending.add("Reminder service hit an error: " + ex.getMessage());
                }
            }
        }
    }

    /** One pass over tomorrow's bookings. Also callable directly from tests. */
    public void scanOnce() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<String> found = new ArrayList<>();

        for (Appointment a : store.allAppointments()) {
            if (!a.isActive() || !a.getDate().equals(tomorrow)) {
                continue;
            }
            Doctor doctor = store.findDoctor(a.getDoctorId());
            String doctorName = doctor == null ? a.getDoctorId() : doctor.getName();
            String message = "Tomorrow " + a.getSlotLabel() + "  "
                    + a.getPatientId() + " with " + doctorName
                    + "  (" + a.getId() + ")";
            found.add(message);
        }

        synchronized (lock) {
            scanCount++;
            for (String message : found) {
                // a reminder is queued only once per appointment
                if (alreadySent.add(message)) {
                    pending.add(message);
                }
            }
        }
    }

    /** Hands over every queued reminder and empties the queue. */
    public List<String> drainReminders() {
        synchronized (lock) {
            if (pending.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> copy = new ArrayList<>(pending);
            pending.clear();
            return copy;
        }
    }

    public int getScanCount() {
        synchronized (lock) {
            return scanCount;
        }
    }

    public void stop() {
        running = false;
    }
}
