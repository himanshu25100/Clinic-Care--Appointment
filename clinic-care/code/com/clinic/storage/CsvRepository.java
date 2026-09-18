package com.clinic.storage;

import com.clinic.exception.StorageException;
import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Prescription;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Character-stream storage back-end. Each entity type lives in its own
 * comma separated file inside the data/ folder.
 *
 * Syllabus concepts: character-oriented streams (FileReader / FileWriter),
 * BufferedReader / BufferedWriter, try-with-resources, IOException handling,
 * throw / throws, String.split, interfaces.
 */
public class CsvRepository implements Repository {

    private static final String HEADER_PREFIX = "#";

    private final File dataDir;

    public CsvRepository(String dataDirPath) {
        this.dataDir = new File(dataDirPath);
        if (!dataDir.exists()) {
            // ignore the result: if it fails, the first save reports the real error
            dataDir.mkdirs();
        }
    }

    @Override
    public String backendName() {
        return "CSV files in " + dataDir.getPath();
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    @Override
    public void loadAll(DataStore store) throws StorageException {
        store.clearAll();

        for (String line : readLines("doctors.csv")) {
            Doctor d = Doctor.fromCsv(line);
            if (d != null) {
                store.addDoctor(d);
            }
        }
        for (String line : readLines("patients.csv")) {
            Patient p = Patient.fromCsv(line);
            if (p != null) {
                store.addPatient(p);
            }
        }
        for (String line : readLines("appointments.csv")) {
            Appointment a = Appointment.fromCsv(line);
            if (a != null) {
                store.addAppointment(a);
            }
        }
        for (String line : readLines("prescriptions.csv")) {
            Prescription r = Prescription.fromCsv(line);
            if (r != null) {
                store.addPrescription(r);
            }
        }

        store.seedDoctorsIfEmpty();
        store.recalculateCounters();
        store.markClean();
    }

    /**
     * Reads one file into a list of usable lines.
     * A missing file is not an error - it just means "nothing stored yet".
     */
    private List<String> readLines(String fileName) throws StorageException {
        List<String> lines = new ArrayList<>();
        File f = new File(dataDir, fileName);
        if (!f.exists()) {
            return lines;
        }
        // try-with-resources: the reader is closed automatically, even on error
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(HEADER_PREFIX)) {
                    continue;   // skip blanks and the header comment
                }
                lines.add(trimmed);
            }
        } catch (IOException ex) {
            // low-level exception is wrapped in our own exception type
            throw new StorageException("Could not read " + fileName, ex);
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    @Override
    public void saveAll(DataStore store) throws StorageException {
        List<String> doctorLines = new ArrayList<>();
        for (Doctor d : store.allDoctors()) {
            doctorLines.add(d.toCsv());
        }
        writeLines("doctors.csv", "#id,name,phone,specialization,room,onLeave", doctorLines);

        List<String> patientLines = new ArrayList<>();
        for (Patient p : store.allPatients()) {
            patientLines.add(p.toCsv());
        }
        writeLines("patients.csv", "#id,name,phone,age,gender,bloodGroup,registeredOn", patientLines);

        List<String> apptLines = new ArrayList<>();
        for (Appointment a : store.allAppointments()) {
            apptLines.add(a.toCsv());
        }
        writeLines("appointments.csv",
                "#id,patientId,doctorId,date,slot,reason,status,bookedAt,fee", apptLines);

        List<String> rxLines = new ArrayList<>();
        for (Prescription r : store.allPrescriptions()) {
            rxLines.add(r.toCsv());
        }
        writeLines("prescriptions.csv",
                "#id,appointmentId,patientId,doctorId,issuedOn,diagnosis,medicines,advice,followUp",
                rxLines);

        store.markClean();
    }

    /**
     * Writes to a temporary file first and only then replaces the real one.
     * If the program is killed mid-write, the old good file survives.
     */
    private void writeLines(String fileName, String header, List<String> lines)
            throws StorageException {
        File target = new File(dataDir, fileName);
        File temp = new File(dataDir, fileName + ".tmp");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write(header);
            writer.newLine();
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException ex) {
            throw new StorageException("Could not write " + fileName, ex);
        }

        if (target.exists() && !target.delete()) {
            throw new StorageException("Could not replace " + fileName, null);
        }
        if (!temp.renameTo(target)) {
            throw new StorageException("Could not rename temp file for " + fileName, null);
        }
    }
}
