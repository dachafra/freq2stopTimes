package dchaves.oeg.dia.fi.upm.es;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

public class Freq2StopTimes {
    private GtfsDaoImpl newStore;
    private GtfsDaoImpl oldStore;

    public Freq2StopTimes(GtfsDaoImpl oldStore){
        this.newStore = new GtfsDaoImpl();
        this.oldStore = oldStore;
    }

    //edit the ids of the trips with the departureTime extracted from frequencies
    public void createNewTrips(){
        Collection<Trip> trips=oldStore.getAllTrips();
        Collection<StopTime> stopTimes = oldStore.getAllStopTimes();
        Collection<Frequency> frequencies = oldStore.getAllFrequencies();
        HashMap<String,Integer> firstStopTime = new HashMap<>();
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);

        //get the first stop of each trip
        stopTimes.forEach(stopTime -> {
            if(!firstStopTime.containsKey(stopTime.getTrip().getId().getId())){
                firstStopTime.put(stopTime.getTrip().getId().getId(),stopTime.getArrivalTime());
            }
            else{
                if(stopTime.getArrivalTime()<firstStopTime.get(stopTime.getTrip().getId().getId())){
                    firstStopTime.replace(stopTime.getTrip().getId().getId(),stopTime.getArrivalTime());
                }
            }
        });

        //edit the id of the trips from stop_times.txt
        trips.forEach(trip -> {
            for (String key : firstStopTime.keySet()) {
                if (trip.getId().getId().equals(key)) {
                    LocalDateTime timeFirstStop = todayMidnight.plusSeconds(firstStopTime.get(key)).minusHours(1);
                    Trip t = new Trip(trip);
                    t.setId(new AgencyAndId());
                    t.getId().setId(trip.getId().getId() + "_" + new SimpleDateFormat("HH:mm:ss").format(Date.from(timeFirstStop.toInstant(ZoneOffset.UTC))));
                    t.getId().setAgencyId(trip.getId().getAgencyId());
                    newStore.saveEntity(t);
                }
            }
        });


        //create the new trips with frequencies.txt
        frequencies.forEach(frequency -> {
            trips.forEach(trip -> {
                if(trip.getId().getId().equals(frequency.getTrip().getId().getId())){
                    Integer times = (frequency.getEndTime()-frequency.getStartTime())/frequency.getHeadwaySecs();
                    for(int i=0; i<times;i++){
                        Trip t = new Trip(trip);
                        t.setId(new AgencyAndId());
                        LocalDateTime timeFirstStop=todayMidnight.plusSeconds(frequency.getStartTime()+frequency.getHeadwaySecs()*i).minusHours(1);
                        t.getId().setId(trip.getId().getId() + "_" + new SimpleDateFormat("HH:mm:ss").format(Date.from(timeFirstStop.toInstant(ZoneOffset.UTC))));
                        t.getId().setAgencyId(trip.getId().getAgencyId());
                        newStore.saveEntity(t);
                    }

                }
            });
        });

    }


    public void createNewStopTimes(){
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);

    }


    public boolean checkFrequencies(){
       return !oldStore.getAllFrequencies().isEmpty();

    }
}
