package dchaves.oeg.dia.fi.upm.es;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Gtfs2Collection {
    private GtfsDaoImpl store;
    private String path;
    private GtfsReader reader;
    private final Logger _log = LoggerFactory.getLogger(Gtfs2Collection.class);

    public Gtfs2Collection (String path) {
        this.store = new GtfsDaoImpl();
        this.reader = new GtfsReader();
        this.path = path;
    }

    public GtfsDaoImpl read(){
        try {
            reader.setInputLocation(new File((this.path)));
            reader.setEntityStore(store);
            reader.run();
            return store;
        }catch (IOException e){
            System.out.println("Error with the path of the GTFS: "+e.getMessage());
            return null;
        }
    }

}
