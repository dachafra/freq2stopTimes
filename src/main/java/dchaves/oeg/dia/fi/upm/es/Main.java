package dchaves.oeg.dia.fi.upm.es;


import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONObject;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;

import java.io.FileReader;
import java.io.IOException;


public class Main
{
    public static void main( String[] args )
    {
        BasicConfigurator.configure();
        try {
            JSONObject config = new JSONObject(IOUtils.toString(new FileReader("config.json")));
            Gtfs2Collection gtfs2Collection = new Gtfs2Collection(config.getString("path"));
            GtfsDaoImpl gtfs = gtfs2Collection.read();
            Freq2StopTimes freq2StopTimes = new Freq2StopTimes(gtfs,config.getString("output"), config.getString("ZoneId"));
            if(freq2StopTimes.checkFrequencies()){
                freq2StopTimes.createNewTrips();
            }
        }catch (IOException e){
            System.out.printf("Error reading the configuration file: "+e.getMessage());
        }



    }
}
