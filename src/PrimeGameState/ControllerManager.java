package PrimeGameState;

import static com.raylib.Raylib.*;
import java.util.HashMap;

public class ControllerManager {
    HashMap<Integer, ControllerState> controllerStates = new HashMap<>();
    int maxPlayers = 4;

    public ControllerManager() {}

    public int maxPlayers() { return this.maxPlayers; }
    public ControllerManager maxPlayers(int i) {
        this.maxPlayers = i;
        return this;
    }

    public ControllerState getPad(int i) {
		ControllerState c = this.controllerStates.get(i);
		if(c == null){
			c = new ControllerState(i);
            this.controllerStates.put(c.pad(), c);
		}
        return c;
    }

    public void update(float dt) {
        for (int i = 0; i < 16; i++) {
            if (IsGamepadAvailable(i) && this.getPad(i) == null) {
                if (this.controllerStates.size() < this.maxPlayers) {
                    ControllerState cs = new ControllerState(i);
                    this.controllerStates.put(cs.pad(), cs);
                }
            }
        }

        for (ControllerState c : this.controllerStates.values()) {
            c.update(dt);
        }
    }
}