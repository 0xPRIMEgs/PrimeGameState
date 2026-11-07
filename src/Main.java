import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

import PrimeGameState.*;

public class Main {
    public static void main(String[] args) {
        SetConfigFlags(FLAG_WINDOW_ALWAYS_RUN);
        InitWindow(PrimeGameState.windowWidth(), PrimeGameState.windowHeight(), "Prime Game State System");
        InitAudioDevice();

        SetTargetFPS(PrimeGameState.targetFPS());
		
		try {
			OptionsManager.loadOptions(); // throws IOException
			OptionsManager.saveOptions(); // throws IOException

			OptionsManager.applyFullscreen(); // after InitWindow
		} catch (java.io.IOException e) {
			System.err.println(e.getMessage());
		}
		
		//ControllerManager conMan = new ControllerManager();
		ControllerManager conMan = new ControllerRemapMenuState.AutoLoadControllerManager();
		PrimeGameState state = new TitleState(0, null);

        //ToggleFullscreen();
		while (state.running) {
            float dt = GetFrameTime();
			if (dt > 0.025f) {
				System.out.println("FRAME SPIKE: " + (dt * 1000) + " ms");
			}

			conMan.update(dt);
			state = state.update(conMan, dt);
			state.preDraw();	
			state.draw();
			state.postDraw();
            
        }

        CloseAudioDevice();
        CloseWindow();
		System.exit(0);
    }
}