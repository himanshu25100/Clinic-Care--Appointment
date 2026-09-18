package com.clinic.storage;

import com.clinic.exception.StorageException;

/**
 * The contract every storage back-end must satisfy.
 *
 * The whole application talks to this interface and never to a file or a
 * database directly. Today {@link CsvRepository} implements it. Swapping in a
 * JdbcRepository (Connection / PreparedStatement / ResultSet) later means
 * writing one new class and changing one line in Main - nothing else moves.
 *
 * Syllabus concepts: interfaces, abstraction, polymorphism, loose coupling.
 */
public interface Repository {

    /** Pulls everything from storage into the in-memory DataStore. */
    void loadAll(DataStore store) throws StorageException;

    /** Pushes the whole in-memory DataStore back to storage. */
    void saveAll(DataStore store) throws StorageException;

    /** Human readable name of the back-end, shown in the System Info screen. */
    String backendName();
}
