package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class MenuItemState{
	String label = "ERROR";
	Vector<String> varLabels = new Vector<String>();
	int varIndex = 0;
	boolean useVars = false;
	boolean useToggle = false;
	boolean toggle = false;
	
	int vertTextMod = 4;
	
	public MenuItemState(String label){
		this.label = label;
	}
	
	public int varIndex(){ return this.varIndex; }
	public boolean toggle(){ return this.toggle; }
	public boolean vars(){ return this.useVars; }
	
	public MenuItemState addVar(String varLabel){
		this.varLabels.add(varLabel);
		this.useVars = true;
		this.useToggle = false;
		return this;
	}
	
	public MenuItemState setToggle(boolean b){
		this.useToggle = true;
		this.toggle = b;
		this.useVars = false;
		return this;
	}
	
	public MenuItemState incVarIndex(){
		this.varIndex += 1;
		if(this.varIndex >= this.varLabels.size()) this.varIndex = this.varLabels.size() - 1;
		return this;
	}
	
	public MenuItemState decVarIndex(){
		this.varIndex -= 1;
		if(this.varIndex < 0) this.varIndex = 0;
		return this;
	}
	
	public static void DrawSlantedRect(float x, float y, float width, float height, float slant, Color color) {
		Vector2 a = new Vector2().x(x + slant).y(y);
		Vector2 b = new Vector2().x(x + width).y(y);
		Vector2 c = new Vector2().x(x + width - slant).y(y + height);
		Vector2 d = new Vector2().x(x).y(y + height);

		DrawTriangle(a, c, b, color);
		DrawTriangle(a, d, c, color);
	}
	
	public void draw(int startX, int startY){
		DrawSlantedRect(startX, startY, 256, 18, 10, BLACK);
		DrawText(this.label, startX + 32, startY + vertTextMod, 10, WHITE);
		if(this.useToggle) DrawCircle(startX + (256 - 24), startY + 9, 6, this.toggle ? GREEN : WHITE);
		if(this.useVars) DrawText(this.varLabels.get(this.varIndex), startX + 196, startY + vertTextMod, 10, SKYBLUE);
	}
}