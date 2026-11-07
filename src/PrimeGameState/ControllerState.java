package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import com.raylib.Raylib;
import java.util.Vector;

public class ControllerState {

    /** Logical outputs and physical sources. Plus and minus are deliberately excluded. */
    public enum Button {
        A(GAMEPAD_BUTTON_RIGHT_FACE_DOWN),
        B(GAMEPAD_BUTTON_RIGHT_FACE_RIGHT),
        X(GAMEPAD_BUTTON_RIGHT_FACE_LEFT),
        Y(GAMEPAD_BUTTON_RIGHT_FACE_UP),
        LB(GAMEPAD_BUTTON_LEFT_TRIGGER_1),
        RB(GAMEPAD_BUTTON_RIGHT_TRIGGER_1),
        LT(GAMEPAD_BUTTON_LEFT_TRIGGER_2),
        RT(GAMEPAD_BUTTON_RIGHT_TRIGGER_2),
        Up(GAMEPAD_BUTTON_LEFT_FACE_UP),
        Down(GAMEPAD_BUTTON_LEFT_FACE_DOWN),
        Left(GAMEPAD_BUTTON_LEFT_FACE_LEFT),
        Right(GAMEPAD_BUTTON_LEFT_FACE_RIGHT),
        LS(GAMEPAD_BUTTON_LEFT_THUMB),
        RS(GAMEPAD_BUTTON_RIGHT_THUMB),
        Middle(GAMEPAD_BUTTON_MIDDLE);
        final int raylibButton;
        Button(int raylibButton) { this.raylibButton = raylibButton; }
    }

    private final Button[] buttonMap = Button.values();
    private boolean swapSticks = false;

    /** mapButton(A, B): physical B drives logical A. Other mappings stay unchanged. */
    public ControllerState mapButton(Button output, Button source) {
        java.util.Objects.requireNonNull(output, "output");
        java.util.Objects.requireNonNull(source, "source");
        this.buttonMap[output.ordinal()] = source;
        return this;
    }
    public Button mappedButton(Button output) {
        return this.buttonMap[java.util.Objects.requireNonNull(output, "output").ordinal()];
    }
    /** Exchanges the current sources for two logical outputs. */
    public ControllerState swapButtons(Button a, Button b) {
        Button sourceA = this.mappedButton(a);
        Button sourceB = this.mappedButton(b);
        this.mapButton(a, sourceB);
        return this.mapButton(b, sourceA);
    }
    private boolean buttonDown(Button output) {
        return IsGamepadButtonDown(this.pad, this.mappedButton(output).raylibButton);
    }
    public boolean swapSticks() { return this.swapSticks; }
    public ControllerState swapSticks(boolean value) {
        this.swapSticks = value;
        this.leftStick.axisX(value ? GAMEPAD_AXIS_RIGHT_X : GAMEPAD_AXIS_LEFT_X);
        this.leftStick.axisY(value ? GAMEPAD_AXIS_RIGHT_Y : GAMEPAD_AXIS_LEFT_Y);
        this.rightStick.axisX(value ? GAMEPAD_AXIS_LEFT_X : GAMEPAD_AXIS_RIGHT_X);
        this.rightStick.axisY(value ? GAMEPAD_AXIS_LEFT_Y : GAMEPAD_AXIS_RIGHT_Y);
        return this;
    }
    /** Settings apply to the logical stick, including after swapping sticks. */
    public ControllerState invertLeftX(boolean value) { ((MappedStickState)this.leftStick).invertX = value; return this; }
    public ControllerState invertLeftY(boolean value) { ((MappedStickState)this.leftStick).invertY = value; return this; }
    public ControllerState invertRightX(boolean value) { ((MappedStickState)this.rightStick).invertX = value; return this; }
    public ControllerState invertRightY(boolean value) { ((MappedStickState)this.rightStick).invertY = value; return this; }
    public boolean invertLeftX() { return ((MappedStickState)this.leftStick).invertX; }
    public boolean invertLeftY() { return ((MappedStickState)this.leftStick).invertY; }
    public boolean invertRightX() { return ((MappedStickState)this.rightStick).invertX; }
    public boolean invertRightY() { return ((MappedStickState)this.rightStick).invertY; }
    public ControllerState swapLeftAxes(boolean value) { ((MappedStickState)this.leftStick).swapAxes = value; return this; }
    public ControllerState swapRightAxes(boolean value) { ((MappedStickState)this.rightStick).swapAxes = value; return this; }
    public boolean swapLeftAxes() { return ((MappedStickState)this.leftStick).swapAxes; }
    public boolean swapRightAxes() { return ((MappedStickState)this.rightStick).swapAxes; }
    public ControllerState resetMappings() {
        for (Button button : Button.values()) this.mapButton(button, button);
        return this.swapSticks(false).invertLeftX(false).invertLeftY(false)
            .invertRightX(false).invertRightY(false).swapLeftAxes(false).swapRightAxes(false);
    }

    // Retains StickState's dead zones, timers and neutral detection. Keeps
    // previous coordinates in logical space after transforming the new sample.
    private static class MappedStickState extends StickState {
        boolean invertX, invertY, swapAxes;
        MappedStickState(int pad, int axisX, int axisY) { super(pad, axisX, axisY); }
        @Override public void update(float dt) {
            float oldX = this.x(), oldY = this.y();
            int ax = this.axisX(), ay = this.axisY();
            if (this.swapAxes) { this.axisX(ay); this.axisY(ax); }
            // Neutral detection only checks zero, so sign inversion does not affect it.
            super.update(dt);
            this.axisX(ax); this.axisY(ay);
            if (this.invertX) { this.x(-this.x()); this.ix = -this.ix; }
            if (this.invertY) { this.y(-this.y()); this.iy = -this.iy; }
            this.prevX(oldX); this.prevY(oldY);
        }
    }

    float heldLS = 0.0f, prevHeldLS = 0.0f;
    float heldRS = 0.0f, prevHeldRS = 0.0f;
    float heldMiddle = 0.0f, prevHeldMiddle = 0.0f;
    int pad = 0;
    boolean active = false;

	float heldZone = 1.0f;
    float deadZone = 0.2f;

    StickState leftStick;
	StickState rightStick;

    float prevHeldA = 0.0f;
    float prevHeldB = 0.0f;
    float prevHeldX = 0.0f;
    float prevHeldY = 0.0f;

    float prevHeldLB = 0.0f;
    float prevHeldRB = 0.0f;
    float prevHeldLT = 0.0f;
    float prevHeldRT = 0.0f;

    float prevHeldUp = 0.0f;
    float prevHeldDown = 0.0f;
    float prevHeldLeft = 0.0f;
    float prevHeldRight = 0.0f;

    float prevHeldPlus = 0.0f;
    float prevHeldMinus = 0.0f;

    float heldA = 0.0f;
    float heldB = 0.0f;
    float heldX = 0.0f;
    float heldY = 0.0f;

    float heldLB = 0.0f;
    float heldRB = 0.0f;
    float heldLT = 0.0f;
    float heldRT = 0.0f;

    float heldUp = 0.0f;
    float heldDown = 0.0f;
    float heldLeft = 0.0f;
    float heldRight = 0.0f;

    float heldPlus = 0.0f;
    float heldMinus = 0.0f;
	
	public ControllerState(int pad){ 
		this.pad = pad; 
		this.leftStick = new MappedStickState(this.pad, GAMEPAD_AXIS_LEFT_X, GAMEPAD_AXIS_LEFT_Y);
		this.rightStick = new MappedStickState(this.pad, GAMEPAD_AXIS_RIGHT_X, GAMEPAD_AXIS_RIGHT_Y);
	}

    public ControllerState update(float dt) {
        if(this.active) {
            this.leftStick.update(dt);
			this.rightStick.update(dt);

            this.prevHeldLS = this.heldLS;
            if (this.buttonDown(Button.LS)) this.heldLS += dt;
            else this.heldLS = 0.0f;

            this.prevHeldRS = this.heldRS;
            if (this.buttonDown(Button.RS)) this.heldRS += dt;
            else this.heldRS = 0.0f;

            this.prevHeldMiddle = this.heldMiddle;
            if (this.buttonDown(Button.Middle)) this.heldMiddle += dt;
            else this.heldMiddle = 0.0f;

            this.prevHeldA = this.heldA;
            this.prevHeldB = this.heldB;
            this.prevHeldX = this.heldX;
            this.prevHeldY = this.heldY;

            this.prevHeldLB = this.heldLB;
            this.prevHeldRB = this.heldRB;
            this.prevHeldLT = this.heldLT;
            this.prevHeldRT = this.heldRT;

            this.prevHeldUp = this.heldUp;
            this.prevHeldDown = this.heldDown;
            this.prevHeldLeft = this.heldLeft;
            this.prevHeldRight = this.heldRight;

            if(this.buttonDown(Button.A)) this.heldA += dt;
            else this.heldA = 0.0f;

            if(this.buttonDown(Button.B)) this.heldB += dt;
            else this.heldB = 0.0f;

            if(this.buttonDown(Button.X)) this.heldX += dt;
            else this.heldX = 0.0f;

            if(this.buttonDown(Button.Y)) this.heldY += dt;
            else this.heldY = 0.0f;

            if(this.buttonDown(Button.LB)) this.heldLB += dt;
            else this.heldLB = 0.0f;

            if(this.buttonDown(Button.RB)) this.heldRB += dt;
            else this.heldRB = 0.0f;

            if(this.buttonDown(Button.LT)) this.heldLT += dt;
            else this.heldLT = 0.0f;

            if(this.buttonDown(Button.RT)) this.heldRT += dt;
            else this.heldRT = 0.0f;

            if(this.buttonDown(Button.Up)) this.heldUp += dt;
            else this.heldUp = 0.0f;

            if(this.buttonDown(Button.Down)) this.heldDown += dt;
            else this.heldDown = 0.0f;

            if(this.buttonDown(Button.Left)) this.heldLeft += dt;
            else this.heldLeft = 0.0f;

            if(this.buttonDown(Button.Right)) this.heldRight += dt;
            else this.heldRight = 0.0f;
			
			if(this.heldMinus > heldZone){ this.active = false; }
		}
		
		else if(this.heldPlus > heldZone){ this.active = true; }
		
		this.prevHeldPlus = this.heldPlus;
        this.prevHeldMinus = this.heldMinus;

		if(IsGamepadButtonDown(this.pad, GAMEPAD_BUTTON_MIDDLE_RIGHT)) this.heldPlus += dt;
		else this.heldPlus = 0.0f;

		if(IsGamepadButtonDown(this.pad, GAMEPAD_BUTTON_MIDDLE_LEFT)) this.heldMinus += dt;
		else this.heldMinus = 0.0f;

        return this;
    }

        public int pad() { return this.pad; }
		public ControllerState pad(int i) {
			this.pad = i;
			this.leftStick.pad(i);
			this.rightStick.pad(i);
			return this;
		}

		public float deadZone() { return this.deadZone; }
		public ControllerState deadZone(float f) {
			this.deadZone = f;
			this.leftStick.deadZone(f);
			this.rightStick.deadZone(f);
			return this;
		}

		public float heldZone() { return this.heldZone; }
		public ControllerState heldZone(float f) {
			this.heldZone = f;
			this.leftStick.heldZone(f);
			this.rightStick.heldZone(f);
			return this;
		}

		public boolean active() { return this.active; }
		public ControllerState active(boolean value) {
			this.active = value;
			return this;
		}

		public StickState leftStick() { return this.leftStick; }
		public StickState rightStick() { return this.rightStick; }

		public boolean pressedA() { return this.heldA > 0.0f && this.prevHeldA == 0.0f; }
		public boolean releasedA() { return this.heldA == 0.0f && this.prevHeldA > 0.0f; }
		public boolean holdingA() { return this.heldA > 0.0f && this.prevHeldA > 0.0f; }

		public boolean pressedB() { return this.heldB > 0.0f && this.prevHeldB == 0.0f; }
		public boolean releasedB() { return this.heldB == 0.0f && this.prevHeldB > 0.0f; }
		public boolean holdingB() { return this.heldB > 0.0f && this.prevHeldB > 0.0f; }

		public boolean pressedX() { return this.heldX > 0.0f && this.prevHeldX == 0.0f; }
		public boolean releasedX() { return this.heldX == 0.0f && this.prevHeldX > 0.0f; }
		public boolean holdingX() { return this.heldX > 0.0f && this.prevHeldX > 0.0f; }

		public boolean pressedY() { return this.heldY > 0.0f && this.prevHeldY == 0.0f; }
		public boolean releasedY() { return this.heldY == 0.0f && this.prevHeldY > 0.0f; }
		public boolean holdingY() { return this.heldY > 0.0f && this.prevHeldY > 0.0f; }

		public boolean pressedLB() { return this.heldLB > 0.0f && this.prevHeldLB == 0.0f; }
		public boolean releasedLB() { return this.heldLB == 0.0f && this.prevHeldLB > 0.0f; }
		public boolean holdingLB() { return this.heldLB > 0.0f && this.prevHeldLB > 0.0f; }

		public boolean pressedRB() { return this.heldRB > 0.0f && this.prevHeldRB == 0.0f; }
		public boolean releasedRB() { return this.heldRB == 0.0f && this.prevHeldRB > 0.0f; }
		public boolean holdingRB() { return this.heldRB > 0.0f && this.prevHeldRB > 0.0f; }

		public boolean pressedLT() { return this.heldLT > 0.0f && this.prevHeldLT == 0.0f; }
		public boolean releasedLT() { return this.heldLT == 0.0f && this.prevHeldLT > 0.0f; }
		public boolean holdingLT() { return this.heldLT > 0.0f && this.prevHeldLT > 0.0f; }

		public boolean pressedRT() { return this.heldRT > 0.0f && this.prevHeldRT == 0.0f; }
		public boolean releasedRT() { return this.heldRT == 0.0f && this.prevHeldRT > 0.0f; }
		public boolean holdingRT() { return this.heldRT > 0.0f && this.prevHeldRT > 0.0f; }

		public boolean pressedUp() { return this.heldUp > 0.0f && this.prevHeldUp == 0.0f; }
		public boolean releasedUp() { return this.heldUp == 0.0f && this.prevHeldUp > 0.0f; }
		public boolean holdingUp() { return this.heldUp > 0.0f && this.prevHeldUp > 0.0f; }

		public boolean pressedDown() { return this.heldDown > 0.0f && this.prevHeldDown == 0.0f; }
		public boolean releasedDown() { return this.heldDown == 0.0f && this.prevHeldDown > 0.0f; }
		public boolean holdingDown() { return this.heldDown > 0.0f && this.prevHeldDown > 0.0f; }

		public boolean pressedLeft() { return this.heldLeft > 0.0f && this.prevHeldLeft == 0.0f; }
		public boolean releasedLeft() { return this.heldLeft == 0.0f && this.prevHeldLeft > 0.0f; }
		public boolean holdingLeft() { return this.heldLeft > 0.0f && this.prevHeldLeft > 0.0f; }

		public boolean pressedRight() { return this.heldRight > 0.0f && this.prevHeldRight == 0.0f; }
		public boolean releasedRight() { return this.heldRight == 0.0f && this.prevHeldRight > 0.0f; }
		public boolean holdingRight() { return this.heldRight > 0.0f && this.prevHeldRight > 0.0f; }

		public boolean pressedPlus() { return this.heldPlus > 0.0f && this.prevHeldPlus == 0.0f; }
		public boolean releasedPlus() { return this.heldPlus == 0.0f && this.prevHeldPlus > 0.0f; }
		public boolean holdingPlus() { return this.heldPlus > 0.0f && this.prevHeldPlus > 0.0f; }

		public boolean pressedMinus() { return this.heldMinus > 0.0f && this.prevHeldMinus == 0.0f; }
		public boolean releasedMinus() { return this.heldMinus == 0.0f && this.prevHeldMinus > 0.0f; }
		public boolean holdingMinus() { return this.heldMinus > 0.0f && this.prevHeldMinus > 0.0f; }
		
		public boolean pressedLS() { return this.heldLS > 0.0f && this.prevHeldLS == 0.0f; }
		public boolean releasedLS() { return this.heldLS == 0.0f && this.prevHeldLS > 0.0f; }
		public boolean holdingLS() { return this.heldLS > 0.0f && this.prevHeldLS > 0.0f; }
		
		public boolean pressedRS() { return this.heldRS > 0.0f && this.prevHeldRS == 0.0f; }
		public boolean releasedRS() { return this.heldRS == 0.0f && this.prevHeldRS > 0.0f; }
		public boolean holdingRS() { return this.heldRS > 0.0f && this.prevHeldRS > 0.0f; }
		
		public boolean pressedMiddle() { return this.heldMiddle > 0.0f && this.prevHeldMiddle == 0.0f; }
		public boolean releasedMiddle() { return this.heldMiddle == 0.0f && this.prevHeldMiddle > 0.0f; }
		public boolean holdingMiddle() { return this.heldMiddle > 0.0f && this.prevHeldMiddle > 0.0f; }
}
