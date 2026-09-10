package org.friendassessment.ambulance;
public class Ambulance {
 private final String id; private final AmbulanceCategory category; private final String driverName;
 private String location; private AmbulancePhase phase=AmbulancePhase.AVAILABLE; private String currentRequest;
 public Ambulance(String id,AmbulanceCategory category,String driverName,String location){
  if(blank(id)||category==null||blank(driverName)||blank(location)) throw new IllegalArgumentException("Invalid ambulance information");
  this.id=id.trim();this.category=category;this.driverName=driverName.trim();this.location=location.trim();
 }
 private boolean blank(String x){return x==null||x.trim().isEmpty();}
 public String id(){return id;} public AmbulanceCategory category(){return category;} public String driverName(){return driverName;}
 public String location(){return location;} public AmbulancePhase phase(){return phase;} public boolean free(){return phase==AmbulancePhase.AVAILABLE&&currentRequest==null;}
 public void dispatch(String requestId){if(!free())throw new DispatchException("Ambulance already assigned"); if(blank(requestId))throw new IllegalArgumentException("Request id required"); currentRequest=requestId;phase=AmbulancePhase.DISPATCHED;}
 public void moveTo(AmbulancePhase next){
  boolean ok=(phase==AmbulancePhase.DISPATCHED&&next==AmbulancePhase.EN_ROUTE)||(phase==AmbulancePhase.EN_ROUTE&&next==AmbulancePhase.PATIENT_PICKED_UP)||(phase==AmbulancePhase.PATIENT_PICKED_UP&&next==AmbulancePhase.HOSPITAL_ARRIVED);
  if(!ok)throw new DispatchException("Invalid ambulance state transition");
  phase=next;
 }
 public String currentRequest(){return currentRequest;}
 public void makeAvailable(){if(phase!=AmbulancePhase.HOSPITAL_ARRIVED)throw new DispatchException("Hospital arrival required before availability");phase=AmbulancePhase.AVAILABLE;currentRequest=null;}
}
