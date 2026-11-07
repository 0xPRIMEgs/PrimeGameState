package PrimeGameState;

import static com.raylib.Raylib.*;

public class StickState {
	int pad = -1;
	int axisX = -1;
	int axisY = -1;

	float prevX = 0.0f;
	float prevY = 0.0f;

	float x = 0.0f;
	float y = 0.0f;
	
	int ix = 0;
	int iy = 0;

	float heldZone = 1.0f;
	float deadZone = 0.2f;

	boolean fromNeutral = false;
	float neutralTime = 0.0f;
	float tiledTime = 0.0f;

	public StickState(int pad, int ax, int ay) {
		this.pad = pad;
		this.axisX = ax;
		this.axisY = ay;
	}

	public int pad() { return this.pad; }
	public StickState pad(int value) { this.pad = value; return this; }

	public int axisX() { return this.axisX; }
	public StickState axisX(int value) { this.axisX = value; return this; }

	public int axisY() { return this.axisY; }
	public StickState axisY(int value) { this.axisY = value; return this; }

	public float prevX() { return this.prevX; }
	public StickState prevX(float value) { this.prevX = value; return this; }

	public float prevY() { return this.prevY; }
	public StickState prevY(float value) { this.prevY = value; return this; }

	public float x() { return this.x; }
	public StickState x(float value) { this.x = value; return this; }

	public float y() { return this.y; }
	public StickState y(float value) { this.y = value; return this; }

	public float heldZone() { return this.heldZone; }
	public StickState heldZone(float value) { this.heldZone = value; return this; }

	public float deadZone() { return this.deadZone; }
	public StickState deadZone(float value) { this.deadZone = value; return this; }

	public boolean fromNeutral() { return this.fromNeutral; }
	public StickState fromNeutral(boolean value) { this.fromNeutral = value; return this; }

	public float neutralTime() { return this.neutralTime; }
	public StickState neutralTime(float value) { this.neutralTime = value; return this; }

	public float tiledTime() { return this.tiledTime; }
	public StickState tiledTime(float value) { this.tiledTime = value; return this; }
	
	public int ix(){ return this.ix; }
	public int iy(){ return this.iy; }

	public void update(float dt) {
		this.fromNeutral = false;
		
		this.prevX = this.x;
		this.prevY = this.y;

		this.ix = 0;
		this.x = GetGamepadAxisMovement(this.pad, this.axisX);
		if (Math.abs(this.x) < this.deadZone()) this.x = 0.0f;
	
		if(this.x > 0.0f) this.ix = 1;
		else if(this.x < 0.0f) this.ix = -1;
		
		this.iy = 0;
		this.y = GetGamepadAxisMovement(this.pad, this.axisY);
		if (Math.abs(this.y) < this.deadZone()) this.y = 0.0f;
		
		if(this.y > 0.0f) this.iy = 1;
		else if(this.y < 0.0f) this.iy = -1;

		if (this.x == 0.0f && this.y == 0.0f) {
			this.neutralTime += dt;
			this.tiledTime = 0.0f;
		} else {
			if (this.prevX == 0.0f && this.prevY == 0.0f) this.fromNeutral = true;
			this.neutralTime = 0.0f;
			this.tiledTime += dt;
		}
	}
}