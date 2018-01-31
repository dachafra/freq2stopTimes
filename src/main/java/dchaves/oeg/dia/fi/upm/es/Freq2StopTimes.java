package dchaves.oeg.dia.fi.upm.es;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.serialization.GtfsWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

public class Freq2StopTimes {
    private GtfsDaoImpl newStore;
    private GtfsDaoImpl oldStore;
    private HashMap<String, String> newTrips;
    private Integer globalIdStopTimes;
    private String outputPath;
    private LocalDateTime todayMidnight;

    public Freq2StopTimes(GtfsDaoImpl oldStore, String outputPath){
        this.newStore = new GtfsDaoImpl();
        this.oldStore = oldStore;
        newTrips = new HashMap<>();
        globalIdStopTimes=1;
        this.outputPath = outputPath;
        this.todayMidnight =  LocalDateTime.of(LocalDate.now(ZoneId.of("Europe/Madrid")),LocalTime.MIDNIGHT);
    }

    //edit the ids of the trips with the departureTime extracted from frequencies
    public void createNewTrips(){
        Collection<Trip> trips=oldStore.getAllTrips();
        Collection<StopTime> stopTimes = oldStore.getAllStopTimes();
        Collection<Frequency> frequencies = oldStore.getAllFrequencies();
        HashMap<String,StopTime> firstStopTime = new HashMap<>();
        HashMap<String,ArrayList<StopTime>> sumaryStopTimes = this.sumaryStopTimes();
        LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(ZoneId.of("Europe/Madrid")),LocalTime.MIDNIGHT);

        //get the first stop of each trip
        stopTimes.forEach(stopTime -> {
            if(!firstStopTime.containsKey(stopTime.getTrip().getId().getId())){
                firstStopTime.put(stopTime.getTrip().getId().getId(),stopTime);
            }
            else{
                if(stopTime.getArrivalTime()<firstStopTime.get(stopTime.getTrip().getId().getId()).getArrivalTime()){
                    firstStopTime.put(stopTime.getTrip().getId().getId(),stopTime);
                }
            }
        });

        //edit the id of the trips from stop_times.txt
        trips.forEach(trip -> {
            for (String key : firstStopTime.keySet()) {
                if (trip.getId().getId().equals(key)) {
                    LocalDateTime timeFirstStop = todayMidnight.plusSeconds(firstStopTime.get(key).getArrivalTime()).minusHours(1);
                    Trip t = new Trip(trip);
                    t.setId(new AgencyAndId());
                    t.getId().setId(trip.getId().getId() + "_" + new SimpleDateFormat("HH:mm:ss").format(Date.from(timeFirstStop.toInstant(ZoneOffset.UTC))));
                    t.getId().setAgencyId(trip.getId().getAgencyId());
                    newStore.saveEntity(t);
                    newTrips.put(trip.getId().getId(),new SimpleDateFormat("HH:mm:ss").format(Date.from(timeFirstStop.toInstant(ZoneOffset.UTC))));
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
                        Integer departureTime = frequency.getStartTime()+frequency.getHeadwaySecs()*i;
                        LocalDateTime departureDate=todayMidnight.plusSeconds(frequency.getStartTime()+frequency.getHeadwaySecs()*i).minusHours(1);
                        String newId=trip.getId().getId() + "_" + new SimpleDateFormat("HH:mm:ss").format(Date.from(departureDate.toInstant(ZoneOffset.UTC)));
                        t.getId().setId(newId);
                        t.getId().setAgencyId(trip.getId().getAgencyId());
                        newStore.saveEntity(t);
                        createNewStopTimes(departureTime,(ArrayList<StopTime>)sumaryStopTimes.get(trip.getId().getId()).clone(),newId,firstStopTime.get(trip.getId().getId()));
                    }

                }
            });
        });
        write();
    }


    public void createNewStopTimes(Integer departureTime, ArrayList<StopTime> stopTimes, String id, StopTime header){
        if(departureTime>=86400){
            departureTime=departureTime-86400;
        }
        for(StopTime t : stopTimes){
            StopTime st = new StopTime(t);
            st.setTrip(new Trip());
            st.getTrip().setId(new AgencyAndId());
            st.setArrivalTime(departureTime+(t.getArrivalTime()-header.getArrivalTime()));
            st.setDepartureTime(departureTime+(t.getDepartureTime()-header.getDepartureTime()));
            st.getTrip().getId().setId(id);
            st.setId(globalIdStopTimes);
            st.getTrip().getId().setAgencyId(t.getTrip().getId().getAgencyId());
            globalIdStopTimes++;
            newStore.saveEntity(st);
        }


    }


    public void write(){
        GtfsWriter writer = new GtfsWriter();
        writer.setOutputLocation(new File(outputPath));
        try {
            writer.run(newStore);
            writer.close();
            this.writeStop_times();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeStop_times() throws IOException{
        //Get the file reference
        Path path = Paths.get(outputPath+"/stop_times_aux.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type\n");
            Collection<StopTime> stopTimes = newStore.getAllStopTimes();
            stopTimes.forEach(stopTime ->{

                LocalDateTime localDateTime=todayMidnight.plusSeconds(stopTime.getDepartureTime()).minusHours(1);
                String departureTime= new SimpleDateFormat("HH:mm:ss").format(Date.from(localDateTime.toInstant(ZoneOffset.UTC)));

                localDateTime=todayMidnight.plusSeconds(stopTime.getArrivalTime()).minusHours(1);
                String arrivalTime= new SimpleDateFormat("HH:mm:ss").format(Date.from(localDateTime.toInstant(ZoneOffset.UTC)));

                try {
                    writer.write(stopTime.getTrip().getId().getId()+","+arrivalTime+","+departureTime+","+stopTime.getStop().getId().getId()+","+stopTime.getStopSequence()+","+stopTime.getPickupType()+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } );
        }
    }


    public HashMap<String, ArrayList<StopTime>> sumaryStopTimes(){
        Collection<StopTime> stopTimes = oldStore.getAllStopTimes();
        HashMap<String,ArrayList<StopTime>> summary = new HashMap<>();

        stopTimes.forEach(stopTime -> {
            if(!summary.containsKey(stopTime.getTrip().getId().getId())){
                summary.put(stopTime.getTrip().getId().getId(),new ArrayList<>());
            }
            summary.get(stopTime.getTrip().getId().getId()).add(stopTime);
        });
        return summary;
    }


    public boolean checkFrequencies(){
       return !oldStore.getAllFrequencies().isEmpty();

    }
}
