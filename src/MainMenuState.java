import PrimeGameState.*;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class MainMenuState extends PrimeGameStateMenu{
	public MainMenuState(int pad, Texture background, PrimeGameState prevState){
		super(pad, background, prevState);
		this.background = LoadTexture("assets/menubg/a.png");
		this.addChoice("Single Player"); this.addChoice("Multiplayer"); this.addChoice("Options"); this.addChoice("Exit"); 
		
		/*this.addChoice("Fullscreen", false);
		
		Vector<String> strs = new Vector<String>();
		strs.add("Jesus"); strs.add("Is"); strs.add("Lord");
		this.addChoice("Test", strs);
		
		this.addChoice("Controller Remap");*/
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
			
			if(this.menuChoice == 0){ this.unloadAssets(); return new Game(this.pad, null); }
			if(this.menuChoice == 1){ this.unloadAssets(); return new TitleState(this.pad, null); }
			if(this.menuChoice == 2){ return new OptionsMenuState(this.pad, null, this); }
			if(this.menuChoice == 3){ this.unloadAssets(); this.running = false; }
			if(this.menuChoice == 4){
				ToggleFullscreen();
				this.choice(this.menuChoice).setToggle(IsWindowFullscreen());
			}
			if(this.menuChoice == 6) return new ControllerRemapMenuState(this.pad, this.snapshot(), this);
		}
		
		return this;
	}
	
	@Override
	public void draw(){
		DrawTintOverScreen(DARKGRAY);
		DrawTintOverScreen(this.getPadColor());
		
		this.drawHeader("PrimeGameState Demo A | By William Starkovich | Main Menu | John 3:16 | ");
		this.drawFooter("Press Up/Down on the DPAD to choose. Press (A) to select.");
		this.drawActive(WIDTH - 48, 8);
		
		this.drawMenuA();
	}
	
	@Override
	public void unloadAssets(){
		super.unloadAssets();
	}
}