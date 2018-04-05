package dchaves.oeg.dia.fi.upm.es;


import org.apache.log4j.BasicConfigurator;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;



public class Main
{
    public static void main( String[] args )
    {
        BasicConfigurator.configure();
        Gtfs2Collection gtfs2Collection = new Gtfs2Collection(args[0]);
        GtfsDaoImpl gtfs = gtfs2Collection.read();
        Freq2StopTimes freq2StopTimes = new Freq2StopTimes(gtfs,args[1], args[2]);
        if(freq2StopTimes.checkFrequencies()){
            freq2StopTimes.createNewTrips();
        }



    }
}
