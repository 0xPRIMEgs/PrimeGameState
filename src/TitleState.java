import PrimeGameState.*;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class TitleState extends PrimeGameStateMenu{

	public TitleState(int pad, Texture background){
		super(pad, background, null);
		this.background = LoadTexture("assets/menubg/b.png");
	}
	
	@Override
	public PrimeGameState update(ControllerManager conMan, float dt){	
		this.cm = conMan;
		// A button
		if (this.cm.getPad(this.pad).pressedA()) {
			PlaySound(accept);
			this.unloadAssets();
			//Texture bg = LoadTexture("assets/menubg/a.png");
			return new MainMenuState(this.pad, null, null);
		}
		
		return this;
	}
	
	@Override
	public void draw(){
		ClearBackground(BLACK);
		this.drawHeader("PrimeGameState Demo A | By William Starkovich | Title Menu | John 3:16 | ");
		this.drawFooter("Hold (+) to Join. Press (A) to continue.");
		this.drawActive(WIDTH - 48, 8);
	}
	
	@Override
	public void unloadAssets(){
		super.unloadAssets();
	}
}