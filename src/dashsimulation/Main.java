/*
 * Main class
 */
package dashsimulation;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Trace trace = new Trace("traces/bus.log");
        DashPlayer player = new DashPlayer(1, trace);
        player.startPlayer();
    }
    
    /**
     * test Trace class
     * @param log file name
     */
    public static void getBandwidthLog(String log) {
        int time = 0;
        double bw = 0;
        Trace trace = new Trace(log);
        while (bw >= 0) {
            bw = trace.getAvailableBW(time);
            System.out.println(time + "\t" + bw);
            time += 1000;
        }
    }
}
