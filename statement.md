# Problem Statement

## The problem

Small and mid-sized clinics in India still run their front desk on a paper
register. One receptionist holds the only copy of the day's schedule, and that
causes four recurring failures:

1. **Double booking.** Two people phone in seconds apart, both are pencilled
   into the 10:00 slot, and one of them is turned away after travelling to the
   clinic.
2. **Lost history.** A returning patient is asked "what did the doctor give you
   last time?" and has to remember, or produce a crumpled slip. Without the
   previous diagnosis the doctor is prescribing half-blind.
3. **No visibility.** Nobody can answer "how many patients did we see last
   week", "which doctor is overloaded", or "which slots always go empty",
   because the data only exists as handwriting.
4. **No reminders.** Patients simply forget, the slot is wasted, and the clinic
   absorbs the loss.

## Scope

**In scope**

- Registration of patients with a permanent ID
- Doctor directory with specializations, fees and leave status
- Slot-based appointment booking, cancellation and rescheduling
- Conflict-free allocation of slots even under concurrent requests
- Prescription writing and lifetime medical history retrieval
- Management reports on load, revenue, demographics and diagnoses
- Automatic persistence to disk and appointment reminders

**Out of scope (deliberately)**

- Payment gateway integration and billing receipts
- SMS or email delivery of reminders (reminders are generated, not transmitted)
- Multi-clinic or multi-branch operation
- Web or mobile interface; this is a console application
- Pharmacy stock, lab reports, and insurance claims

## Target users

| User | What they do with the system |
|---|---|
| **Patient** | Registers once, books and manages their own appointments, reads their prescription history |
| **Receptionist** | Registers walk-ins, finds patients who forgot their ID, books on their behalf |
| **Doctor** | Views the day's schedule, opens a patient's file before the consultation, writes the prescription after it |
| **Clinic manager** | Reads the analytics to plan staffing and see revenue |

## High-level features

- **Patient management** — validated registration, unique IDs, duplicate-phone
  detection, name search, profile updates
- **Appointment scheduling** — eight slots per working day per doctor, live
  availability, thread-safe booking, cancel and reschedule with ownership
  checks, senior-citizen discount, Sunday and leave handling
- **Medical records** — structured prescriptions with medicines, dosage
  patterns, advice and follow-up dates; full history newest-first; distinct
  past-condition summary
- **Reports and analytics** — per-doctor load, status breakdown, revenue,
  age distribution, diagnosis ranking, 7-day clinic load grid
- **Reliability features** — background auto-save, crash-safe file writes,
  background reminder scanning, a full custom exception hierarchy

## Success criteria

The system is successful if:

1. No two active appointments can ever hold the same doctor, date and slot —
   verified by a concurrency test, not just by inspection.
2. A patient who returns after months can retrieve their complete history from
   nothing but their patient ID.
3. Invalid input is rejected with a specific, readable message and never
   corrupts stored data.
4. The application can be killed at any moment and lose at most 20 seconds of
   work.
