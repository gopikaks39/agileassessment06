package org.friendassessment.ambulance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class EmergencyDispatcherTest {

    // Helper method to create an ambulance
    private Ambulance amb(String id, AmbulanceCategory type) {
        return new Ambulance(
                id,
                type,
                "Driver",
                "Central"
        );
    }

    // Helper method to create an emergency
    private EmergencyCase emergency(
            String id,
            PriorityLevel priority
    ) {
        return new EmergencyCase(
                id,
                "P-" + id,
                "Emergency",
                priority,
                "Central",
                "Hospital",
                20
        );
    }

    // =========================================================
    // POSITIVE TEST CASES
    // =========================================================

    @Test
    void positiveCriticalUsesIcu() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb("B1", AmbulanceCategory.BASIC)
        );

        d.addAmbulance(
                amb("I1", AmbulanceCategory.ICU)
        );

        EmergencyCase c = emergency(
                "E1",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(c);

        assertEquals("I1", c.ambulanceId());

        assertEquals(
                RequestPhase.ASSIGNED,
                c.phase()
        );
    }

    @Test
    void positiveHighPrefersAls() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "A1",
                        AmbulanceCategory.ADVANCED_LIFE_SUPPORT
                )
        );

        EmergencyCase c = emergency(
                "E1",
                PriorityLevel.HIGH
        );

        d.createEmergency(c);

        assertEquals(
                "A1",
                c.ambulanceId()
        );
    }

    @Test
    void positiveWaitingQueueAutoAssigns() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        EmergencyCase one = emergency(
                "E1",
                PriorityLevel.CRITICAL
        );

        EmergencyCase two = emergency(
                "E2",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(one);

        d.createEmergency(two);

        // Second emergency should wait
        assertEquals(
                1,
                d.waitingCount()
        );

        // Ambulance moves through valid states
        d.changePhase(
                "I1",
                AmbulancePhase.EN_ROUTE
        );

        d.changePhase(
                "I1",
                AmbulancePhase.PATIENT_PICKED_UP
        );

        d.changePhase(
                "I1",
                AmbulancePhase.HOSPITAL_ARRIVED
        );

        // Finish first emergency
        d.finish("I1");

        // Second emergency should automatically get ambulance
        assertEquals(
                "I1",
                two.ambulanceId()
        );

        assertEquals(
                0,
                d.waitingCount()
        );
    }

    @Test
    void positiveEtaCalculated() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        EmergencyCase c = emergency(
                "E1",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(c);

        // Distance = 20 km
        // Speed = 40 km/h
        // ETA = 20 / 40 * 60 = 30 minutes

        assertEquals(
                30.0,
                c.etaMinutes()
        );
    }

    @Test
    void positiveEmergencyBecomesActiveWhenEnRoute() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        EmergencyCase c = emergency(
                "E10",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(c);

        d.changePhase(
                "I1",
                AmbulancePhase.EN_ROUTE
        );

        assertEquals(
                RequestPhase.ACTIVE,
                c.phase()
        );
    }

    @Test
    void positiveEmergencyHistoryRecorded() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        EmergencyCase c = emergency(
                "E20",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(c);

        assertFalse(
                d.history().isEmpty()
        );
    }


    // =========================================================
    // NEGATIVE TEST CASES
    // =========================================================

    @Test
    void negativeInvalidEmergency() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new EmergencyCase(
                        "",
                        "",
                        "",
                        null,
                        "",
                        "",
                        -1
                )
        );
    }

    @Test
    void negativeDuplicateAmbulance() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "A1",
                        AmbulanceCategory.BASIC
                )
        );

        assertThrows(
                DispatchException.class,
                () -> d.addAmbulance(
                        amb(
                                "A1",
                                AmbulanceCategory.ICU
                        )
                )
        );
    }

    @Test
    void negativeDoubleAssignment() {

        Ambulance a = amb(
                "A1",
                AmbulanceCategory.ICU
        );

        a.dispatch("E1");

        assertThrows(
                DispatchException.class,
                () -> a.dispatch("E2")
        );
    }

    @Test
    void negativeInvalidStateTransition() {

        Ambulance a = amb(
                "A1",
                AmbulanceCategory.BASIC
        );

        a.dispatch("E1");

        assertThrows(
                DispatchException.class,
                () -> a.moveTo(
                        AmbulancePhase.HOSPITAL_ARRIVED
                )
        );
    }

    @Test
    void negativeDuplicateEmergency() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        d.createEmergency(
                emergency(
                        "E1",
                        PriorityLevel.CRITICAL
                )
        );

        assertThrows(
                DispatchException.class,
                () -> d.createEmergency(
                        emergency(
                                "E1",
                                PriorityLevel.CRITICAL
                        )
                )
        );
    }

    @Test
    void negativeUnknownAmbulancePhaseChange() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        assertThrows(
                DispatchException.class,
                () -> d.changePhase(
                        "UNKNOWN",
                        AmbulancePhase.EN_ROUTE
                )
        );
    }

    @Test
    void negativeFinishUnknownAmbulance() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        assertThrows(
                DispatchException.class,
                () -> d.finish("UNKNOWN")
        );
    }


    // =========================================================
    // BOUNDARY TEST CASES
    // =========================================================

    @Test
    void boundaryZeroDistanceEta() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "I1",
                        AmbulanceCategory.ICU
                )
        );

        EmergencyCase c = new EmergencyCase(
                "E100",
                "P-E100",
                "Emergency",
                PriorityLevel.CRITICAL,
                "Central",
                "Hospital",
                0
        );

        d.createEmergency(c);

        assertEquals(
                0.0,
                c.etaMinutes()
        );
    }

    @Test
    void boundaryCriticalEmergencyWithoutIcuWaits() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "B1",
                        AmbulanceCategory.BASIC
                )
        );

        EmergencyCase c = emergency(
                "E101",
                PriorityLevel.CRITICAL
        );

        d.createEmergency(c);

        assertEquals(
                1,
                d.waitingCount()
        );

        assertNull(
                c.ambulanceId()
        );
    }


    // =========================================================
    // MULTIPLE FAILURE SCENARIOS
    // =========================================================

    @Test
    void multipleFailureInvalidAmbulanceOperations() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        assertThrows(
                DispatchException.class,
                () -> d.changePhase(
                        "UNKNOWN",
                        AmbulancePhase.EN_ROUTE
                )
        );

        assertThrows(
                DispatchException.class,
                () -> d.finish("UNKNOWN")
        );
    }

    @Test
    void multipleFailureDuplicateAndInvalidFinish() {

        EmergencyDispatcher d = new EmergencyDispatcher();

        d.addAmbulance(
                amb(
                        "A1",
                        AmbulanceCategory.BASIC
                )
        );

        // Failure 1: Duplicate ambulance
        assertThrows(
                DispatchException.class,
                () -> d.addAmbulance(
                        amb(
                                "A1",
                                AmbulanceCategory.ICU
                        )
                )
        );

        // Failure 2: Cannot finish ambulance before hospital arrival
        assertThrows(
                DispatchException.class,
                () -> d.finish("A1")
        );
    }
}