package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class PrimeGameState{
	Rectangle src;
	Rectangle dst;
	Vector2 origin;
	RenderTexture screen;
	
	public static final int WIDTH = 640;
    public static final int HEIGHT = 360;
	public static final float scale = 0.5f;
	public static final int windowWidth = (int)(1280 * scale);
	public static final int windowHeight = (int)(720 * scale);
	public static final int targetFPS = 60;

    public static final float DEADZONE = 0.25f;
	public boolean running = true;
	
	protected Texture background;
	protected int pad = 0;
	
	public PrimeGameState(int pad, Texture background){
		this.pad = pad;
		this.background = background;
		this.src = new Rectangle().x(0).y(0).width(WIDTH).height(-HEIGHT);
		this.dst = new Rectangle().x(0).y(0).width(windowWidth).height(windowHeight);
		this.origin = new Vector2().x(0).y(0);
		
		this.screen = LoadRenderTexture(WIDTH, HEIGHT);
        SetTextureFilter(this.screen.texture(), TEXTURE_FILTER_POINT);
	}
	
	public PrimeGameState update(ControllerManager conMan, float dt){
		return this;
	}
	
	public void preDraw() {
		BeginTextureMode(this.screen);
		if(this.background != null) DrawTexture(this.background, 0, 0, WHITE);
	}
	
	public void draw(){
		
	}
	
	public void postDraw(){
		EndTextureMode();

		// Scale to fullscreen
		BeginDrawing();
			ClearBackground(BLACK);

			dst.width(GetScreenWidth());
			dst.height(GetScreenHeight());

			DrawTexturePro(this.screen.texture(), this.src, this.dst, this.origin, 0, WHITE);
		EndDrawing();
	}
	
	public static int windowWidth(){ return windowWidth; }
	public static int windowHeight(){ return windowHeight; }
	public static int targetFPS(){ return targetFPS; };
	
	
		
	public static void DrawTintOverScreen(Color c){
		BeginBlendMode(BLEND_MULTIPLIED);
			DrawRectangle(0, 0, WIDTH, HEIGHT, c);
		EndBlendMode();
	}
	
	public Color getPadColor(){
		return getPadColor(this.pad);
	}
	
	public Color getPadColor(int i){
		Color[] ca = new Color[]{RED,BLUE,GREEN,YELLOW};
		if(i < 0) return PURPLE;
		if(i >= ca.length) return PURPLE;
		return ca[i];
	}
	
	public PrimeGameState background(Texture t){ this.background = t; return this; }
	
	public void unloadAssets(){
		UnloadRenderTexture(screen);
		if(this.background != null) UnloadTexture(this.background);
	}
	
	public Texture snapshot() {
		Image image = LoadImageFromTexture(screen.texture());
		ImageFlipVertical(image);

		Texture result = LoadTextureFromImage(image);
		UnloadImage(image);

		return result;
	}
}