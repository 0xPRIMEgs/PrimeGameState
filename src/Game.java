import PrimeGameState.*;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class Game extends PrimeGameState{
	Sound accept;
	
	int px, py, psize;
	
	public Game(int pad, Texture background){
		super(pad, background);
		accept = LoadSound("assets/accept.ogg");
		
		this.px = (int)(WIDTH * 0.5);
		this.py = (int)(HEIGHT * 0.5);
		this.psize = 16;
	}
	
	@Override
	public PrimeGameState update(ControllerManager conMan, float dt){		
		if (IsGamepadAvailable(this.pad)) {			
			this.px += conMan.getPad(this.pad).leftStick().ix();
			this.py += conMan.getPad(this.pad).leftStick().iy();
	
			// A button
			if (conMan.getPad(this.pad).pressedA()) {
				PlaySound(accept);
			}
			
			if (conMan.getPad(this.pad).pressedPlus()) {
				return new GamePauseState(this.pad, this.snapshot(), this);
			}
		}
		
		return this;
	}
	
	@Override
	public void draw(){
		DrawRectangle(0, 0, WIDTH, HEIGHT, WHITE); //ClearBackground(BLACK); magiclly ClearBackground does NOT get respected if Game has a background texture. This is the fix.
		DrawRectangle(this.px, this.py, this.psize, this.psize, this.getPadColor());
	}
	
	@Override
	public void unloadAssets(){
		super.unloadAssets();
		UnloadSound(accept);
	}
}