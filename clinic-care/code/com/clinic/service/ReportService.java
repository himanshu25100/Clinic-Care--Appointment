package com.clinic.service;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Prescription;
import com.clinic.storage.DataStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE 4 - Reporting and Analytics.
 *
 * Turns the raw lists into the numbers a clinic manager actually asks for:
 * who is busiest, what is earned, which slots go empty, what people fall ill
 * with most often.
 *
 * Syllabus concepts: HashMap / LinkedHashMap, Map.Entry iteration, 2-D arrays,
 * recursion, enums, arrays.
 */
public class ReportService {

    private final DataStore store;
    private final AppointmentService appointmentService;

    public ReportService(DataStore store, AppointmentService appointmentService) {
        this.store = store;
        this.appointmentService = appointmentService;
    }

    /** Count of appointments per doctor, keyed by doctor ID. */
    public Map<String, Integer> appointmentsPerDoctor() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Doctor d : store.allDoctors()) {
            counts.put(d.getId(), 0);       // every doctor appears, even with zero
        }
        for (Appointment a : store.allAppointments()) {
            if (a.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            Integer current = counts.get(a.getDoctorId());
            counts.put(a.getDoctorId(), current == null ? 1 : current + 1);
        }
        return counts;
    }

    /** How many appointments ended in each status. */
    public Map<AppointmentStatus, Integer> statusBreakdown() {
        Map<AppointmentStatus, Integer> counts = new LinkedHashMap<>();
        for (AppointmentStatus s : AppointmentStatus.values()) {
            counts.put(s, 0);
        }
        for (Appointment a : store.allAppointments()) {
            counts.put(a.getStatus(), counts.get(a.getStatus()) + 1);
        }
        return counts;
    }

    /** Most frequent diagnoses across all prescriptions. */
    public Map<String, Integer> diagnosisFrequency() {
        Map<String, Integer> counts = new HashMap<>();
        for (Prescription r : store.allPrescriptions()) {
            String key = r.getDiagnosis().trim().toLowerCase();
            Integer current = counts.get(key);
            counts.put(key, current == null ? 1 : current + 1);
        }
        return counts;
    }

    /**
     * Total money billed, computed recursively.
     * Cancelled appointments already carry a fee of 0, so they add nothing.
     */
    public double totalRevenue() {
        return sumFees(store.allAppointments(), 0);
    }

    private double sumFees(List<Appointment> list, int index) {
        if (index >= list.size()) {     // base case: nothing left
            return 0.0;
        }
        return list.get(index).getFeeCharged() + sumFees(list, index + 1);
    }

    /**
     * Utilisation of the next 7 days across the whole clinic, as a 2-D array.
     * grid[day][slot] holds how many doctors are busy in that slot that day.
     */
    public int[][] weeklyLoadGrid(LocalDate startDate) {
        int[][] grid = new int[7][Doctor.SLOTS_PER_DAY];
        for (int day = 0; day < 7; day++) {
            LocalDate date = startDate.plusDays(day);
            for (Appointment a : appointmentService.onDate(date)) {
                int slot = a.getSlotIndex();
                if (slot >= 0 && slot < Doctor.SLOTS_PER_DAY) {
                    grid[day][slot] = grid[day][slot] + 1;
                }
            }
        }
        return grid;
    }

    /** Patients grouped into age bands, for the demographics report. */
    public Map<String, Integer> ageDistribution() {
        String[] bands = {"0-12", "13-19", "20-39", "40-59", "60+"};
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String band : bands) {
            counts.put(band, 0);
        }
        for (Patient p : store.allPatients()) {
            String band = bandFor(p.getAge());
            counts.put(band, counts.get(band) + 1);
        }
        return counts;
    }

    private String bandFor(int age) {
        if (age <= 12) {
            return "0-12";
        } else if (age <= 19) {
            return "13-19";
        } else if (age <= 39) {
            return "20-39";
        } else if (age <= 59) {
            return "40-59";
        } else {
            return "60+";
        }
    }

    /** The doctor with the most appointments. Returns null if there are none. */
    public String busiestDoctorId() {
        String busiest = null;
        int best = -1;
        for (Map.Entry<String, Integer> entry : appointmentsPerDoctor().entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                busiest = entry.getKey();
            }
        }
        return best <= 0 ? null : busiest;
    }

    /** Diagnoses sorted from most to least common, as "name (count)" strings. */
    public List<String> topDiagnoses(int limit) {
        Map<String, Integer> freq = diagnosisFrequency();
        List<String> keys = new ArrayList<>(freq.keySet());

        // simple selection sort on the counts - descending
        for (int i = 0; i < keys.size(); i++) {
            int maxIndex = i;
            for (int j = i + 1; j < keys.size(); j++) {
                if (freq.get(keys.get(j)) > freq.get(keys.get(maxIndex))) {
                    maxIndex = j;
                }
            }
            String temp = keys.get(i);
            keys.set(i, keys.get(maxIndex));
            keys.set(maxIndex, temp);
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < keys.size() && i < limit; i++) {
            result.add(keys.get(i) + " (" + freq.get(keys.get(i)) + ")");
        }
        return result;
    }
}
