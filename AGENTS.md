# Element Space (元素空间)

A PICO OS 6 Shared Space app for self-study: a periodic table in a Planar window, and one
Volumetric window per opened element showing a rotatable 3D model of the atom.

- Package / applicationId: `com.example.elementspace.p7c3a91e4` (generated demo identity)
- PICO Spatial SDK BOM 6.1.9, created with `pico-cli project create --template planar`
- Product spec (PRD): https://claude.ai/code/artifact/80fe5287-0f8c-4cae-a8ba-9a70e389893c
- Design (Figma): https://www.figma.com/design/43ZtRRrS5nDNWaCAuuPLyQ

## Windows

| Window | Declared in | Notes |
| --- | --- | --- |
| Periodic table | `DefaultWindowContainer` in `Main.kt`, size and glass in `AndroidManifest.xml` | Planar, 1360 x 880 dp, system glass |
| Atom Volume (`AtomVolume`) | `WindowContainer(form = Form.Volumetric)` in `Main.kt` | 1600 dp cube (960 to 2700), no glass; opened with a tag per element and the atomic number in the bundle; at most 3 at once |

Inside a Volume, the view switcher is a window-level `Toolbar` and the info card is an `Augment`
(420 x 880 dp, anchored to the right face of the Volume). Nothing 2D is drawn inside the Volume.

## Layout of the code

```
data/repository/   ElementRepository (bundled dataset), SessionRepository (language + open Volumes)
domain/model/      Element, Isotope, Allotrope, ElectronConfiguration, Vec3 and scene geometry
domain/usecase/    filtering, electron configuration, trend scale, nucleus packing,
                   orbital cloud sampling, allotrope lattices (all pure Kotlin, unit-tested)
ui/table/          PeriodicTable UiState / ViewModel / Screen + components
ui/atom/           Atom UiState / ViewModel / Screen + components
ui/atom/components/AtomScene.kt       ECS entity tree, instanced meshes, OrbitSystem
ui/atom/components/AtomSceneView.kt   SpatialView + drag-to-rotate and two-hand scale
ui/theme/          app-owned colours, Noto typography, in-app language resources
```

## Rules to keep

- All 2D UI is SpatialUI inside `PicoTheme`. No Material / Material3 imports.
- Do not replace native `ColorScheme` roles. App colours live in `ui/theme/ElementColors.kt`.
- No app-authored borders or `backgroundMaterial`; window glass is the system's.
- Hover uses `Modifier.spatialHoverEffect`, never `hoverable` + animated scale.
- Composables never touch a Repository; state flows ViewModel -> UiState -> Screen.
- Per-frame 3D motion belongs in an ECS `System` (`OrbitSystem`), not in recomposition.
- Text comes from `res/values` (English) and `res/values-zh`; read it with `str(...)` so the
  in-app language switch applies. A new language is a new values folder plus an `AppLanguage` entry.

## Build and check

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Install and launch go through `pico-cli` (`pico-cli app install`, `pico-cli app launch`).

## Known gaps (as of 2026-10-04)

- Verified on the PICO emulator 6.1 (AVD `Pico_Emulator_6_1_fresh`) with a person clicking,
  because the CLI cannot inject spatial taps: main window, opening an atom Volume, Toolbar and
  info card placement, shell view. Never run on a headset; frame rate and eye / hand / controller
  input are unverified.
- Lesson from a real crash: never keep a `MeshResource` across scene rebuilds. Destroying the old
  content releases its meshes, and `ModelEntity(mesh, …)` with a released mesh throws
  `IllegalArgumentException`. `AtomScene` creates meshes per rebuild.
- `spatialHoverEffect` only transforms what comes after it in the modifier chain. To move a whole
  tile, it must sit before `clip` / `background`.
- A Volume keeps its physical size (it does not scale with distance like a Planar window), so its
  default is a 1600 dp cube; 960 dp read as small and far.
- The system glass is bright in a lit room, unlike the dark glass in the Figma frames; tile tint
  was strengthened so light labels stay readable.
- Electron cloud is an instanced point cloud sampled from orbital shapes. The SDK's
  `ParticleComponent` can only come from a Spatial Editor bundle, so it is not used.
- Trend mode lifts tiles along Z; it does not draw solid pillars. Category and the four trend
  properties are one always-visible segmented control ("Colour by"); the earlier Trends
  expand/collapse button was removed at the user's request because it read as two unrelated controls.
- A `Subwindow` is always 360 dp wide and as tall as its window, so beside a 1600 dp Volume it was
  a tall strip with the lower half empty. The info card is therefore an `Augment`, which is sized
  by its content. Its sections follow the selected view (shell: particles, shell bars, isotopes;
  nucleus: particles + mass number; cloud: subshell switches + configuration; allotrope: list).
  Captions that used to float at the top-left of the Volume were tiny and far away; they are now
  one line in the card.
- The nucleus is a relaxed sphere packing (`BuildNucleusLayoutUseCase`), tested for roundness,
  non-overlap and proton/neutron mixing. Do not seed height and angle from the same sequence: that
  put every nucleon on one spiral. Electrons are small metallic beads (PBR, electron colour) with a
  faint halo; their size and colour are a drawing convention, not physics.
- Fonts: the variable fonts `NotoSans-VF.ttf` and `NotoSansSC-VF.ttf` (from the Google Fonts
  repository, OFL texts alongside) are bundled in `app/src/main/assets/fonts`. Chinese UI uses
  Noto Sans SC, English UI uses Noto Sans (`ui/theme/ElementTypography.kt`). Rendering is unverified
  on a device, and Noto Sans SC has no glyph for 𫟷 (Lv), which falls back to the system font.
- Demo recording: `ui/navigation/DemoMode.kt` runs a scripted walkthrough when the emulator
  global setting `elementspace_demo` lists atomic numbers (`adb shell settings put global
  elementspace_demo 79,6`), because nobody can click in the emulator from the CLI. Off otherwise;
  delete the setting after recording.
- Element data was typed in for the first build and must be checked against a licensed source.
