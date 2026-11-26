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
| Reaction Volume (`ReactionVolume`, v1.1.0) | `WindowContainer(form = Form.Volumetric)` in `Main.kt` | 1600 dp cube, one at a time (tag `reaction`); shows `SessionRepository.openReaction`, so picking another reaction swaps it in place |
| Atom Volume (`AtomVolume`) | `WindowContainer(form = Form.Volumetric)` in `Main.kt` | 1600 dp cube (960 to 2700), no glass; opened with a tag per element and the atomic number in the bundle; at most 3 at once |

Inside a Volume, the view switcher is a window-level `Toolbar` and the info card is an `Augment`
(420 x 880 dp, anchored to the right face of the Volume). Nothing 2D is drawn inside the Volume.

## Layout of the code

```
data/repository/   ElementRepository (bundled dataset), SessionRepository (language + open Volumes)
domain/model/      Element, Isotope, Allotrope, ElectronConfiguration, Vec3 and scene geometry
domain/usecase/    filtering, electron configuration, trend scale, nucleus packing,
                   orbital cloud sampling, allotrope lattices (all pure Kotlin, unit-tested)
ui/table/          PeriodicTable UiState / ViewModel / Screen + components (settings menu, how-to, exit confirm)
ui/reaction/       Reaction UiState / ViewModel / Screen; components: ReactionScene (ECS, flat Bohr models,
                   GlideComponent / FlightComponent), ReactionCard (Augment), ReactionToolbar
data/repository/ReactionRepository.kt   bundled reaction library (PEP textbooks, source per entry)
domain/usecase/ChemicalFormula.kt       formula parser, balance / electron checks (ReactionLibraryTest)
domain/usecase/ReactionLayout.kt        ring / slot / pair / flight geometry (pure, unit-tested)
platform/BackKeys.kt, AppExit.kt        BACK / controller B (never exits), Exit app
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

## v1.1.0 polish (2026-11-24): common reactions, settings

- Info card section "Common reactions" lists every bundled reaction containing the element (no school-stage filter since v1.2.0: the app ships abroad too).
  ▶ shows it in the reaction Volume: 4 steps, electrons that move are gold and fly along an arc; card on the right
  explains each step, counts electrons lost / gained, shows what you see, properties and the textbook source.
- Adding a reaction = one entry in `BundledReactionRepository.LIBRARY`. `ReactionLibraryTest` checks it is balanced,
  conserves electrons, keeps particle ids across steps, keeps the electron total constant and that atoms do not overlap.
- Settings (title bar): language, How to use, Exit app (confirm). BACK / B closes the topmost overlay, never exits.
- Nuclei in the reaction Volume use model colours (`ElementColors.model`), not category colours (H and O were both blue).

### Debug broadcast (debuggable builds)

```bash
adb shell am broadcast -a com.example.elementspace.p7c3a91e4.DEBUG -p com.example.elementspace.p7c3a91e4 --es cmd reaction --es arg na_cl2
```

Commands (`--es cmd`, optional `--es arg`): `atom <Z>`, `reaction na_cl2|h2_o2|fe_cuso4`, `step <1..4>`, `next`, `prev`,
`replay`, `play`, `settings`, `howto`, `exit` (confirm card), `exit-yes` (same call as [退出]), `reset`, `lang zh|en`; v1.4.0 adds `aview`, `cloud`, `orbital`, `phase`, `nodes`, `axes`.
Wait ~5 s after `reaction` before `step` (the new reaction resets to step 1), and ~5 s before a screenshot: the
emulator's capture lags the scene by a few seconds. `logcat -s ElementSpaceRx` logs every rendered step.

Screenshot list: S-set `settings`; S-howto `howto`; S-exit `exit`; S-card `atom 11` (also 1, 8, 17, 26, 29);
S-rx-n `reaction <id>` then `step n` (3 reactions x 4 steps).

## v1.2.0 polish (2026-11-24): organic molecules (C, H, O)

- Molecule library `BundledMoleculeRepository` (14: methane, ethane, butane, isobutane, ethylene, acetylene, cyclohexane,
  benzene, ethanol, dimethyl ether, acetic acid, ethyl acetate, CO2, H2O). Geometry is built by `MoleculeBuilder` from
  textbook bond lengths / angles; `MoleculeLibraryTest` checks formula vs atoms, valence (C 4, H 1, O 2), bond lengths,
  labelled angles, no overlaps, flat / linear shapes, comparisons and linked reactions.
- C / H / O info cards: "How carbon bonds" (C only) and "Molecules" grouped by skeleton (chain / branched / ring / inorganic).
  ▶ opens the molecule Volume (`MOLECULE_VOLUME`, one at a time): ball-and-stick / space-filling, bond-angle arcs,
  functional-group highlight, "compare with …" puts two molecules side by side (each turns about its own centre).
- Organic reactions (`OrganicReactions`: CH4 + Cl2, C2H4 + Br2, esterification) use `ReactionKind.BONDS`: ball-and-stick
  frames with stable atom ids; red dashed = about to break, gold = new bond; the card counts atoms left / right.
- No school-system labels in the UI (no junior / senior / compulsory / elective tags); the textbook source line stays.

Debug commands added: `molecule <id>`, `compare <id|none>`, `mmode ball|fill`, `angles`, `groups`,
`reaction ch4_cl2|c2h4_br2|esterification`. `logcat -s ElementSpaceMol` logs every molecule render.

## v1.3.0 polish (2026-11-25): readable cards, card placement, conditions, atom symbols

- Side cards (`ui/common/SideCard.kt`, `CardKit.kt`): 560 x 920 dp, anchored at x = 0.8 of the Volume
  (just past the drawn model) + 24 dp, turned 20° about their left edge towards a viewer in front,
  `followViewpoints = ViewPoint.All`. Every card is a `PagedCard`: fixed header, tabs, one kind of
  content per page. Body text never below bodyLarge; section titles titleMedium; source bodyMedium.
  Atom: 结构 / 化学式 / 分子 / 性质 (empty pages left out). Reaction: 这一步 / 现象与原理. Molecule:
  结构 / 性质 (comparison stays one page). Debug: `tab <n>` picks a page on every open card.
- Atom symbols (`ui/common/SceneLabels.kt`): a bitmap texture on `MeshResource.createPlane` (already an
  XY plane facing +Z), always facing the front: the label undoes the parent's Rx(pitch) * Ry(yaw) and
  is pushed towards the viewer by the ball radius. Ions print their charge (`AtomLabelText`: protons -
  drawn electrons for full ions only; sulfate cluster SO₄²⁻). An ion's label sits in front of its
  transparent charge halo, which hides anything inside it. Molecule Volume: toolbar 符号 toggle
  (debug `labels`), on by default.
- Conditions (`Reaction.visuals`, `conditionSteps` default {1, 2}: off for reactants and products):
  LIGHT = glowing sun + 3 rays aimed at the bond light breaks + 「光照」tag; HEAT = burner + two
  flickering cones (`createCone(1, 1)` scaled (r, h, r), pivot at its centre) + 「加热」; IGNITE = small
  flame with spinning sparks where the reactants meet (under them if that spot is taken) + 「点燃」;
  CATALYST = tag only (浓硫酸 · 催化剂), never drawn as a molecule or counted.

## v1.4.0 polish (2026-11-25): atomic orbitals, bond-angle / group toggles

- Orbitals (`domain/usecase/Orbitals.kt`): hydrogen-like ψ = R_nl(r)·A(direction). R uses the associated
  Laguerre polynomials (Z = 1, r in a₀); A is the real s/p/d/f harmonic, with its nodal planes / cones.
  `extent` = 99 % radius; `radialNodes` (n − l − 1), `mostProbableRadius`. `SampleOrbitalUseCase` draws r
  from P(r) = r²R² by its cumulative table and the direction from A² by rejection (separable). Points
  are in chemistry axes (z up); `AtomScene` maps x → x, y → −z, z → y.
- `Slater.zEff` (groups (1s)(2s,2p)(3s,3p)(3d)(4s,4p)(4d)(4f)…, 0.30 / 0.35 / 0.85 / 1.00) and
  `OrbitalTexts` (Slater working, radial note, "why it fills this way": Aufbau order, unpaired count,
  exceptions vs `ElectronConfigurationUseCase.aufbauOnly`, 4s-before-3d, np³ for n ≤ 4 only — Sb < Te).
- Cloud view: toolbar 整体电子云 / 单个轨道 (default single). Single = phase colours (blue ψ > 0, orange
  ψ < 0), nodes (translucent discs both sides, bead rings for cones, translucent spheres for radial
  nodes), axes with x / y / z labels, name tag under the orbital ("2p_x": SceneLabels draws text after
  "_" as a subscript). It stands still (no spin) so axes stay put. Whole = the old per-subshell cloud.
- Atom card in the cloud view: first page 轨道 — phase / nodes / axes toggles, orbital box diagram
  (highest energy on top, Hund fill, tap a box), quantum numbers, shape, node counts, then 进阶:
  radial distribution chart, Slater Z*, why. Sources on the page.
- Molecules: molecules without a hand-picked angle get one per kind of carbon
  (`BundledMoleculeRepository.carbonAngles`: 4 bonds ≈109°28′, 3 ≈120°, 2 180°); angle values and the
  group name are SceneLabels tags in 3D; 键角 / 官能团 buttons are greyed out when there is nothing to mark.
- Debug: `aview shell|nucleus|cloud|allotrope`, `cloud whole|single`, `orbital 3d:2` (n, letter, index
  into `Orbitals.shapes[l]`), `phase`, `nodes`, `axes`.

## v1.5.0 polish (2026-11-25): common reactions for every element

- `data/repository/ElementReactions.kt` is GENERATED from the factory's
  `apps/element-space/polish/M5-reactions-data.py` by `M5-gen-kotlin.py` (edit the data, regenerate).
  179 equations, each with a source (人教版 J / S1 / S2 / X1, or Greenwood & Earnshaw by chapter for
  elements the textbooks skip), condition key, types and a one-line phenomenon in both languages.
  `NOTES` says why 28 elements (He, Ne, Ar, At, Fr, Cm–Og) have no common reaction; the atom card's
  化学式 page shows that note instead of a list.
- `AutoReaction` draws them from the equation alone (`Reaction.schematic = true`, kind BONDS, 4 steps:
  reactants, old bonds break, atoms regroup, products with gold new bonds). Each formula unit is a
  schematic cluster: a unique non-O centre (S in SO₄, preference list `CENTRES`) with O around it,
  metals of a salt on an O, H on a free O (else on the centre), parenthesised groups as their own
  little star; no unique centre → alternating ring. Units pack in rows of 3; the picture fits
  |x| ≤ 0.7 (the side card starts at 0.8) via `ReactionFrame.atomScale` (≤ 0.13, the hand-made scale).
  Conditions: ign → IGNITE, light → LIGHT, heat/high/hot → HEAT, catalysts → CATALYST tag, the rest
  (discharge, pressure, −196 ℃ …) → `ConditionKind.NOTE` tag. Card adds 「示意…」 under the legend.
- `forSymbol` sorts junior, senior, then reference-book reactions.

## v1.6.0 polish (2026-11-25): atom -> ion, collide two elements

- Every interaction is a single tap (gaze + pinch or controller trigger): no grabbing, no dragging.
- Atom toolbar (shell view): [拿走一个电子] [加一个电子], greyed out outside the element's common ions
  (`minCharge` / `maxCharge`). 3D: tinted halo (red = lost, blue = gained) and an ion tag under the
  model; card: `IonText.explain` (charge, new shell counts, the noble gas it now matches). The charge
  no longer resets when "advanced" collapses.
- Table title bar: [碰一碰] toggle. In the mode a tile tap picks instead of opening; the second pick
  runs `CollideElementsUseCase` and shows `CollideOverlay`. Rules in order: a library reaction whose
  two reactants are exactly the two simple substances (why = who loses / gains, or shared pairs
  leaning to the more electronegative atom); elements in `ElementReactions.NOTES` other than He/Ne/Ar
  -> no experiment; noble gas -> full shell; two metals -> alloy, no reaction; else "no common direct
  reaction" with buttons to each element. It never invents a product.
- 46 element + element reactions appended to the factory data file (`EXTRA` block; appended so
  earlier ids keep their numbers). Library: 232 reactions.
- Debug: `collide 11,17` / `collide off`, `charge 1|-1`.

## v1.7.0 polish (2026-11-25): plain-language line, three levels, first-run card, guided paths

- `ElementSummary.of`: one plain sentence at the top of every atom card (31 hand-written, the rest by
  family; elements in `ElementReactions.NOTES` reuse that note).
- `LearningLevel` BASIC / SCHOOL / UNIVERSITY in `SessionRepository.level` (default BASIC, picked in
  the Settings menu; in memory only). Atom card: the 轨道 page needs UNIVERSITY; the reaction list is
  filtered by `reactionsFor` (beginner = junior + hand-animated, topped up to three) with a line saying
  how many more exist.
- First launch (`SharedPreferences element_space/first_run_done`): `FirstRunOverlay`, three things to
  try, then free exploring or the first path.
- 跟我学: `GuidePaths.ALL` (5 paths, 3-4 steps). `GuideBar` is docked at the bottom of the table
  window in place of the status row: one sentence + one button per step. `GuideAction`: open an atom,
  drive an open atom (`SessionRepository.atomCommands` -> `AtomViewModel`), show a reaction or
  molecule (`auxWindow` request the screen opens), or collide two elements.
- Debug: `guide` (list) / `guide <id>`, `gnext`, `gexit`, `firstrun`, `level basic|school|university`.
  After a fresh install send `reset` once to close the first-run card before screenshots.

## v1.8.0 polish (2026-11-25): everyday things ("生活里的它")

- `EverydayThings.ALL` (14 things for 18 elements): name, one plain line, and what "看里面" opens
  (`ThingInside`: a molecule, an allotrope of this element, or a reaction). Atom card page 生活里.
- The thing's model replaces the atom in the atom Volume (`AtomSceneSpec.thing`), turning slowly;
  the balloon bobs (`BobComponent`). `ThingModels.copy` loads assets/models/<dir>/<id>.glb with
  `Entity.loadSuspend(..., ModelFormat.GLTF)`, strips opacity components, and hands out clones;
  `index.json` (written by the factory's glb_index.py with trimesh) gives the box used to centre and
  fit the model to 1.1 scene units. A scene rebuild is skipped while the same thing stays on show.
- Assets (all in assets/models/things, 0.1–1.2 MB each):
  balloon, pencil, can, wire, ring, bulb, battery = Tripo Smart Mesh (pick 1 of 4) + Tripo texture, 2k,
  about 5–7k quads, no decimation needed (~/Downloads/element-space-tripo/);
  salt, cola, ice, egg = reused from the chef-board app's decimated Tripo models;
  cinnabar = ink-brush app; jar = fireflies app;
  diamond = generated by the factory script diamond.py (ideal brilliant-cut proportions, flat normals).
- Debug: `thing <id>` / `thing none` (to the open atoms that have it).

## v1.9.0 polish (2026-11-26): lab bench with visible phenomena

- A fifth window, `LAB_VOLUME` (`ui/lab`), opened from 实验台 in the table's title bar; it shows
  `SessionRepository.openExperiment`. `Experiments.ALL` is plain data: equipment (`LabProp`), the
  effects of each stage (`LabEffect`) and the textbook wording of what you see. Eight experiments:
  flame test (7 salts, flame low / mid / high), burning magnesium, iron wire in oxygen, sodium and
  water, silver tree, blue precipitate, hydrogen peroxide + MnO₂, thermite. Each links to its
  reaction for "看原子怎样重新组合".
- One tap per step: the same button is in the toolbar under the bench and on the card. Nothing is
  dragged or grabbed.
- `LabScene`: bench and equipment are Tripo models placed once per experiment (assets/models/lab,
  glassware drawn with `ThingModels.glass`); the effects layer is rebuilt per stage, all under a
  `stage` entity scaled 1.5. Effects: sprite particles from a pool of camera-facing planes moved by
  `LabTickSystem` — additive (`BlendingMode.ADD`, texture fading to black, opaque material) for
  flames, sparks and glows; alpha-blended straight-alpha white for smoke, bubbles (ring) and flakes;
  `PointLightComponent` that flickers with whatever burns (never more than two); PBR liquids whose
  colour changes over seconds; small plain solids for reagents and products.
- The SDK's `ParticleComponent` is not used: there is no documented way to build one in code (it is
  authored in the Spatial Editor). No sound yet.
- Assets (assets/models/lab): bench, lamp, crucible (on tripod), tuberack = Tripo Smart Mesh + texture;
  beaker, gasjar = Tripo Smart Mesh, untextured, glass material in code. Crucible tongs were
  generated but unusable and are not in the app.
- Debug: `lab <id>`, `lnext`, `lreset`, `lchoice <n>`, `lpower 0|1|2`.

## v1.9.1 (2026-11-26)

- Four more experiments on the same beaker: iron nail in copper sulfate, zinc + dilute sulfuric acid,
  marble + dilute hydrochloric acid, carbon dioxide into limewater (12 in all).
- Sixth guided path, "焰火的颜色从哪来", which opens the flame test (`GuideAction.Lab`).
- The content depth chosen in Settings is saved (`SharedPreferences element_space/level`).

## v1.9.2 (2026-11-26)

- Three more everyday things (Tripo, textured): `nail` (Fe, rusting), `chip` (Si), `match` (P, S). 17 things in all; `things/index.json` regenerated with scratchpad `glb_index.py`.
- Making an ion now shows the electron: `FlyComponent` in AtomScene moves one gold electron from the outer shell away (charge up) or in from outside (charge down), 1.6 s, driven by the existing orbit system. Only in the shell view.
- `EverydayThing.glows`: the bulb is drawn switched on (`ThingModels.lit` sets emissive on its materials, plus an additive halo using `LabScene.glowBitmap()` and one point light).

## v1.10.0 (2026-11-26)

- Lab sound: `ui/lab/LabSound.kt`. `LabMix.of(effects, power)` turns the visible effects into three loop volumes (flame, sparks, fizz); `LabSound` plays them with SoundPool and fires `lab_ignite` once when a flame first appears. Clips in `res/raw/lab_*.wav` are synthesized noise: regenerate with factory `apps/element-space/polish/M10-sounds.py <res/raw>`. No recordings, nothing to license.
- Toolbar toggle 声音, remembered in SharedPreferences `element_space/sound` (default on).
- Guide paths `iron_rust` and `chip_sand` (8 paths). New `AtomAction.Thing(id)` shows an everyday thing from a guide step.
- Tests: LabMixTest; GuidePathsTest checks Thing ids belong to the element.
