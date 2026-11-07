package PrimeGameState;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

/**
 * Put beside MainMenuState.java (outside the PrimeGameState package folder).
 * Open without unloading the returning state:
 *   return new ControllerRemapMenuState(this.pad, this.snapshot(), this);
 *
 * Only the constructor's pad has menu control. No other pad is read or edited.
 * Navigation is physical D-pad/left stick and physical A, independent of remaps.
 * Edits stay in this menu until Save and Return successfully writes the file.
 * Default resets the draft; Save and Return commits it. Plus/minus stay fixed.
 *
 * For automatic loading before gameplay, optionally instantiate:
 *   ControllerManager conMan = new ControllerRemapMenuState.AutoLoadControllerManager();
 * This replaces only the manager construction at the call site; no existing
 * source files need to be rewritten by this class. Without that integration,
 * saved mappings load automatically when this menu first updates.
 *
 * Files: controller_remaps/pad_0.txt, pad_1.txt, ... (physical pad index, zero-based).
 * Files follow pad indices, not device serial numbers, if controllers reorder.
 */
public class ControllerRemapMenuState extends PrimeGameStateMenu {
	private static final ControllerState.Button[] BUTTONS = ControllerState.Button.values();
	private static final Path DEFAULT_DIRECTORY = Path.of("controller_remaps");
	private static final String[] OPTION_KEYS = {
		"swapSticks", "invertLeftX", "invertLeftY", "invertRightX", "invertRightY",
		"swapLeftAxes", "swapRightAxes"
	};
	private static final String[] OPTION_LABELS = {
		"Swap Left / Right Sticks", "Mirror Left Stick X", "Mirror Left Stick Y",
		"Mirror Right Stick X", "Mirror Right Stick Y", "Swap Left X / Y", "Swap Right X / Y"
	};
	private static final int DEFAULT_CHOICE = BUTTONS.length + OPTION_KEYS.length;
	private static final int SAVE_CHOICE = DEFAULT_CHOICE + 1;
	private static final int VISIBLE_ROWS = 11;

	private final Path directory;
	private Settings settings;
	private boolean initialized = false;
	private boolean inputReady = false;
	private boolean previousA = false;
	private int previousX = 0;
	private int previousY = 0;
	private int firstRow = 0;
	private String status = "";

	public ControllerRemapMenuState(int pad, Texture background, PrimeGameState prevState) {
		this(pad, background, prevState, DEFAULT_DIRECTORY);
	}

	public ControllerRemapMenuState(int pad, Texture background, PrimeGameState prevState, Path directory) {
		super(pad, background, Objects.requireNonNull(prevState, "A return state is required"));
		this.directory = Objects.requireNonNull(directory, "directory");
		for (ControllerState.Button button : BUTTONS) {
			Vector<String> sources = new Vector<String>();
			for (ControllerState.Button source : BUTTONS) sources.add(source.name());
			this.addChoice("Output " + button.name() + " <-", sources);
		}
		for (String label : OPTION_LABELS) this.addChoice(label, false);
		this.addChoice("Default");
		this.addChoice("Save and Return");
	}

	@Override
	public PrimeGameState update(ControllerManager conMan, float dt) {
		this.cm = conMan;
		UpdateMusicStream(this.bgm);
		ControllerState cs = this.cm.getPad(this.pad);
		if (!this.initialized) {
			try {
				loadRemap(cs, this.directory);
				this.status = "Settings are applied when saved.";
			} catch (IOException | IllegalArgumentException | SecurityException e) {
				this.status = "Could not load file. Current settings kept.";
				System.err.println("Pad " + this.pad + " remap load failed: " + e.getMessage());
			}
			this.settings = new Settings(cs);
			this.refreshChoices();
			this.initialized = true;
		}

		// Menu ownership is this.pad, exactly as in the other menus. Active alone
		// does not give another pad permission to operate or change this menu.
		if (!IsGamepadAvailable(this.pad) || !cs.active()) {
			this.inputReady = false;
			return this;
		}
		int x = this.readDirection(GAMEPAD_AXIS_LEFT_X, GAMEPAD_BUTTON_LEFT_FACE_LEFT, GAMEPAD_BUTTON_LEFT_FACE_RIGHT);
		int y = this.readDirection(GAMEPAD_AXIS_LEFT_Y, GAMEPAD_BUTTON_LEFT_FACE_UP, GAMEPAD_BUTTON_LEFT_FACE_DOWN);
		boolean a = IsGamepadButtonDown(this.pad, GAMEPAD_BUTTON_RIGHT_FACE_DOWN);

		// Do not reuse the A press that opened the menu, or held input on reconnect.
		if (!this.inputReady) {
			this.previousX = x;
			this.previousY = y;
			this.previousA = a;
			if (x == 0 && y == 0 && !a) this.inputReady = true;
			return this;
		}

		if (y != 0 && y != this.previousY) {
			this.menuChoice = Math.max(0, Math.min(SAVE_CHOICE, this.menuChoice + y));
			PlaySound(this.move);
		} else if (x != 0 && x != this.previousX) {
			this.changeChoice(x);
		}

		boolean clicked = a && !this.previousA;
		this.previousX = x;
		this.previousY = y;
		this.previousA = a;
		if (clicked) {
			PlaySound(this.accept);
			if (this.menuChoice == DEFAULT_CHOICE) {
				this.settings = new Settings();
				this.refreshChoices();
				this.status = "Defaults selected. Save and Return to apply.";
			} else if (this.menuChoice == SAVE_CHOICE) {
				try {
					this.settings.save(this.directory, this.pad);
					this.settings.apply(cs);
					this.unloadAssets();
					return this.prevState;
				} catch (IOException | SecurityException e) {
					this.status = "Save failed. Settings kept here; press A to retry.";
					System.err.println("Pad " + this.pad + " remap save failed: " + e.getMessage());
				}
			} else {
				this.changeChoice(1);
			}
		}
		return this;
	}

	private int readDirection(int axis, int negativeButton, int positiveButton) {
		boolean negative = IsGamepadButtonDown(this.pad, negativeButton);
		boolean positive = IsGamepadButtonDown(this.pad, positiveButton);
		if (negative || positive) return (positive ? 1 : 0) - (negative ? 1 : 0);
		float value = GetGamepadAxisMovement(this.pad, axis);
		return value < -0.5f ? -1 : (value > 0.5f ? 1 : 0);
	}

	private void changeChoice(int direction) {
		if (this.menuChoice < BUTTONS.length) {
			int index = this.settings.sources[this.menuChoice].ordinal();
			index = Math.floorMod(index + direction, BUTTONS.length);
			this.settings.sources[this.menuChoice] = BUTTONS[index];
		} else if (this.menuChoice < DEFAULT_CHOICE) {
			int option = this.menuChoice - BUTTONS.length;
			this.settings.options[option] = !this.settings.options[option];
		} else return;
		this.refreshChoices();
		this.status = "Unsaved changes. Save and Return to apply.";
		PlaySound(this.move);
	}

	private void refreshChoices() {
		for (int i = 0; i < BUTTONS.length; i++) {
			MenuItemState item = this.choice(i);
			int target = this.settings.sources[i].ordinal();
			while (item.varIndex() < target) item.incVarIndex();
			while (item.varIndex() > target) item.decVarIndex();
		}
		for (int i = 0; i < OPTION_KEYS.length; i++) {
			this.choice(BUTTONS.length + i).setToggle(this.settings.options[i]);
		}
	}

	@Override
	public void draw() {
		DrawRectangle(0, 0, WIDTH, HEIGHT, WHITE); //ClearBackground(BLACK); magiclly ClearBackground does NOT get respected if Game has a background texture. This is the fix.
		DrawTintOverScreen(DARKGRAY);
		DrawTintOverScreen(this.getPadColor());
		this.drawHeader("Controller Remapping | Pad " + (this.pad + 1) + " | John 3:16");
		this.drawFooter("D-pad / LS: move   Left/Right or A: edit   (physical controls)");
		DrawText("Logical output <- Physical button", 32, 42, 10, WHITE);
		DrawText("Only pad " + (this.pad + 1) + " has menu control.", 320, 64, 10, WHITE);
		DrawText("Plus / Minus stay fixed.", 320, 84, 10, WHITE);
		DrawText("Mirrors apply after stick swapping.", 320, 104, 10, WHITE);
		DrawText("Changes apply after saving.", 320, 124, 10, WHITE);
		if (this.cm != null && (!IsGamepadAvailable(this.pad) || !this.cm.getPad(this.pad).active())) {
			DrawText("Connect this pad; hold (+) to join.", 320, 164, 10, YELLOW);
		}
		this.drawMenuA();
		DrawText(this.status, 32, 304, 10, WHITE);
	}

	@Override
	public void drawMenuA() {
		if (this.menuChoice < this.firstRow) this.firstRow = this.menuChoice;
		if (this.menuChoice >= this.firstRow + VISIBLE_ROWS) this.firstRow = this.menuChoice - VISIBLE_ROWS + 1;
		int end = Math.min(this.choices.size(), this.firstRow + VISIBLE_ROWS);
		for (int i = this.firstRow; i < end; i++) {
			this.choice(i).draw(32, 64 + (20 * (i - this.firstRow)));
		}
		DrawText(">>>>", 48, 68 + (20 * (this.menuChoice - this.firstRow)), 10, WHITE);
		DrawText((this.menuChoice + 1) + " / " + this.choices.size(), 320, 264, 10, WHITE);
	}

	/** Load just this controller, leaving all others untouched. Missing file is OK. */
	public static boolean loadRemap(ControllerState cs) throws IOException {
		return loadRemap(cs, DEFAULT_DIRECTORY);
	}

	public static boolean loadRemap(ControllerState cs, Path directory) throws IOException {
		Objects.requireNonNull(cs, "controller");
		Path file = directory.resolve("pad_" + cs.pad() + ".txt");
		if (Files.notExists(file)) return false;
		Settings loaded = Settings.load(file, cs.pad());
		loaded.apply(cs);
		return true;
	}

	/** Optional drop-in manager: loads once per controller object before input polling. */
	public static class AutoLoadControllerManager extends ControllerManager {
		private final Path directory;
		private final HashMap<Integer, ControllerState> loadedPads = new HashMap<Integer, ControllerState>();

		public AutoLoadControllerManager() { this(DEFAULT_DIRECTORY); }
		public AutoLoadControllerManager(Path directory) {
			this.directory = Objects.requireNonNull(directory, "directory");
		}

		@Override
		public ControllerState getPad(int pad) {
			ControllerState cs = super.getPad(pad);
			if (this.loadedPads.get(pad) != cs) {
				try {
					loadRemap(cs, this.directory);
				} catch (IOException | IllegalArgumentException | SecurityException e) {
					System.err.println("Pad " + pad + " remap load failed: " + e.getMessage());
				}
				this.loadedPads.put(pad, cs);
			}
			return cs;
		}
	}

	private static class Settings {
		private final ControllerState.Button[] sources = BUTTONS.clone();
		private final boolean[] options = new boolean[OPTION_KEYS.length];

		Settings() {}
		Settings(ControllerState cs) {
			for (int i = 0; i < BUTTONS.length; i++) this.sources[i] = cs.mappedButton(BUTTONS[i]);
			this.options[0] = cs.swapSticks();
			this.options[1] = cs.invertLeftX();
			this.options[2] = cs.invertLeftY();
			this.options[3] = cs.invertRightX();
			this.options[4] = cs.invertRightY();
			this.options[5] = cs.swapLeftAxes();
			this.options[6] = cs.swapRightAxes();
		}

		void apply(ControllerState cs) {
			for (int i = 0; i < BUTTONS.length; i++) cs.mapButton(BUTTONS[i], this.sources[i]);
			cs.swapSticks(this.options[0]).invertLeftX(this.options[1]).invertLeftY(this.options[2])
				.invertRightX(this.options[3]).invertRightY(this.options[4])
				.swapLeftAxes(this.options[5]).swapRightAxes(this.options[6]);
		}

		static Settings load(Path file, int pad) throws IOException {
			Properties values = new Properties();
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				values.load(reader);
			}
			if (!"1".equals(values.getProperty("version")) || !Integer.toString(pad).equals(values.getProperty("pad"))) {
				throw new IOException("Wrong remap version or pad index");
			}
			Settings result = new Settings();
			try {
				for (int i = 0; i < BUTTONS.length; i++) {
					String source = values.getProperty("button." + BUTTONS[i].name());
					if (source == null) throw new IllegalArgumentException("Missing button");
					result.sources[i] = ControllerState.Button.valueOf(source.trim());
				}
				for (int i = 0; i < OPTION_KEYS.length; i++) {
					String value = values.getProperty(OPTION_KEYS[i]);
					if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException("Invalid toggle");
					result.options[i] = Boolean.parseBoolean(value);
				}
			} catch (IllegalArgumentException e) {
				throw new IOException("Incomplete or invalid remap file", e);
			}
			return result;
		}

		void save(Path directory, int pad) throws IOException {
			Properties values = new Properties();
			values.setProperty("version", "1");
			values.setProperty("pad", Integer.toString(pad));
			for (int i = 0; i < BUTTONS.length; i++) values.setProperty("button." + BUTTONS[i].name(), this.sources[i].name());
			for (int i = 0; i < OPTION_KEYS.length; i++) values.setProperty(OPTION_KEYS[i], Boolean.toString(this.options[i]));
			Files.createDirectories(directory);
			Path file = directory.resolve("pad_" + pad + ".txt");
			Path temporary = Files.createTempFile(directory, "pad_" + pad + "_", ".tmp");
			try {
				try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
					values.store(writer, "Controller remap: logical output = physical source. Plus/minus are fixed.");
				}
				try {
					Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(temporary);
			}
		}
	}
}
