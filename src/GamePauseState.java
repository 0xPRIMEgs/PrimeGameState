import PrimeGameState.*;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class GamePauseState extends PrimeGameStateMenu{
	public GamePauseState(int pad, Texture background, PrimeGameState prevState){
		super(pad, background, prevState);
		this.addChoice("Resume"); this.addChoice("Main Menu"); this.addChoice("Exit Game");
	}
	
	@Override
	public PrimeGameState update(ControllerManager conMan, float dt){
		this.cm = conMan;
		ControllerState cs = this.cm.getPad(this.pad);
		UpdateMusicStream(this.bgm);
		
		handleMovement(cs);
		
		// A button
		if (click(cs)) {
			PlaySound(accept);
			this.unloadAssets();
			if(this.menuChoice == 0){ if(this.prevState != null){ return this.prevState; } else{ return new TitleState(this.pad, null); } }
			if(this.menuChoice == 1) return new MainMenuState(this.pad, null, null);
			if(this.menuChoice == 2) this.running = false;
		}
		
		return this;
	}
	
	@Override
	public void draw(){
		DrawTintOverScreen(DARKGRAY);
		DrawTintOverScreen(this.getPadColor());
		
		this.drawHeader("PrimeGameState Demo A | By William Starkovich | Pause Screen | John 3:16 | ");
		this.drawFooter("Press Up/Down on the DPAD to choose. Press (A) to select.");
		this.drawActive(WIDTH - 48, 8);
		
		this.drawMenuA();
	}
	
	@Override
	public void unloadAssets(){
		super.unloadAssets();
	}
}