package dashsimulation;

import java.util.ArrayList;
import java.util.List;

/**
 * to be used when simulating multiple players sharing bottleneck link
 */
public class Simulation {
    Trace trace;
    List<DashPlayer> player;
    int num_players;
    
    public Simulation(int num_players, Trace trace) {
        this.player = new ArrayList<>(num_players);
        this.num_players = num_players;
        this.trace = trace;
        
        for (int i = 0; i < this.num_players; i++) {
            player.set(i, new DashPlayer(num_players, trace));
        }
    }
    
    public void startSimulation() {
        for (int i = 0; i < this.num_players; i++) {
            System.out.println("=====PLAYER " + (i+1) + "=====");
            player.get(i).startPlayer();
            System.out.println();
        }
    }
}
