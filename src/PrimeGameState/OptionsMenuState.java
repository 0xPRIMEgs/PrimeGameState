package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

/**
 * Place beside MainMenuState.java and ControllerRemapMenuState.java.
 * Open without unloading the returning state:
 *   return new OptionsMenuState(this.pad, this.snapshot(), this);
 *
 * Options save automatically to options.txt when changed and on Save and Return.
 * They load once when this menu is first constructed. To load before gameplay,
 * call OptionsManager.loadOptions(), then OptionsManager.applyFullscreen()
 * after InitWindow (loading can throw IOException).
 * Music/SFX levels are shared across states. This menu and the
 * remapping menu it opens apply both levels to their own audio automatically.
 * For other menus/game audio, apply these helpers after loading and whenever
 * resuming/updating the state (existing files are deliberately not changed):
 *   OptionsManager.applyMusicVolume(this.bgm);
 *   OptionsManager.applySoundVolume(this.move);
 *   OptionsManager.applySoundVolume(this.accept);
 * Raylib has no separate global music and SFX volume controls, so every other
 * Music/Sound handle must use the relevant helper. Master volume is untouched.
 */
public class OptionsMenuState extends PrimeGameStateMenu {
	private static final int MUSIC = 0;
	private static final int SOUND = 1;
	private static final int FULLSCREEN = 2;
	private static final int REMAP = 3;
	private static final int RETURN = 4;

	private boolean initialized = false;
	private String status = "";
	private int padChoices = 1;

	public OptionsMenuState(int pad, Texture background, PrimeGameState prevState) {
		super(pad, background, Objects.requireNonNull(prevState, "A return state is required"));
		if (!OptionsManager.loaded()) {
			try {
				OptionsManager.loadOptions();
			} catch (IOException | SecurityException e) {
				this.status = "Could not load options.txt. Current settings kept.";
				System.err.println("Options load failed: " + e.getMessage());
			}
		}
		OptionsManager.applyFullscreen();
		this.addChoice("Music Volume", this.volumeChoices());
		this.addChoice("Sound Effects Volume", this.volumeChoices());
		this.addChoice("Fullscreen", IsWindowFullscreen());
		Vector<String> pads = new Vector<String>();
		pads.add("Pad 1");
		this.addChoice("Button Remapping", pads);
		this.addChoice("Save and Return");
		this.setVarIndex(MUSIC, Math.round(OptionsManager.musicVolume() * 10.0f));
		this.setVarIndex(SOUND, Math.round(OptionsManager.soundVolume() * 10.0f));
		this.applyMenuVolumes();
	}

	private Vector<String> volumeChoices() {
		Vector<String> values = new Vector<String>();
		for (int i = 0; i <= 100; i += 10) values.add(i + "%");
		return values;
	}

	private void setVarIndex(int row, int value) {
		MenuItemState item = this.choice(row);
		while (item.varIndex() < value) item.incVarIndex();
		while (item.varIndex() > value) item.decVarIndex();
	}

	private void refreshPads() {
		int count = Math.max(this.cm.maxPlayers(), this.pad + 1);
		for (int i = 0; i < 16; i++) {
			if (IsGamepadAvailable(i)) count = Math.max(count, i + 1);
		}
		count = Math.max(1, Math.min(16, count));
		while (this.padChoices < count) {
			this.padChoices++;
			this.choice(REMAP).addVar("Pad " + this.padChoices);
		}
	}

	@Override
	public PrimeGameState update(ControllerManager conMan, float dt) {
		this.cm = conMan;
		this.refreshPads();
		this.applyMenuVolumes();
		UpdateMusicStream(this.bgm);
		this.choice(FULLSCREEN).setToggle(IsWindowFullscreen());
		if (!this.initialized) {
			this.setVarIndex(REMAP, Math.max(0, Math.min(this.padChoices - 1, this.pad)));
			this.initialized = true;
			return this;
		}

		ControllerState cs = this.cm.getPad(this.pad);
		if (!IsGamepadAvailable(this.pad) || !cs.active()) return this;
		this.handleMovement(cs);
		this.updateVolumes();

		if (this.click(cs)) {
			if (this.menuChoice == MUSIC || this.menuChoice == SOUND) {
				int value = (this.choice(this.menuChoice).varIndex() + 1) % 11;
				this.setVarIndex(this.menuChoice, value);
				this.updateVolumes();
			}
			PlaySound(this.accept);
			if (this.menuChoice == FULLSCREEN) {
				OptionsManager.fullscreen(!IsWindowFullscreen());
				OptionsManager.applyFullscreen();
				this.choice(FULLSCREEN).setToggle(IsWindowFullscreen());
				this.trySaveOptions();
			}
			if (this.menuChoice == REMAP) {
				int targetPad = this.choice(REMAP).varIndex();
				if (!IsGamepadAvailable(targetPad)) {
					this.status = "Connect Pad " + (targetPad + 1) + " before remapping it.";
					return this;
				}
				this.status = "";
				// The chosen pad owns the remapping menu; this Options menu keeps
				// its original owner when Save and Return brings it back.
				return new ControllerRemapMenuState(targetPad, this.snapshot(), this) {
					@Override
					public PrimeGameState update(ControllerManager manager, float delta) {
						OptionsManager.applyMusicVolume(this.bgm);
						OptionsManager.applySoundVolume(this.move);
						OptionsManager.applySoundVolume(this.accept);
						return super.update(manager, delta);
					}
				};
			}
			if (this.menuChoice == RETURN) {
				if (!this.trySaveOptions()) return this;
				this.unloadAssets();
				return this.prevState;
			}
		}
		return this;
	}

	private void updateVolumes() {
		float oldMusic = OptionsManager.musicVolume();
		float oldSound = OptionsManager.soundVolume();
		OptionsManager.musicVolume(this.choice(MUSIC).varIndex() / 10.0f);
		OptionsManager.soundVolume(this.choice(SOUND).varIndex() / 10.0f);
		this.applyMenuVolumes();
		if (oldMusic != OptionsManager.musicVolume() || oldSound != OptionsManager.soundVolume()) this.trySaveOptions();
	}

	private boolean trySaveOptions() {
		try {
			OptionsManager.fullscreen(IsWindowFullscreen());
			OptionsManager.saveOptions();
			this.status = "Options saved.";
			return true;
		} catch (IOException | SecurityException e) {
			this.status = "Save failed. Use Save and Return to retry.";
			System.err.println("Options save failed: " + e.getMessage());
			return false;
		}
	}


	private void applyMenuVolumes() {
		OptionsManager.applyMusicVolume(this.bgm);
		OptionsManager.applySoundVolume(this.move);
		OptionsManager.applySoundVolume(this.accept);
	}


	@Override
	public void draw() {
		DrawRectangle(0, 0, WIDTH, HEIGHT, WHITE); //ClearBackground(BLACK); magiclly ClearBackground does NOT get respected if Game has a background texture. This is the fix.
		DrawTintOverScreen(DARKGRAY);
		DrawTintOverScreen(this.getPadColor());
		this.drawHeader("Options | Pad " + (this.pad + 1) + " | John 3:16");
		this.drawFooter("Up/Down: choose   Left/Right: change   A: select");
		this.drawActive(WIDTH - 48, 8);
		this.drawMenuA();
		DrawText("Remapping: choose a pad, then press A.", 32, 192, 10, WHITE);
		DrawText("The selected pad controls its remapping menu.", 32, 212, 10, WHITE);
		DrawText("If inactive, hold (+) on that pad to join.", 32, 232, 10, WHITE);
		DrawText("Options save automatically to options.txt.", 32, 272, 10, WHITE);
		DrawText(this.status, 32, 296, 10, YELLOW);
	}
}
