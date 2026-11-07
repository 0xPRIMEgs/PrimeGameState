package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class PrimeGameStateMenu extends PrimeGameState{
	protected ControllerManager cm;
	protected int menuChoice = 0;
	protected int infoBarSize = 32;
	
	protected Music bgm;
	protected Sound move;
	protected Sound accept;
	
	
	protected Vector<MenuItemState> choices = new Vector<MenuItemState>();
	
	protected PrimeGameState prevState; // for Pause Menus
	
	public PrimeGameStateMenu(int pad, Texture background, PrimeGameState prevState){
		super(pad, background);
		this.prevState = prevState;
		this.bgm = LoadMusicStream("assets/bgm.ogg");
		this.move = LoadSound("assets/move.ogg");
		this.accept = LoadSound("assets/accept.ogg");
		
		//OptionsMenuState.applyMusicVolume(this.bgm);
		//OptionsMenuState.applySoundVolume(this.move);
		//OptionsMenuState.applySoundVolume(this.accept);
		
		OptionsManager.applyMusicVolume(this.bgm);
		OptionsManager.applySoundVolume(this.move);
		OptionsManager.applySoundVolume(this.accept);
		
		PlayMusicStream(this.bgm);
	}
	
	public void addChoice(String text){ this.choices.add(new MenuItemState(text)); }
	public void addChoice(String text, boolean toggle){ this.choices.add(new MenuItemState(text).setToggle(toggle)); }
	public void addChoice(String text, Vector<String> varLabels){ 
		MenuItemState mis = new MenuItemState(text);
		for(String label : varLabels){
			mis.addVar(label);
		}
		this.choices.add(mis); 
	}
	
	public MenuItemState choice(int i){ return this.choices.get(i); }
	
	public void handleMovement(ControllerState cs){
		if ((cs.leftStick.iy() < 0 && cs.leftStick().fromNeutral()) || cs.pressedUp()) {
			this.menuChoice -= 1;
			PlaySound(this.move);
		}
		
		if ((cs.leftStick.iy() > 0 && cs.leftStick().fromNeutral()) || cs.pressedDown()) {
			this.menuChoice += 1;
			PlaySound(this.move);
		}
		
		if(this.menuChoice < 0) this.menuChoice = 0;
		if(this.menuChoice >= this.choices.size()) this.menuChoice = this.choices.size() - 1;
		
		if(choices.get(this.menuChoice).vars()){
			if ((cs.leftStick.ix() < 0 && cs.leftStick().fromNeutral()) || cs.pressedLeft()) {
				choices.get(this.menuChoice).decVarIndex();
				PlaySound(this.move);
			}
			
			if ((cs.leftStick.ix() > 0 && cs.leftStick().fromNeutral()) || cs.pressedRight()) {
				choices.get(this.menuChoice).incVarIndex();
				PlaySound(this.move);
			}
		}
	}
	
	public boolean click(ControllerState cs){
		return cs.pressedA();
	}
	
	public void drawHeader(String text){
		DrawRectangle(0, 0, WIDTH, infoBarSize, Fade(BLACK, 0.5f));
		DrawText(text, 8, 8, 16, WHITE);
	}
	
	public void drawFooter(String text){
		DrawRectangle(0, HEIGHT - infoBarSize, WIDTH, infoBarSize, Fade(BLACK, 0.5f));
		DrawText(text, 8, HEIGHT - 16, 16, WHITE);
	}
	
	public void drawActive(int startX, int startY){
		if(this.cm != null){
			for(int i = 0; i < this.cm.maxPlayers(); i++){
				if(cm.getPad(i).active()) DrawCircle(startX + (6 * i), startY, 4, this.getPadColor(i));
			}
		}
	}
	
	public void drawMenuA(){
		for(int i = 0; i < this.choices.size(); i++){
			this.choices.get(i).draw(32, infoBarSize + 32 + (20 * i));
		}
		
		DrawText(">>>>", 48, infoBarSize + 32 + (20 * this.menuChoice) + 4, 10, WHITE);
	}
	
	@Override
	public void unloadAssets(){
		super.unloadAssets();
		UnloadSound(move);
		UnloadSound(accept);
		UnloadMusicStream(bgm);
	}
}