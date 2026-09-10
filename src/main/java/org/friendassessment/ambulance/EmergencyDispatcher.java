package org.friendassessment.ambulance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class EmergencyDispatcher {

    private final Map<String, Ambulance> fleet = new HashMap<>();
    private final Map<String, EmergencyCase> cases = new HashMap<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    private final PriorityQueue<EmergencyCase> waiting =
            new PriorityQueue<>(
                    Comparator
                            .comparingInt((EmergencyCase c) -> c.priority().value())
                            .reversed()
                            .thenComparing(EmergencyCase::created)
            );

    private static final double SPEED = 40.0;

    public void addAmbulance(Ambulance a) {

        if (a == null) {
            throw new DispatchException("Ambulance required");
        }

        if (fleet.putIfAbsent(a.id(), a) != null) {
            throw new DispatchException("Duplicate ambulance id");
        }
    }

    public void createEmergency(EmergencyCase c) {

        if (c == null) {
            throw new DispatchException("Emergency required");
        }

        if (cases.putIfAbsent(c.id(), c) != null) {
            throw new DispatchException("Duplicate emergency id");
        }

        Ambulance a = choose(c);

        if (a == null) {
            waiting.offer(c);
            log(c.id(), "Waiting for ambulance");
        } else {
            assign(c, a);
        }
    }

    private Ambulance choose(EmergencyCase c) {

        return fleet.values()
                .stream()
                .filter(Ambulance::free)
                .filter(a -> compatible(a.category(), c.priority()))
                .min(
                        Comparator
                                .comparingInt(
                                        (Ambulance a) ->
                                                typeScore(
                                                        a.category(),
                                                        c.priority()
                                                )
                                )
                                .thenComparingInt(
                                        a -> a.location()
                                                .equalsIgnoreCase(c.pickup())
                                                ? 0 : 1
                                )
                )
                .orElse(null);
    }

    private boolean compatible(
            AmbulanceCategory a,
            PriorityLevel p
    ) {

        return p != PriorityLevel.CRITICAL
                || a == AmbulanceCategory.ICU;
    }

    private int typeScore(
            AmbulanceCategory a,
            PriorityLevel p
    ) {

        if (p == PriorityLevel.CRITICAL) {
            return a == AmbulanceCategory.ICU ? 0 : 9;
        }

        if (p == PriorityLevel.HIGH) {
            return a == AmbulanceCategory.ADVANCED_LIFE_SUPPORT
                    ? 0
                    : (a == AmbulanceCategory.ICU ? 1 : 2);
        }

        return a == AmbulanceCategory.BASIC ? 0 : 1;
    }

    private void assign(
            EmergencyCase c,
            Ambulance a
    ) {

        a.dispatch(c.id());

        double eta =
                c.distanceKm() / SPEED * 60;

        c.assign(a.id(), eta);

        log(c.id(), "Assigned " + a.id());
    }

    public void changePhase(
            String ambulanceId,
            AmbulancePhase next
    ) {

        Ambulance a = fleet.get(ambulanceId);

        if (a == null) {
            throw new DispatchException("Ambulance not found");
        }

        a.moveTo(next);

        EmergencyCase c = cases.get(a.currentRequest());

        if (c != null && next == AmbulancePhase.EN_ROUTE) {
            c.activate();
        }

        if (c != null) {
            log(c.id(), "Ambulance phase " + next);
        }
    }

    public void finish(String ambulanceId) {

        Ambulance a = fleet.get(ambulanceId);

        if (a == null) {
            throw new DispatchException("Ambulance not found");
        }

        if (a.phase() != AmbulancePhase.HOSPITAL_ARRIVED) {
            throw new DispatchException(
                    "Cannot finish before hospital arrival"
            );
        }

        EmergencyCase c = cases.get(a.currentRequest());

        if (c != null) {
            c.complete();
            log(c.id(), "Emergency completed");
        }

        a.makeAvailable();

        allocateWaiting();
    }

    private void allocateWaiting() {

        while (!waiting.isEmpty()) {

            Ambulance a = choose(waiting.peek());

            if (a == null) {
                return;
            }

            assign(waiting.poll(), a);
        }
    }

    private void log(
            String id,
            String action
    ) {

        history.add(
                new HistoryEntry(
                        id,
                        action,
                        java.time.Instant.now()
                )
        );
    }

    public EmergencyCase getCase(String id) {

        EmergencyCase c = cases.get(id);

        if (c == null) {
            throw new DispatchException("Emergency not found");
        }

        return c;
    }

    public int waitingCount() {
        return waiting.size();
    }

    public List<HistoryEntry> history() {
        return List.copyOf(history);
    }
}