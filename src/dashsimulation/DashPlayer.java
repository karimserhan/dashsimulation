package dashsimulation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

//bitrate levels in kbps
enum LEVEL {
    LEVEL_1(250,1), LEVEL_2(500,2), LEVEL_3(750,3), LEVEL_4(1000,4), LEVEL_5(1500,5), LEVEL_6(3000,6);
    
    private final int value;
    private final int level;
    private LEVEL(final int value, final int level) { this.value = value; this.level = level; }
    /**
     * @return bit rate in kbps of level
     */
    public int getValue() { return value; }
    /**
     * @return level
     */
    public int getLevel() { return level; }
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

    // used to calculate geometric mean of time history
    long bandwidth_history_product = 1; // product of all bandwidths recorded
    long bandwidth_history_length = 0; // number of bandwidths recorded

    //used to calculate bandwidth utilization
    int bandwidth_sum = 0; //sum of all bandwidths observed
    int bitrate_sum = 0;  //sum of all bitrates
    
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
        System.out.println("Bandwidth utilization = " + (bitrate_sum*100.0/bandwidth_sum + "%"));
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
        bandwidth_sum += current_bandwidth;
        bitrate_sum += selected_bitrate.getValue();
        if (buffer_size > 0) {
            buffer_size -= step_duration;
            System.out.println(current_time + "\t" + selected_bitrate.getLevel()); //log
        }
        else { buffer_size = 0; System.out.println(current_time + "\t0"); }
    }
    
    /**
     * Returns selected bitrate.
     *
     * If the current time is less than some constant, uses capacity
     * estimation to determine best bitrate.
     *
     * Otherwise, uses a buffer-based approach. If the buffer is smaller than
     * some constant, selects minimum bitrate. If the buffer is larger than
     * some constant, selects maximum bitrate. Otherwise, slowly increases
     * the bitrate proportional to buffer size.
     *
     * @return video chunk bit rate selected by player
     */
    private LEVEL selectBitrate() {
        // TODO adjust these constants
        // for time in [0, buffer_buildup_time), use capacity estimation
        final int buffer_buildup_time = 1000; // milliseconds
        // if buffer size < min_buffer_size, use minimum bitrate
        final int min_buffer_size = 1000; // milliseconds
        // if buffer size > max_buffer_size, use maximum bitrate
        final int max_buffer_size = rand_target_buffer; // milliseconds

        if (current_time < buffer_buildup_time) {
            return getNextLowestBitrate(getEstimatedCapacity());
        }
        else if (buffer_size < min_buffer_size) {
            return LEVEL.LEVEL_1;
        }
        else if (buffer_size > max_buffer_size) {
            return LEVEL.LEVEL_6;
        }
        else {
            // TODO adjust the 1.0
            return getNextLowestBitrate(
                    (int)Math.round(
                      LEVEL.LEVEL_1.getValue() + 10.0 * Math.sqrt(buffer_size)
                    )
            );
        }
    }
    
    /**
     * Returns estimated bandwidth capacity.
     *
     * Calculates geometric mean of all past bandwidth measurements.
     *
     * @return bandwidth estimated by player
     */
    private int getEstimatedCapacity() {
        bandwidth_history_product *= current_bandwidth;
        bandwidth_history_length += 1;
        return (int)Math.round(Math.pow(bandwidth_history_product,
                                        1.0 / bandwidth_history_length));
    }

    /**
     * Returns next lowest bitrate according to LEVEL enum.
     *
     * @return next lowest bitrate
     */
    private LEVEL getNextLowestBitrate(int bitrate) {
        if (bitrate < LEVEL.LEVEL_2.getValue())
            return LEVEL.LEVEL_1;
        else if (bitrate < LEVEL.LEVEL_3.getValue())
            return LEVEL.LEVEL_2;
        else if (bitrate < LEVEL.LEVEL_4.getValue())
            return LEVEL.LEVEL_3;
        else if (bitrate < LEVEL.LEVEL_5.getValue())
            return LEVEL.LEVEL_4;
        else if (bitrate < LEVEL.LEVEL_6.getValue())
            return LEVEL.LEVEL_5;
        else
            return LEVEL.LEVEL_6;
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
