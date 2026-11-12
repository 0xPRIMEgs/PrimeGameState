# PrimeGameState
A Java/Jaylib powered GameState / Controller / Menu Management System

Prime License:
If you're a Christian you can use this as MIT. If not apply for it's use in an issue. Jesus is Lord.

---

## Cheatsheet

A quick reference for PGS's public Java API, arranged by class with short descriptions in the style of the [raylib cheatsheet](https://www.raylib.com/cheatsheet/cheatsheet.html).

PGS provides game states, a virtual 640×360 screen, controller input, menus, controller remapping, and persistent game options. Your game owns the main loop and chooses the next state by returning it from `update()`.

**Source reference:** [`main` at `62dbdd2`](https://github.com/0xPRIMEgs/PrimeGameState/tree/62dbdd297c54a7e3e3afa9041af66122452b2703), checked November 12, 2026. This reference covers `src/PrimeGameState/`; the other files under `src/` demonstrate integration.

```java
import PrimeGameState.*;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Vector;
```

Use a compatible Jaylib binding exposing `com.raylib.Raylib`, with its JavaCPP/native dependencies. Extract the repository's `assets.zip` into your working directory before using the menus or demo.

| Class | Purpose |
| --- | --- |
| [PrimeGameState](#primegamestate--states-and-rendering) | Base state, render target, screenshots, and screen helpers. |
| [PrimeGameStateMenu](#primegamestatemenu--menu-base) | Menu rows, selection movement, headers, footers, and menu audio. |
| [MenuItemState](#menuitemstate--menu-rows) | Plain rows, toggles, and multiple-choice values. |
| [ControllerManager](#controllermanager--controller-collection) | Create, cache, and poll controller states. |
| [ControllerState](#controllerstate--buttons-and-remapping) | Logical buttons, join/leave state, and input remapping. |
| [StickState](#stickstate--analog-and-digital-stick-input) | Analog values, digital directions, dead zones, and neutral detection. |
| [OptionsManager](#optionsmanager--shared-settings) | Music/SFX volumes, fullscreen preference, and `options.txt`. |
| [OptionsMenuState](#optionsmenustate--ready-made-options-menu) | Ready-made menu for options and controller-remap selection. |
| [ControllerRemapMenuState](#controllerremapmenustate--saved-controller-mappings) | Per-pad remap editor, file loading, and optional automatic loading. |

### Conventions

- Pass `dt` in **seconds**, normally from `GetFrameTime()`.
- Controller indices and menu indices are **zero-based**. UI labels use Pad 1 for index `0`.
- Poll `ControllerManager.update(dt)` **once per frame**, before reading input or updating states.
- `return this` keeps a state active. Returning another state switches to it. Always assign the result.
- Call `preDraw()`, `draw()`, then `postDraw()`. `postDraw()` already owns the window's `BeginDrawing()`/`EndDrawing()` pair.
- The signatures below are references, not complete Java class declarations. All listed methods are public; static methods are marked `static`.

## PrimeGameState — states and rendering

```java
PrimeGameState(int pad, Texture background);                 // Allocate a virtual-screen render texture.
PrimeGameState update(ControllerManager conMan, float dt);  // Override to update; default returns this.
void preDraw();                                             // Begin the render target; draw background if present.
void draw();                                                // Override to draw state contents; default does nothing.
void postDraw();                                            // End target and scale it to the current window.

PrimeGameState background(Texture texture);                 // Replace the background reference; return this.
Texture snapshot();                                        // Copy the rendered screen into a new upright texture.
void unloadAssets();                                        // Unload screen and non-null background texture.

static int windowWidth();                                   // Configured initial window width: 640.
static int windowHeight();                                  // Configured initial window height: 360.
static int targetFPS();                                     // Target frame rate: 60.
static void DrawTintOverScreen(Color color);                 // Multiply-tint the 640×360 drawing area.
Color getPadColor();                                        // Color for this state's owning pad.
Color getPadColor(int index);                               // Color for a specified pad.
```

Public constants and field:

```java
static final int WIDTH = 640;
static final int HEIGHT = 360;
static final float scale = 0.5f;
static final int windowWidth = 640;   // (int) (1280 * scale)
static final int windowHeight = 360;  // (int) (720 * scale)
static final int targetFPS = 60;
static final float DEADZONE = 0.25f;
boolean running = true;              // A flag your main loop must check.
```

| Pad index | Color |
| --- | --- |
| `0` | `RED` |
| `1` | `BLUE` |
| `2` | `GREEN` |
| `3` | `YELLOW` |
| Any other value | `PURPLE` |

Subclasses can access `protected int pad` and `protected Texture background`.

Create states **after `InitWindow()`**, because their constructors allocate graphics resources. Draw in virtual-screen coordinates. `postDraw()` stretches the target to `GetScreenWidth()` × `GetScreenHeight()` with point filtering; it does not letterbox. The window-size getters return the configured initial dimensions, not the live resized dimensions.

`preDraw()` does not clear the target. If your state has no full-screen background, draw an opaque full-screen rectangle before its contents. A snapshot captures the existing render target, normally the last drawn frame, and allocates a new texture; it does not call `draw()` itself.

**Ownership:** `unloadAssets()` frees the background passed to the state as well as its render target. Avoid sharing one owned texture between states that both unload it. `background(newTexture)` does not unload the previous texture. Cleanup is explicit and is not guarded against repeated calls. Override it for extra resources and call `super.unloadAssets()` once.

## PrimeGameStateMenu — menu base

Extends `PrimeGameState`.

```java
PrimeGameStateMenu(int pad, Texture background, PrimeGameState prevState);

void addChoice(String text);                           // Append a plain menu row.
void addChoice(String text, boolean toggle);           // Append a toggle row with an initial value.
void addChoice(String text, Vector<String> varLabels);  // Append a row with selectable labels.
MenuItemState choice(int index);                       // Read/edit a row by index.

void handleMovement(ControllerState cs);               // Up/down select; left/right change variable rows.
boolean click(ControllerState cs);                     // True on logical A's press frame.
void drawHeader(String text);                         // Top bar and label.
void drawFooter(String text);                         // Bottom bar and label.
void drawActive(int startX, int startY);               // Active-pad indicators, if cm has been assigned.
void drawMenuA();                                     // Draw rows with a selected-row marker.
void unloadAssets();                                 // Unload base resources, sounds, and music.
```

Subclass fields:

```java
protected ControllerManager cm;
protected int menuChoice = 0;
protected int infoBarSize = 32;
protected Vector<MenuItemState> choices;
protected PrimeGameState prevState;
protected Music bgm;
protected Sound move;
protected Sound accept;
```

The constructor loads and starts `assets/bgm.ogg`, and loads `assets/move.ogg` and `assets/accept.ogg`. Initialize the audio device before constructing menus. Stored volume settings are applied when these handles are loaded.

Implement your subclass's `update()` and `draw()`: assign `cm`, call `UpdateMusicStream(bgm)`, check controller availability/activity, call `handleMovement()`, and handle `click()`. The base menu does not do those steps automatically. Add at least one choice before calling `handleMovement()`.

Selection and variable indices clamp at the ends. Stick navigation reacts when leaving neutral; D-pad navigation reacts to press edges. There is no automatic held-direction repeat. `click()` only reports the press: your code performs the action, changes toggles, and plays the accept sound.

`prevState` stores a return reference. It does not create a managed state stack or automatically update, draw, or unload that state.

## MenuItemState — menu rows

```java
MenuItemState(String label);                         // Plain row with no variables or toggle enabled.
int varIndex();                                     // Selected variable-label index; starts at 0.
boolean toggle();                                   // Current toggle value.
boolean vars();                                     // Whether variable-label mode is enabled.
MenuItemState addVar(String label);                  // Append a label; enable variable mode, disable toggle mode.
MenuItemState setToggle(boolean value);              // Set value; enable toggle mode, disable variable mode.
MenuItemState incVarIndex();                         // Increment and clamp to the last variable label.
MenuItemState decVarIndex();                         // Decrement and clamp to zero.
void draw(int startX, int startY);                    // Draw a slanted row, label, and optional value/indicator.

static void DrawSlantedRect(float x, float y, float width,
    float height, float slant, Color color);         // Draw a slanted rectangle from two triangles.
```

Supply at least one variable label before incrementing or drawing a variable row. There is no direct `varIndex(int)` setter; move toward the desired index with `incVarIndex()` / `decVarIndex()`.

```java
// Inside a PrimeGameStateMenu subclass:
addChoice("Play");
addChoice("Fullscreen", false);
Vector<String> difficulties = new Vector<>();
difficulties.add("Easy");
difficulties.add("Normal");
difficulties.add("Hard");
addChoice("Difficulty", difficulties);
choice(2).incVarIndex(); // Start on Normal.

// When the Fullscreen row is activated:
choice(1).setToggle(!choice(1).toggle());
```

## ControllerManager — controller collection

```java
ControllerManager();                        // Start with an empty cache and maxPlayers = 4.
int maxPlayers();                           // Configured player-count value.
ControllerManager maxPlayers(int count);    // Set that value; return this.
ControllerState getPad(int index);           // Return cached state; create one if absent.
void update(float dt);                       // Discover available pad indices 0..15, then poll cached states.
```

Use the same manager throughout the game. New controller states start inactive. `getPad()` can create a state even if that physical controller is disconnected. Check `IsGamepadAvailable(index)` separately when connection status matters.

In this revision, `maxPlayers` is not an enforced allocation/join limit: `getPad()` creates states on demand, including during the discovery scan. Enforce any hard player limit in your game logic. `maxPlayers` also controls the range shown by `drawActive()`.

## ControllerState — buttons and remapping

```java
ControllerState(int pad);                           // Create an inactive state with identity mappings.
ControllerState update(float dt);                   // Poll this pad; normally called by the manager.

int pad();          ControllerState pad(int index);  // Physical index; setter also updates both sticks.
boolean active();   ControllerState active(boolean value); // Joined/enabled flag, not connection status.
float deadZone();   ControllerState deadZone(float value); // Stick dead zone; default 0.2; sets both sticks.
float heldZone();   ControllerState heldZone(float value); // Join/leave hold duration; default 1 second.
StickState leftStick();                             // Logical left stick.
StickState rightStick();                            // Logical right stick.
```

Hold physical **Plus/Start** longer than `heldZone()` to activate a pad; hold physical **Minus/Back** longer than it to deactivate. Plus and Minus are polled even while inactive. Other buttons and sticks are only sampled while active. Check both availability and `active()` before consuming gameplay input; inactive state values can retain their previous samples.

`PrimeGameState.DEADZONE` is a separate constant. Controllers and sticks default to `0.2f`; call `deadZone(PrimeGameState.DEADZONE)` if you want `0.25f`.

### Button queries

Every method in this table returns `boolean` and takes no arguments.

| Logical control | Press edge | Release edge | Continued hold |
| --- | --- | --- | --- |
| A | `pressedA()` | `releasedA()` | `holdingA()` |
| B | `pressedB()` | `releasedB()` | `holdingB()` |
| X | `pressedX()` | `releasedX()` | `holdingX()` |
| Y | `pressedY()` | `releasedY()` | `holdingY()` |
| Left bumper | `pressedLB()` | `releasedLB()` | `holdingLB()` |
| Right bumper | `pressedRB()` | `releasedRB()` | `holdingRB()` |
| Left trigger button | `pressedLT()` | `releasedLT()` | `holdingLT()` |
| Right trigger button | `pressedRT()` | `releasedRT()` | `holdingRT()` |
| D-pad up | `pressedUp()` | `releasedUp()` | `holdingUp()` |
| D-pad down | `pressedDown()` | `releasedDown()` | `holdingDown()` |
| D-pad left | `pressedLeft()` | `releasedLeft()` | `holdingLeft()` |
| D-pad right | `pressedRight()` | `releasedRight()` | `holdingRight()` |
| Plus / Start | `pressedPlus()` | `releasedPlus()` | `holdingPlus()` |
| Minus / Back | `pressedMinus()` | `releasedMinus()` | `holdingMinus()` |
| Left stick click | `pressedLS()` | `releasedLS()` | `holdingLS()` |
| Right stick click | `pressedRS()` | `releasedRS()` | `holdingRS()` |
| Middle / guide | `pressedMiddle()` | `releasedMiddle()` | `holdingMiddle()` |

`pressed*()` means down now and not down at the previous poll. `released*()` means up now and down previously. **`holding*()` excludes the first press frame**: use `pressedA() || holdingA()` for a continuous button-down check. These queries use duration counters, so poll with a positive `dt`. LT/RT here are digital button queries, not analog trigger magnitudes.

### Button mappings

```java
// ControllerState.Button enum values:
A, B, X, Y, LB, RB, LT, RT, Up, Down, Left, Right, LS, RS, Middle

ControllerState mapButton(Button output, Button source); // Physical source drives the named logical output.
Button mappedButton(Button output);                     // Current physical source for an output.
ControllerState swapButtons(Button a, Button b);         // Exchange the two outputs' current sources.
ControllerState resetMappings();                        // Identity buttons; clear stick swaps/inversions.
```

`Button` is `ControllerState.Button`. Its normal Java enum helpers `values()` and `valueOf(String)` are also available. Plus and Minus are deliberately absent from the remappable enum.

| Default logical button | Raylib physical button |
| --- | --- |
| `A`, `B`, `X`, `Y` | `RIGHT_FACE_DOWN`, `RIGHT_FACE_RIGHT`, `RIGHT_FACE_LEFT`, `RIGHT_FACE_UP` |
| `LB`, `RB` | `LEFT_TRIGGER_1`, `RIGHT_TRIGGER_1` |
| `LT`, `RT` | `LEFT_TRIGGER_2`, `RIGHT_TRIGGER_2` |
| `Up`, `Down`, `Left`, `Right` | `LEFT_FACE_UP`, `LEFT_FACE_DOWN`, `LEFT_FACE_LEFT`, `LEFT_FACE_RIGHT` |
| `LS`, `RS` | `LEFT_THUMB`, `RIGHT_THUMB` |
| `Middle` | `MIDDLE` |
| Fixed Plus, Minus | `MIDDLE_RIGHT`, `MIDDLE_LEFT` |

All physical names in the table have the `GAMEPAD_BUTTON_` prefix. These are positional mappings; printed controller labels can differ.

```java
ControllerState cs = conMan.getPad(0);
cs.mapButton(ControllerState.Button.A, ControllerState.Button.B);
// Physical B now drives logical A. Logical B keeps its existing mapping.
```

Mapping is direct, not chained: remapping the logical B output does not change what “physical B” means in the example above. Duplicate physical sources are allowed. Use `swapButtons()` for a two-way exchange.

### Stick remapping

```java
boolean swapSticks();    ControllerState swapSticks(boolean value);
boolean invertLeftX();   ControllerState invertLeftX(boolean value);
boolean invertLeftY();   ControllerState invertLeftY(boolean value);
boolean invertRightX();  ControllerState invertRightX(boolean value);
boolean invertRightY();  ControllerState invertRightY(boolean value);
boolean swapLeftAxes();  ControllerState swapLeftAxes(boolean value);
boolean swapRightAxes(); ControllerState swapRightAxes(boolean value);
```

Swapping sticks changes which physical axes feed each logical stick. Axis swaps and sign inversions apply to the logical stick after that selection. `resetMappings()` resets mappings only; it does not reset activity, dead zones, or input-history counters. Programmatic mapping changes stay in memory unless persisted separately; the public remap menu handles its own save operation.

## StickState — analog and digital stick input

```java
StickState(int pad, int axisX, int axisY);           // Select a pad and two raylib axes.
void update(float dt);                             // Poll axes, apply dead zone, and update direction/timers.

int pad();         StickState pad(int value);
int axisX();       StickState axisX(int value);
int axisY();       StickState axisY(int value);
float x();         StickState x(float value);       // Current analog X.
float y();         StickState y(float value);       // Current analog Y.
float prevX();     StickState prevX(float value);   // Previous analog X.
float prevY();     StickState prevY(float value);   // Previous analog Y.
int ix();                                          // Digital X: -1, 0, or +1.
int iy();                                          // Digital Y: -1, 0, or +1.

float deadZone();  StickState deadZone(float value); // Per-axis threshold; default 0.2.
float heldZone();  StickState heldZone(float value); // Stored value; not used by StickState.update().
boolean fromNeutral();
StickState fromNeutral(boolean value);             // True on the frame the stick leaves full neutral.
float neutralTime(); StickState neutralTime(float value); // Seconds with both axes zero.
float tiledTime(); StickState tiledTime(float value);     // Seconds with at least one axis nonzero.
```

The API name is **`tiledTime()`**, exactly as spelled in the source. The dead zone zeros an axis when `abs(value) < deadZone`; it does not rescale the remaining range or use a circular dead zone. `fromNeutral()` requires both previous axes to be zero, so changing direction without returning to neutral does not produce another event.

For sticks obtained through `ControllerState`, let the manager update them. Do not poll them a second time in the same frame. Raw setters change stored values only; for example, `x(value)` does not recalculate `ix()`.

```java
ControllerState cs = conMan.getPad(0);
if (IsGamepadAvailable(0) && cs.active()) {
    actorX += cs.leftStick().x() * speed * dt; // Analog movement.
    int direction = cs.leftStick().ix();      // -1 / 0 / +1.
    boolean newTilt = cs.leftStick().fromNeutral();
}
```

## OptionsManager — shared settings

```java
static boolean loaded();                          // Whether options loading completed, including a missing file.
static float musicVolume();                       // Stored music gain; default 1.0.
static void musicVolume(float value);              // Clamp 0..1 and round to a 0.1 step.
static float soundVolume();                       // Stored SFX gain; default 1.0.
static void soundVolume(float value);              // Clamp 0..1 and round to a 0.1 step.
static boolean fullscreen();                      // Stored fullscreen preference; default false.
static void fullscreen(boolean value);            // Change preference only.

static void applyMusicVolume(Music music);         // Apply stored gain to one loaded music handle.
static void applySoundVolume(Sound sound);         // Apply stored gain to one loaded sound handle.
static void applyFullscreen();                    // Match the live window to the stored preference.

static boolean loadOptions() throws IOException;  // Read options.txt; false if absent, leaving values unchanged.
static void saveOptions() throws IOException;     // Save stored values to options.txt in the working directory.
```

Setters change stored values only. Apply them to the window/audio handles and save when needed. Volume setters reject non-finite values. Loading can run before window/audio initialization; `applyFullscreen()` requires an initialized window. Repeated fullscreen application does not toggle a window that already matches.

Loading validates the entire file before changing any setting. The saved properties are:

```properties
version=1
musicVolume=100
soundVolume=100
fullscreen=false
```

File volume values are integers `0..100` in steps of `10`; API values are floats `0..1`. Controller remaps are separate files. Apply volumes to every relevant sound/music handle when loading and again when resuming after options change; this API does not set a separate global music/SFX mixer.

## OptionsMenuState — ready-made options menu

Extends `PrimeGameStateMenu`.

```java
OptionsMenuState(int pad, Texture background, PrimeGameState prevState);
PrimeGameState update(ControllerManager conMan, float dt);
void draw();
```

Requires a non-null return state. Offers music volume, SFX volume, fullscreen, a target-pad selector for button remapping, and Save and Return. It loads options on construction if not already loaded, applies fullscreen, and saves changed settings automatically.

The owning active pad controls the options menu. The chosen target pad controls the remapping menu it opens. That pad must be connected and active; hold its Plus button to join if needed. On successful Save and Return, the menu unloads itself and returns `prevState`. A save failure keeps the menu open for retry.

```java
// Inside your state's update(): keep this state alive for the return trip.
return new OptionsMenuState(this.pad, this.snapshot(), this);
```

Use `null` instead of `snapshot()` if no captured background is needed. In this source revision, the built-in options/remap `draw()` methods paint an opaque white rectangle before tinting, so their supplied background is covered.

## ControllerRemapMenuState — saved controller mappings

Extends `PrimeGameStateMenu`.

```java
ControllerRemapMenuState(int pad, Texture background, PrimeGameState prevState);
ControllerRemapMenuState(int pad, Texture background,
    PrimeGameState prevState, Path directory);       // Override the remap directory.
PrimeGameState update(ControllerManager conMan, float dt);
void draw();
void drawMenuA();                                    // Scrolling list with up to 11 visible rows.

static boolean loadRemap(ControllerState cs) throws IOException;
static boolean loadRemap(ControllerState cs, Path directory) throws IOException;
```

Requires a non-null return state. Only the constructor's pad can operate the menu or be edited. Navigation uses the **physical** D-pad/left stick and physical A, independent of saved mappings. The menu waits for neutral/released input when opened or re-enabled.

Changes are a draft until **Save and Return** writes successfully and applies them. **Default** resets the draft; save to commit it. The menu automatically loads the owning pad's saved mapping on its first update. Plus/Minus remain fixed.

The default directory is `controller_remaps`, with `pad_0.txt`, `pad_1.txt`, and so on. Files follow physical pad indices, not controller serial numbers. `loadRemap()` applies one controller's file and returns `false` when it is absent. There is no public `saveRemap()` in this revision; saving is performed by the editor's Save and Return action.

### AutoLoadControllerManager

Public static nested class extending `ControllerManager`:

```java
ControllerRemapMenuState.AutoLoadControllerManager();               // Use controller_remaps/.
ControllerRemapMenuState.AutoLoadControllerManager(Path directory); // Use a custom directory.
ControllerState getPad(int pad);                                    // Load once per controller object.
```

```java
ControllerManager conMan =
    new ControllerRemapMenuState.AutoLoadControllerManager();
```

Use this manager to load saved mappings before gameplay, without first opening the remap menu. Load errors are logged and the current mappings are retained. A missing/failed load is marked attempted for that controller object; use `loadRemap()` explicitly to retry after changing the file.

## Quick start — the main loop

This minimal example uses the base state and controller manager. Save it as `QuickStart.java` beside the demo sources, and compile/run with PGS and your Jaylib dependencies. Menus additionally need the packaged assets.

```java
import PrimeGameState.*;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;

public class QuickStart {
    public static void main(String[] args) {
        try {
            OptionsManager.loadOptions();
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
        }

        InitWindow(PrimeGameState.windowWidth(), PrimeGameState.windowHeight(), "PGS");
        InitAudioDevice();
        SetTargetFPS(PrimeGameState.targetFPS());
        OptionsManager.applyFullscreen();

        ControllerManager controllers =
            new ControllerRemapMenuState.AutoLoadControllerManager();
        PrimeGameState state = new DemoState(0);

        try {
            while (!WindowShouldClose() && state.running) {
                float dt = GetFrameTime();
                controllers.update(dt);
                state = state.update(controllers, dt);
                if (!state.running) break;

                state.preDraw();
                state.draw();
                state.postDraw();
            }
        } finally {
            state.unloadAssets(); // DemoState leaves final cleanup to this loop.
            CloseAudioDevice();
            CloseWindow();
        }
    }

    private static final class DemoState extends PrimeGameState {
        private float x = 312f;
        private float y = 172f;

        DemoState(int pad) {
            super(pad, null);
        }

        @Override
        public PrimeGameState update(ControllerManager controllers, float dt) {
            ControllerState cs = controllers.getPad(this.pad);
            if (IsGamepadAvailable(this.pad) && cs.active()) {
                x += cs.leftStick().x() * 120f * dt;
                y += cs.leftStick().y() * 120f * dt;
                if (cs.pressedB()) this.running = false;
            }
            return this;
        }

        @Override
        public void draw() {
            DrawRectangle(0, 0, WIDTH, HEIGHT, BLACK);
            DrawText("Hold Plus to join. Left stick: move. B: exit.", 16, 16, 16, WHITE);
            DrawRectangle((int) x, (int) y, 16, 16, getPadColor());
        }
    }
}
```

The built-in state classes do not require an external `BeginDrawing()` wrapper. For 3D content, place `BeginMode3D(camera)` / `EndMode3D()` inside your state's `draw()`; the virtual render target is already active.

## Quick recipes

### Open and return from a temporary menu

```java
// In a game state's update(), using your own PrimeGameStateMenu subclass:
return new MyMenu(this.pad, this.snapshot(), this);

// In MyMenu's update(), when Resume is selected:
this.unloadAssets();
return this.prevState;
```

Keep the previous state loaded while the temporary menu is open. PGS updates only the state your loop calls, so the retained game is paused naturally. The menu owns its snapshot texture and frees it through `super.unloadAssets()`.

### Implement a menu update

```java
// Inside a PrimeGameStateMenu subclass; add choices in its constructor.
@Override
public PrimeGameState update(ControllerManager conMan, float dt) {
    this.cm = conMan;
    OptionsManager.applyMusicVolume(this.bgm);
    OptionsManager.applySoundVolume(this.move);
    OptionsManager.applySoundVolume(this.accept);
    UpdateMusicStream(this.bgm);

    ControllerState cs = this.cm.getPad(this.pad);
    if (!IsGamepadAvailable(this.pad) || !cs.active()) return this;
    this.handleMovement(cs);

    if (this.click(cs)) {
        PlaySound(this.accept);
        // Handle this.menuChoice here; return another state if needed.
    }
    return this;
}
```

### Replace a state permanently

```java
// Inside update(); NextState is your own concrete state class.
PrimeGameState next = new NextState(this.pad, null);
this.unloadAssets();
return next;
```

Do not unload a state you intend to resume. When abandoning a menu and its retained game, clean up both. When exiting with a retained-state chain, your application must clean up every still-live state; the base menu does not recursively unload `prevState`.

Choose one owner for final cleanup. The quick-start loop unloads its final state, so its state only sets `running = false`. If adapting demo states that already unload during their exit branch, avoid unloading them again. Stop drawing after an exit transition that freed the current state.

### Activate a controller immediately

```java
conMan.getPad(0).active(true); // Skip hold-to-join for a single-player setup.
```

### Change and save options

```java
OptionsManager.musicVolume(0.6f);
OptionsManager.soundVolume(0.8f);
OptionsManager.applyMusicVolume(bgm);
OptionsManager.applySoundVolume(shotSound);
OptionsManager.fullscreen(true);
OptionsManager.applyFullscreen();
OptionsManager.saveOptions(); // Handle or declare IOException.
```

### Load a remap without opening the menu

```java
ControllerState cs = conMan.getPad(0);
boolean found = ControllerRemapMenuState.loadRemap(cs); // Handle or declare IOException.
```

### Draw a pause overlay

```java
// In a menu whose background is a snapshot of the paused game:
@Override
public void draw() {
    DrawTintOverScreen(DARKGRAY);
    drawHeader("Paused");
    drawFooter("A: select");
    drawMenuA();
}
```

`DrawTintOverScreen()` uses multiplied color tinting; it does not convert the snapshot to grayscale. Drawing a full-screen opaque rectangle first would cover the snapshot.

## Repository demo

| File | Demonstrates |
| --- | --- |
| `src/Main.java` | Initialization, controller polling, state updates, and draw order. |
| `src/TitleState.java` | Title screen and transition to a main menu. |
| `src/MainMenuState.java` | Menu actions and opening options. |
| `src/Game.java` | Controller movement and opening a snapshot-based pause menu. |
| `src/GamePauseState.java` | Resume, main-menu, and exit actions. |
| `assets.zip` | Menu music, sounds, and demo background textures. |

The demo classes are application examples; the reusable library classes live in the `PrimeGameState` package. Despite older placement comments in a few files, `OptionsMenuState` and `ControllerRemapMenuState` are also in that package in this revision.
