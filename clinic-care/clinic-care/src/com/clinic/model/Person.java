package com.clinic.model;

/**
 * Abstract base class for every human entity in the system.
 *
 * Syllabus concepts demonstrated:
 *  - Abstraction (abstract class + abstract method)
 *  - Encapsulation (private fields + getters/setters)
 *  - 'final' keyword (id can never change once assigned)
 *  - Constructors and 'this' keyword
 */
public abstract class Person {

    private final String id;      // final: identity must never change
    private String name;
    private String phone;

    protected Person(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Every subclass must describe itself in its own way.
     * This is the hook that makes polymorphism possible.
     */
    public abstract String describe();

    /** Each subclass converts itself to one CSV line for storage. */
    public abstract String toCsv();

    @Override
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // 'instanceof' operator, straight from the syllabus
        if (!(other instanceof Person)) {
            return false;
        }
        Person p = (Person) other;
        return this.id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
