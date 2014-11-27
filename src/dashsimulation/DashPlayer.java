package dashsimulation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

//bitrate levels in kbps
enum LEVEL {
    LEVEL_1(250), LEVEL_2(500), LEVEL_3(750), LEVEL_4(1000), LEVEL_5(1500), LEVEL_6(3000);
    
    private final int value;
    private LEVEL(final int value) { this.value = value; }
    /**
     * @return bit rate in kbps of level
     */
    public int getValue() { return value; }
};

public class DashPlayer {
    final int chunk_duration = 2000; //in ms
    final int step_duration = 200; //duration of each simulaiton step, in ms
    
    Trace trace; //bandwidth trace trace used
    int num_players = 1; //number of players sharing bottleneck link
    
    //state variables used
    int current_time = 0;
    int current_bandwidth; //actual bandwidth in simulation
    boolean is_downloading = false; 
    LEVEL selected_bitrate = LEVEL.LEVEL_1; //selected bitrate of chunk being downloaded
    int remaining_chunk_bits; //number of bits remaining to complete download of current chunk
    int buffer_size = 0; //buffered video in ms
    int rand_target_buffer = updateRandTargetBuffer(); //target buffer size (for periodic scheduling)
                                                       //randomized to add jitter
    
    /**
     * Constructor
     * @param num_players total number of players sharing link with this player (includes self)
     * @param trace bandwidth trace to use
     */
    public DashPlayer(int num_players, Trace trace) {
        this.trace = trace;
        this.num_players = num_players;
    }
                                 
    public void startPlayer() {
        while (true) {
            current_bandwidth = getEffectiveBandwidth();
            if (current_bandwidth < 0) break; //negative when trace ends
            step();
            current_time += step_duration;
        }
    }
    
    /**
     * Things to do at each step in simulation
     * Runs every step_duration (simulated to 200 ms)
     */
    private void step() {
        //schedule chunk: periodic scheduling + random jitter
        if (!is_downloading && buffer_size < rand_target_buffer) {
            is_downloading = true;
            //select bit rate
            selected_bitrate = selectBitrate();
            //compute bits in chunk
            remaining_chunk_bits = chunk_duration*selected_bitrate.getValue();
            //add overhead (IP, TCP, DL header)
            remaining_chunk_bits += getOverhead(remaining_chunk_bits);
            //introduce random jitter by randomizing target buffer
            rand_target_buffer = updateRandTargetBuffer();
        } 
        
        //download video
        if (is_downloading) {
            remaining_chunk_bits -= step_duration*current_bandwidth;
            if (remaining_chunk_bits <= 0) {
                buffer_size += chunk_duration;
                is_downloading = false;
            }
        }
        //play video
        if (buffer_size > 0) {
            buffer_size -= step_duration;
            System.out.println(current_time + "\t" + selected_bitrate.getValue()); //log
        }
        else { buffer_size = 0; System.out.println(current_time + "\t0"); }
    }
    
    /**
     * @return video chunk bit rate selected by player
     */
    private LEVEL selectBitrate() {
        return LEVEL.LEVEL_2;
    }
    
    /**
     * @return bandwidth estimated by player
     * TODO: geometric mean / harmonic mean (suggested in 
     */
    private int estimateSelectedBandwidth() {
        return -1;
    }
    
    /**
     * @return actual bandwidth (from trace)
     */
    private int getEffectiveBandwidth() {
        int total_bandwidth = trace.getAvailableBW(current_time);
        return total_bandwidth/num_players;
    }
    
    /**
     * @return randomized target buffer between 29.5 a 30.5 seconds (in ms)
     */
    private int updateRandTargetBuffer() {
        final int avg = 30000;
        final int jitter = 1000;
        return avg + (int)((Math.random() - 0.5)*jitter);
    }
    
    /**
     * @param message_bits size of app layer message
     * @return number of bits of header fields for all generated packets
     */
    private int getOverhead(int message_bits) {
        //compute number of packets as ceiling of message_bits/(payload that fits in one packet)
        //[TCP + IP header ~ 60 bytes, MTU ~ 1500 bytes]
        int num_packets = (8*(1500-60) + message_bits - 1)/(8*(1500-60)); 
        //return num_packets*header size 
        return num_packets*(60*8);
    }
}
