package PrimeGameState;

import static com.raylib.Raylib.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;

/** Shared game options, persistence and application. Place beside OptionsMenuState.
 * loadOptions() reads values without needing a window or audio device.
 * After InitWindow, call applyFullscreen(). Apply audio volumes to each loaded
 * handle when loading or resuming its state. Setters change values only;
 * use the apply methods to apply them and saveOptions() to persist them.
 * Existing options.txt files remain compatible.
 */
public final class OptionsManager {
	private OptionsManager() {}
	private static float musicVolume = 1.0f;
	private static float soundVolume = 1.0f;
	private static boolean fullscreen = false;
	private static final Path OPTIONS_FILE = Path.of("options.txt");
	private static boolean optionsLoaded = false;

	public static boolean loaded() { return optionsLoaded; }
	public static float musicVolume() { return musicVolume; }
	public static float soundVolume() { return soundVolume; }
	public static boolean fullscreen() { return fullscreen; }
	public static void musicVolume(float value) { musicVolume = normalizeVolume(value); }
	public static void soundVolume(float value) { soundVolume = normalizeVolume(value); }
	public static void fullscreen(boolean value) { fullscreen = value; }

	private static float normalizeVolume(float value) {
		if (!Float.isFinite(value)) throw new IllegalArgumentException("Volume must be finite");
		return Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 10.0f) / 10.0f;
	}

	public static void applyMusicVolume(Music music) { SetMusicVolume(music, musicVolume); }
	public static void applySoundVolume(Sound sound) { SetSoundVolume(sound, soundVolume); }
	/** Call only after InitWindow. Repeated calls do not toggle an already matching window. */
	public static void applyFullscreen() {
		if (IsWindowFullscreen() != fullscreen) ToggleFullscreen();
	}

	/** Loads values only; call applyFullscreen() after InitWindow. Missing file leaves current values unchanged.
	 * Validates the entire file before changing any setting. Other states must
	 * apply the audio helpers to their own loaded Music/Sound handles as usual.
	 */
	public static boolean loadOptions() throws IOException {
		if (Files.notExists(OPTIONS_FILE)) {
			optionsLoaded = true;
			return false;
		}
		Properties values = new Properties();
		try (Reader reader = Files.newBufferedReader(OPTIONS_FILE, StandardCharsets.UTF_8)) {
			values.load(reader);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid options.txt", e);
		}
		if (!"1".equals(values.getProperty("version"))) throw new IOException("Unsupported options version");
		float music = readVolume(values, "musicVolume");
		float sound = readVolume(values, "soundVolume");
		String fullscreen = values.getProperty("fullscreen");
		if (!"true".equals(fullscreen) && !"false".equals(fullscreen)) throw new IOException("Invalid fullscreen setting");
		boolean targetFullscreen = Boolean.parseBoolean(fullscreen);
		OptionsManager.fullscreen = targetFullscreen;
		musicVolume = music;
		soundVolume = sound;
		optionsLoaded = true;
		return true;
	}

	private static float readVolume(Properties values, String key) throws IOException {
		try {
			int value = Integer.parseInt(values.getProperty(key));
			if (value < 0 || value > 100 || value % 10 != 0) throw new NumberFormatException();
			return value / 100.0f;
		} catch (NumberFormatException e) {
			throw new IOException("Invalid " + key + ": expected 0 to 100 in steps of 10", e);
		}
	}

	/** Saves stored volumes and fullscreen preference; controller maps stay separate. */
	public static void saveOptions() throws IOException {
		Properties values = new Properties();
		values.setProperty("version", "1");
		values.setProperty("musicVolume", Integer.toString(Math.round(musicVolume * 100.0f)));
		values.setProperty("soundVolume", Integer.toString(Math.round(soundVolume * 100.0f)));
		values.setProperty("fullscreen", Boolean.toString(fullscreen));
		Path target = OPTIONS_FILE.toAbsolutePath();
		Path temporary = Files.createTempFile(target.getParent(), "options_", ".tmp");
		try {
			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				values.store(writer, "Game options. Volume: 0-100 in steps of 10.");
			}
			try {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

}
