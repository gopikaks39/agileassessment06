package org.friendassessment.ambulance;
import java.time.Instant;
public class EmergencyCase {
 private final String id,patientId,emergencyType,pickup,hospital; private final PriorityLevel priority; private final double distanceKm; private final Instant created=Instant.now();
 private RequestPhase phase=RequestPhase.WAITING; private String ambulanceId; private double etaMinutes;
 public EmergencyCase(String id,String patientId,String emergencyType,PriorityLevel priority,String pickup,String hospital,double distanceKm){
  if(blank(id)||blank(patientId)||blank(emergencyType)||priority==null||blank(pickup)||blank(hospital)||distanceKm<0)throw new IllegalArgumentException("Invalid emergency request");
  this.id=id.trim();this.patientId=patientId.trim();this.emergencyType=emergencyType.trim();this.priority=priority;this.pickup=pickup.trim();this.hospital=hospital.trim();this.distanceKm=distanceKm;
 }
 private boolean blank(String x){return x==null||x.trim().isEmpty();}
 public String id(){return id;} public PriorityLevel priority(){return priority;} public String pickup(){return pickup;} public double distanceKm(){return distanceKm;} public Instant created(){return created;}
 public RequestPhase phase(){return phase;} public String ambulanceId(){return ambulanceId;} public double etaMinutes(){return etaMinutes;}
 public void assign(String ambulance,double eta){phase=RequestPhase.ASSIGNED;ambulanceId=ambulance;etaMinutes=eta;} public void activate(){phase=RequestPhase.ACTIVE;} public void complete(){phase=RequestPhase.COMPLETED;}
}
