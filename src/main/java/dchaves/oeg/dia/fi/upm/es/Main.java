package dchaves.oeg.dia.fi.upm.es;


import org.onebusaway.gtfs.impl.GtfsDaoImpl;


public class Main
{
    public static void main( String[] args )
    {
        Gtfs2Collection gtfs2Collection = new Gtfs2Collection(args[0]);
        GtfsDaoImpl gtfs = gtfs2Collection.read();
        Freq2StopTimes freq2StopTimes = new Freq2StopTimes(gtfs);
        if(freq2StopTimes.checkFrequencies()){
            freq2StopTimes.createNewTrips();
        }



    }
}
