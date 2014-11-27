package dashsimulation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Trace {
    List<TraceMeasurement> measurementList;
    
    /**
     * @param file_name: can be traces/bus.log, traces/ferry.log, traces/metro.log, traces/tram.log
     */
    Trace(String file_name) {
        measurementList = new ArrayList<>();
        if (!load_from_file(file_name)) 
             System.out.println("Loading trace failed");
    }
    
    /**
     * @param file_name log file name
     * @return succeeded/failed
     */
    public boolean load_from_file(String file_name) {
        measurementList.clear();
        BufferedReader reader;
        try {
             reader = new BufferedReader(new FileReader(file_name));
        } catch (FileNotFoundException ex) { System.out.println("Log file not found"); return false; }
        
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(" ");
                
                if (fields.length < 6) { System.out.println("Unexpected log file format (1)"); return false; }
                
                long timestamp;
                int elapsed_ms;
                double gps_lat, gps_lng;
                int nbr_of_bytes;
                
                try {
                    timestamp = Long.parseLong(fields[1]);
                    if (fields[2].compareTo("NOFIX") != 0 && fields[3].compareTo("NOFIX") != 0) {
                        gps_lat = Double.parseDouble(fields[2]);
                        gps_lng = Double.parseDouble(fields[3]);
                    }
                    else gps_lat = gps_lng = 0;
                    nbr_of_bytes = Integer.parseInt(fields[4]);
                    elapsed_ms = Integer.parseInt(fields[5]);
                } catch (NumberFormatException exp2) { System.out.println("Unexpected log file format (2)"); return false; }
                
                measurementList.add(new TraceMeasurement(timestamp, gps_lat, gps_lng, nbr_of_bytes, elapsed_ms));
            }
        } catch (IOException exp) { System.out.println("Unable to read from log file"); return false; }
        return true;
    }
    
    public TraceMeasurement getMeasurementAt(int i) {
        if (i < measurementList.size()) {
            return measurementList.get(i);
        }
        else return null;
    }
    
    /**
     * @param time
     * @return bandwidth at particular time in kbps
     */
    public int getAvailableBW(int time) {
        //timer.scheduleAtFixedRate(this, timer_duration, timer_duration);
        int counter = 0;
        int trace_time = 0;
        TraceMeasurement measurement = getMeasurementAt(counter);
        while (measurement != null) {
            counter += 1;
            trace_time += (double)measurement.ms_increment;
            if (trace_time >= time) {
                return (measurement.nbr_of_bytes*8) / measurement.ms_increment;
            }
            measurement = getMeasurementAt(counter);
        }
        return -1;
    }
}

class TraceMeasurement {
    public long timestamp;
    public double gps_lat;
    public double gps_lng;
    public int nbr_of_bytes;
    public int ms_increment;
    
    TraceMeasurement(long timestamp, double gps_lat, double gps_lng, int nbr_of_bytes, int elapsed_ms) {
        this.timestamp = timestamp;
        this.gps_lat = gps_lat;
        this.gps_lng = gps_lng;
        this.nbr_of_bytes = nbr_of_bytes;
        this.ms_increment = elapsed_ms;
    }
}