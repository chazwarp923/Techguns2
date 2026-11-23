# Port changelog (unattended session)

Running log of changes made while working unattended, for review. Nothing here is committed.
Newest entries at the bottom. Every entry was compile-checked (`gradlew compileJava processResources`); none were tested in game unless stated.

## Already done earlier this session (with you)

- Structure worldgen for the 1.12 scanned structures: `src/main/java/techguns/world/gen/` (LegacyBlocks, MBlock, PlaceContext,
  ScannedStructure, TGStructureDefs, TGStructures) and `data/techguns/worldgen/structure{,_set}/`.
- 1.12 chest loot tables converted to `data/techguns/loot_table/chests/`.
- Fixes: recursive cache crash, /locate support, vanilla random_spread placement, double chests, connecting-block shapes, door halves.
- Removed config options `worldgen.StructureSpawnWeight{Small,Medium,Big}` (TGConfig).
- C2ME added as a dev-client-only runtime mod (build.gradle, gradle.properties `c2me_version`).

## Unattended work

### 1. Code-built structures: alien bug nest and military base (compiles; not tested in game)

New framework for structures that 1.12 built with code instead of from a block list:
- `world/gen/GenWorld.java`: in-memory block buffer with the 1.12 World-style calls the generators used. A structure is
  generated once from its seed, then every chunk copies its own part. Reads of positions the generator did not write
  come from a fixed rule (solid below the ground, air above), never from the live world, so chunks always agree.
- `world/gen/TerrainView.java`: ground heights from the chunk generator (same answer at any time).
- `world/gen/ProcGenerator.java`, `world/gen/ProcStructures.java`: generator interface, registry by name, a cache of
  generated structures (12 entries), and ground levelling for camps.
- `world/gen/TGStructures.java`: `techguns:legacy` structures can now also list procedural names; new piece type
  `techguns:procedural` (`ProcPiece`), which also feeds NeoForge's `PieceBeardifierModifier` so vanilla terrain
  adaptation can blend the ground around it.
- `MBlock.Spawner.heightOffset()` (helicopter spawner height).

Ported generators (`world/gen/procedural/`):
- `AlienBugNestGen` (1.12 AlienBugNest/AlienBugNestStructure): sand chambers, tunnels, egg piles, alien bug spawners,
  slimy ladder shaft. Deserts, badlands and wastelands only (`data/techguns/tags/worldgen/biome/has_structure/alien_bug_nest.json`).
- `MilitaryCampGen` + `CampParts` (1.12 MilitaryBaseStructure/MilitaryCamp and Tent, Containers, CampProps, Bunker,
  Barracks, Helipad, Tanks, WatchTowerSmall, EmptyPlane). Fence, roads, helicopter spawner in the centre, soldier spawners
  on the roads, bunker/barracks chests with the `militarybase_*` loot tables.

Data: new structures `alien_bug_nest` and `military_base`. Both are in the `overworld_medium` structure set:
weights medium 35 / bug nest 20 / base 8, matching 1.12's medium weights and roughly its big-structure frequency.
`overworld_small` now stays 6 chunks (was 3) away from medium placements so small houses don't land inside a camp.

Deliberate differences from 1.12, worth checking:
- Camp ground is levelled completely to one height (1.12 allowed up to 4 blocks of difference and smoothed it).
  Terrain around it is blended by vanilla `beard_thin`. The camp is placed in the `top_layer_modification` step, after
  trees, so the levelling removes trees in the camp; trees from neighbouring chunks can still overhang a little.
- Tents use the biome's camo net colour (1.12 computed the colour but always placed the woodland net).
- 1.12's slimy-ladder shaft code had a bug that made it fill every free wall-adjacent spot in the shaft's box; kept as is
  since that is what the nest looked like.
- The castle was not ported in this step; see entry 2.

### 2. Dungeon templates and the castle (compiles; not tested in game)

- `tools/dungeon_converter/`: a small stand-alone Java program, with a README, that reads the 1.12 Java-serialized
  dungeon templates (`assets/techguns/dungeons/*.ser`). It uses stub classes with the 1.12 names and serialVersionUIDs,
  and writes `assets/techguns/dungeons/*.json`. All 12 templates were converted; the JSON files are new resources. The
  `.ser` files are left in place and are no longer read, so they can be deleted once you're happy with the JSON.
- `world/gen/dungeon/` (new):
  - `DungeonTemplate`: loads a JSON template, converts the palette with `LegacyBlocks`, and gives chests the preset's
    loot table. `chisel:netherbrick` becomes nether bricks, following the 1.12 replacement table.
  - `SegmentType`: 1.12 TemplateSegment patterns and rotations.
  - `MazeDungeonPath`: a line-by-line port of the 1.12 maze generator (corridors, ramps, rooms, foundations, pillars,
    roof, NPC spawners).
  - `Dungeon`: picks the best of 5 paths.
  - `DungeonPreset` / `PresetCastle`.
- `world/gen/procedural/CastleGen.java`: 1.12 CastleStructure.
  - Size: 32-47 x 24-39 x 32-47.
  - Placement: needs land with up to 10 blocks of height difference. The lowest level is sunk into the ground.
  - Spawners: zombie and skeleton soldiers.
  - Registered as procedural name `castle`; new structure `data/techguns/worldgen/structure/castle.json` (no terrain
    adaptation, like 1.12).
  - Added to the `overworld_medium` set with weight 8, the same as the military base (both had weight 1 among 1.12's
    big structures).
- `LegacyBlocks`: `techguns:debugblock` (1.12 template scanner markers) maps to air.

Not ported:
- Nether dungeon: it was commented out in 1.12's spawn list, so it never generated there.
- Tech fortress: no preset spawned it in 1.12. Its templates also use the 1.12 `door3x3`, which isn't mapped in
  LegacyBlocks.

Differences from 1.12:
- 1.12 picked a random template per level from a list; every list had one entry, so this changes nothing.
- 1.12 did not bounds-check the cell after an upward ramp, which could crash at the grid edge. Out of range is now
  treated as blocked.
- `GenWorld.mergeLegacyPairs()` (new; run after a dungeon is built) applies the two fixes the scanned structures already
  get:
  - Door halves take their missing properties from each other. The castle templates have spruce doors.
  - Side-by-side single chests with the same facing become double chests. The castle's lower corner room and mid ramp
    have chest pairs.

  `MBlock.Chest.withState()` was added for this.

### 3. Desert oil cluster (compiles; not tested in game)

1.12 only spawned this when a mod added an oil fluid (config `FluidListOilWorldspawn`), and so does the port.
- New `world/gen/WorldspawnOil.java` reads the existing `fluid_recipes.FluidListOilWorldspawn` config (default `oil`,
  `crude_oil`).
  - A full id such as `immersivepetroleum:crude_oil` is used as is.
  - A bare name matches that fluid name in any mod, as in 1.12, choosing the alphabetically first id when several match.
  - It uses the first fluid that has a source block.
- `TGStructureDefs` defines `desert_oil_cluster` only when such a fluid exists. Its palette matches 1.12: sandstone,
  sandbags, a soldier spawner (army soldiers and commandos), oil and sand, and oil ore cluster blocks. It uses
  `clean(6).yOffset(-4)`.
- New structure `data/techguns/worldgen/structure/desert_oil_cluster.json`:
  - biomes come from the new tag `#techguns:has_structure/desert_oil_cluster` (`#c:is_desert`);
  - it is in the `overworld_medium` set with weight 15, as in 1.12;
  - when no oil mod is installed it never generates, and the set falls through to its other structures.
- It counts as an ore-cluster structure for the `spawnOreClusterStructures` config, as in 1.12.

### 4. Headless worldgen smoke tests

- New `src/main/java/techguns/gametest/WorldgenTests.java` contains three game tests:
  - all 12 dungeon templates load with 16 segments each;
  - castle, military base and bug nest build for 8 seeds, and one of each is placed into the test world;
  - every scanned structure is placed in all 4 rotations.

  After each placement, every door's lower half is checked for a matching upper half (same facing, hinge and open
  state), as a regression check for the door bug you reported.

  They only check that nothing throws; they don't check how the result looks. They're registered under the `techguns`
  namespace, which only the dev runs enable.
- `src/main/resources/data/techguns/structure/empty.nbt` is a 1x1x1 empty template the tests need.
- `build.gradle`: the `gameTestServer` run now uses its own game directory, `run-gametest/`, so it doesn't touch
  `run/`. `.gitignore`: added `run-gametest`.
- Run them with `./gradlew runGameTestServer`.

The first test run found two bugs, both now fixed. After the fixes all 3 tests pass. The only warning left is the
unmapped `door3x3` in the tech fortress templates, which never spawn.
- Castle chests had the loot table id `techguns:techguns:chests/castle`, so generating a castle would have crashed.
  `PresetCastle` now passes `chests/castle`, because `MBlock.Chest` adds the namespace itself.
- `LegacyBlocks.vanilla("skull", meta)` failed: vanilla's flattening table has no entry for skulls because 1.12 kept the
  skull type in the block entity. Skulls now become skeleton skulls (meta 1 on the floor, 2-5 on a wall facing N/S/W/E).
  The castle templates have 2 skulls.

### 5. Gameplay gaps against the 1.12 event handlers and TODOs (compiles; game tests pass; not tested in game)

All remaining `TODO` markers in `src/main/java` are resolved, and the 1.12 `TGEventHandler`/`TGTickHandler` handlers were
compared one by one against the port.

- **Sounds (1.12 TGSound / PacketPlaySound).** The port played every Techguns sound as a fixed vanilla sound, so it
  stayed where it started. Now:
  - New `client/audio/TGSound` (tickable sound instance): moving sounds follow the entity and stop when it is gone;
    gun-position sounds sit one block beside the head; an `EntityCondition` stops a sound early.
  - New `packets/PacketPlaySound`: the server sends Techguns sounds to players within 100 blocks, as 1.12 did. The
    acting player skips them unless `playOnOwnPlayer`, because their client already played the sound itself.
  - `util/SoundUtil` rewritten around this. The client player is installed in `TGClientSetup`, so this stays safe on
    a dedicated server.
  - Charge guns: the charge-up sound now stops when charging ends (`EntityCondition.CHARGING_WEAPON`).
- **Shishkebap flames.** An earlier comment in `RenderGunBase` said the item-attached particles were only for a debug gun.
  That was wrong: they are the Shishkebap's burning blade (`ScreenTestFX`).
  - `RenderItemBase.renderItemParticles` draws them in hand space, as 1.12 did.
  - New `client/particle/AmbientItemFX` starts and stops the effect. 1.12 did this with server packets on equipment
    change and when the fuel ran out; the client knows both the held items and their ammo, so it now checks every
    client tick.
  - `TGParticleBuffers.posed` now takes any `MultiBufferSource`. `TGItemRenderer.getRenderer` was added.
- **Lock-on guns.** Pressing or releasing right mouse with a lock-on charge gun resets the lock (`ClientGunInput`).
- **Weapon swap (1.12 PacketSwapWeapon).** When the gun stacks change hands (swap-hands key), the server swaps their
  fire delays. The new `PacketSwapWeapon` makes clients nearby swap the attack times (recoil and reload animations) too.
  This is in `TGTickHandler.detectWeaponSwap`. `TGExtendedPlayer.swapAttackTimes` already existed but was never called.
- **Unsafe mode permission (1.12 TGPermissions).** The config `general.RestrictUnsafeModeToOP` existed but nothing used
  it.
  - New `TGPermissions` registers the permission node `techguns.allowunsafemode` (granted to everyone by default) and
    checks it.
  - The config, when on, overrides the node: only ops may leave safe mode.
  - It is enforced when toggling safe mode and on login (`ArmorEvents.onLogin`).
- **Ammo change crafting.** Crafting a gun with a different ammo type now gives back the ammo that was loaded, or the
  ammo and empty magazine (new `events/GunInteractEvents.onCraft`, as in 1.12 `onCraftEvent`). The port's comment said
  1.12 lost it; it didn't.
- **Two-handed guns.** While a two-handed gun is in either hand, off-hand right-click actions on items, blocks and
  entities are blocked, as 1.12 did. Sneaking with a gun in the off hand and some other item in the main hand still
  uses the block. This is in `GunInteractEvents`.
- **Area mining highlight.** Mining tools with a mining radius now outline every block the swing would also break, using
  the same checks as the mining itself (new `client/MiningHighlight`, 1.12 `onBlockHighlight`).
- **Jetpack flames on other players.** A player who starts tracking someone already boosting now gets the boost state
  (`ArmorEvents.onStartTracking`). Before, it was only sent when it changed.
- Removed an obsolete TODO in `TGTickHandler` (syncing the face/back/hand slots); Curios syncs those itself.

Checked and left as they are:
- 1.12 `onRenderHand` used reflection to stop charge guns and grenades from bobbing while in use. The port's
  `GenericGun.shouldCauseReequipAnimation` already covers this in 1.21. Worth a look in game.
- Military crate drops already work through loot tables; Curios drops the extra slots on death.
- Not ported:
  - CraftTweaker scripts for gun, armor and death-effect stats would need a new CraftTweaker dependency, so that's
    left for you to decide.
  - Machine recipes are already datapack JSON recipes, so packs can change them without CraftTweaker.

### 6. Game tests for the gameplay code and the mod's data (all 20 tests pass)

Run with `./gradlew runGameTestServer` (about a minute, headless, in `run-gametest/`).

- New `gametest/GameplayTests.java` (14 tests). The players are NeoForge fake players, which drop packets, so the
  tests call the handlers directly. They cover:
  - the unsafe mode permission node and `RestrictUnsafeModeToOP` (op and non-op);
  - the safe-mode key and the forced safe mode on login;
  - weapon swap detection: fire delays swap only when the same stacks change hands;
  - `swapAttackTimes`;
  - the ammo change crafting refund, including no refund when the grid holds something else;
  - off-hand blocking with two-handed guns, for items and entities;
  - the sneak-to-use-block rule;
  - area mining block selection. A 3x3 plane of stone, obsidian, chest, air, planks, bedrock, dirt and deepslate must
    select exactly stone, obsidian, dirt and deepslate. An empty drill and a chainsaw select nothing;
  - the Shishkebap's ambient effect needing fuel, and no other gun having one;
  - every Techguns sound having a registry id, sound category ids, and the server-side `SoundUtil` calls;
  - `PacketPlaySound`/`PacketSwapWeapon` codec round trips;
  - `EntityCondition`;
  - the start-tracking jetpack sync.
- New `gametest/DataTests.java` (3 tests):
  - every machine recipe type has recipes, and every Techguns recipe has a result;
  - every Techguns loot table loads, and the chest tables used by structures exist;
  - every Techguns item and block has an `en_us` name.
- Refactors so the tests can reach the logic (no behaviour change):
  - `PacketTGKeybindPress.handleOnServer` was split out of `handle`;
  - `GenericGunMeleeCharge.getAreaMiningBlocks` is now shared by `mineArea` and the client `MiningHighlight`, so the
    outline and the actual mining can't disagree;
  - `GenericGun.isAmbientEffectActive` is used by `AmbientItemFX`.
- I checked that the tests catch failures. With the safe-mode permission check removed and the off-hand rule disabled,
  `safemodeToggleRespectsPermission` and `twoHandedGunBlocksOffhand` failed. Both changes were then reverted.
- Not covered: client-only code. This is the `TGSound` playback, the Shishkebap flame rendering, the mining outline
  drawing and the lock-on reset. NeoForge 1.21.1 has no client game tests, so those still need an in-game look.

### 7. Techguns Manual (Patchouli) and Ponder scenes (all 23 tests pass; not tested in game)

Optional dependencies: Patchouli `1.21.1-93-NEOFORGE` and Ponder `1.0.69` (Flywheel `1.0.0-9` is its client
dependency). All three are `localRuntime`, so they're in the dev client and the game test server, but not required.

- **Manual.** The 1.12 book files were copied over with `book.json` in `assets/`, the 1.12 layout. Since Patchouli
  1.20 it has to be in `data/` with `use_resource_pack: true`, so the book didn't load in the port. Converted with a one-off script:
  - `book.json` moved to `data/techguns/patchouli_books/techguns_manual/`, creative tab `techguns:techguns`;
  - metadata ids flattened (`itemshared:99` to `enricheduranium`, `simplemachine:8` to `camo_bench`, and so on), the
    multiblock pages use the new block ids, and the armor-stand NBT uses 1.21's lower case `count`;
  - text that described 1.12 only things was rewritten: the inventory tab and its toggle buttons are Curios slots and
    keybinds, RF is FE, the config file name, and the TODO/WIP notes;
  - entry `category` ids are namespaced (`techguns:weapons`), which Patchouli 1.20+ requires. Without this the whole
    book loaded empty;
  - broken 1.12 links fixed (`reactionchamber`, `fabricator`, `machines/reaction_chamber` were missing their folder).
  The crafting recipes were already ported with a `patchouli` mod-loaded condition.
- **Ponder** (`plugins/ponder`, registered from `TGClientSetup` only when Ponder is loaded). Four scenes, tagged
  "Techguns Multiblocks": fabricator, reaction chamber, small ore drill, framed ore drill. The schematics are in
  `assets/techguns/ponder/` and the scene text in `en_us.json` (`techguns.ponder.*`). The "right-click to form" step
  runs the machine's own `MultiBlockSchematic.form` on Ponder's level, so the scene shows the real formed models.
  For that, `MultiBlockMachineTileEntMaster.setFormed` no longer returns on the client. All its callers are server
  only, apart from the scenes.
- New `gametest/GuideTests.java` (3 tests):
  - every category, icon, spotlight item, crafting recipe, multiblock block, armor-stand entity (all 4 pieces must
    load), image and `$(l:...)` link (including anchors) in the manual resolves. It also applies Patchouli's own load
    rules: namespaced categories and parents, crafting pages on crafting recipes, and multiblock patterns with one `0`
    center and even rows;
  - the manual recipes make a `patchouli:guide_book` with the `techguns:techguns_manual` component;
  - every Ponder schematic placed in the world passes `checkForm`, forms, links every part and unforms
    (template `empty_large`, 7x9x7).
- I checked that they catch failures: a wrong glass block in the fabricator schematic and a misspelled icon both
  failed, then I put them back.
- Needs an in-game look: the book opens from the creative tab with the Techguns manual model, pages render, the
  multiblock previews show; in Ponder, the camera faces the controllers and the formed models appear after the
  right-click step.

### 8. Textures for 1.14+ vanilla art, deepslate ores, Raw Titanium, Metal Blast Furnace (all 24 tests pass; not tested in game)

I compared all 370 Techguns block and item textures, pixel by pixel, with vanilla 1.12.2 and 1.21.1. The 1.12.2 jar
came from Mojang's launcher servers. Only these were built on vanilla art that changed in 1.14+. They were rebuilt
with scripts (in the job's temp folder, not the repo) and checked on a before/after sheet:
- **Ores** (tin, lead, titanium, uranium): titanium and uranium were 1.12 redstone ore with recoloured spots, and tin
  and lead sat on an old style stone. They now use a 1.21 vanilla ore's coloured spot pixels recoloured with the
  Techguns spot colours (matched by brightness), and vanilla's grey rock shading. Iron's pattern for tin, gold's for
  lead, diamond's for titanium, redstone's for uranium. Uranium's 4 frame animation and green glow are kept.
- **New deepslate ores**, made the same way from the deepslate vanilla ores. Blocks `deepslate_ore_*` with 1.5 more
  hardness and the deepslate sound, like vanilla's. They come with models, loot (drops itself), the same tool-level
  tags, `c:ores/*`, `c:ores_in_ground/stone|deepslate`, and en/de/ru names. The ore features place them into
  `deepslate_ore_replaceables`.
- **Spawn ranges** reach 64 blocks deeper, with counts scaled so the density per layer is unchanged (table in
  `CHANGES_FROM_1.12.md` 4).
- **Recipes:** tin and lead smelting and blasting, and the titanium reaction chamber recipe, take `c:ores/*` tags.
- **Ingots and nuggets:** the 1.21 iron ingot and iron nugget shapes, recoloured from each Techguns texture's own
  colours.
- **Crates:** the armor, explosive and medical crate sides keep their markings on 1.21 oak, spruce and birch planks.
- **Ore clusters** (all 9): their background was 1.12 stone, or netherrack for the nether crystal, at 2x with shading
  painted on. Pixels that are the old base times one brightness factor became 1.21 stone at 2x times that factor.
  Coloured glow pixels close to the old base kept their colour difference on the new base. For netherrack this was
  limited to red pixels, so the crystals don't pick up netherrack noise. The clusters, the 4 animation frames and the
  glows are unchanged.
- **Raw Titanium:** `oretitanium` keeps its id. It's named Raw Titanium (Rohtitan, Необработанный титан) and its icon
  is the raw iron shape in its colours.
- **Copper leftovers removed:** `ingotcopper` texture and model, `ore_copper` texture and model, and their lang keys.
  Copper is vanilla's.
- **Metal Blast Furnace:** English block, container and manual names. German and Russian didn't clash with vanilla.
- New `gametest/OreTests.java`. A model reference check found no broken references from this change; the 7 that
  exist were already there.
- Found: the `doOreGen*` configs were never checked by worldgen (fixed in 9).
- Needs an in-game look: the new textures, and deepslate ores appearing below y 0.

### 9. Ore generation config options work (all 25 tests pass)

- New placement modifier `techguns:config_enabled` (`world/gen/ConfigOrePlacement.java`), registered on the mod bus.
  `{"type": "techguns:config_enabled", "ore": "tin"}` lets the feature through only while that `doOreGen*` option
  is on. It's the first modifier in each of the four ore placed features, so it covers both stone and deepslate.
- It reads the config as chunks generate, so a changed value applies to newly generated chunks without touching
  registries. Chunks that already exist keep their ore.
- `doOreGenCopper` is removed: Techguns has no copper ore. NeoForge drops the key from existing config files.
- New test `OreTests.oreGenConfigIsApplied`.

### 10. Leftover model files removed, model test, CI (all 26 tests pass)

- A check of every model file found 7 broken references. New test `ModelTests.allModelsAndTexturesResolve` showed
  none of them belongs to a registered block or item: they were unused 1.12 files. Deleted:
  - item models `bunkerdoor`, `door3x3`, `minigundrumepmty` (typo), `plasticbag`, `steelbarrel` (the registered items
    are `bunker_door`, `door3x3_*`, `minigundrumempty`, and there's no plastic bag or steel barrel item);
  - block models `testdoor_frame` and `testdoor_segment` (texture path `blocks/blocks/...`, no block);
  - 15 1.12 blockstate files with no block (`basicmachine`, `simplemachine`, `simplemachine2`, `multiblockmachine`,
    `oredrill`, `orecluster`, `debugblock`, `fluid`, and the gun/helicopter `.obj` forge blockstates; the `.obj`
    meshes themselves are still used by the renderers), plus the backups `concrete.json.off` and
    `explosive_charge.json.old`.
- The test checks that every registered Techguns block's blockstate, every item's model, and every model, parent
  and texture they reach exist, and that every blockstate file has a block. A test run with `ingottin.png` renamed
  failed with exactly that one problem.
- CI: `.github/workflows/build.yml` runs `./gradlew build` and `./gradlew runGameTestServer` on Java 21 for each push
  and pull request, and uploads the game test logs on failure. It doesn't upload the jar, because the
  license doesn't allow distributing builds. `gradlew` is now marked
  executable in git (it was 644, which fails on Linux). A failed game test makes the Gradle run exit non-zero
  (checked locally), so it fails the job.

## Where things stand

- Every structure 1.12 actually spawned is now ported: all scanned structures, the alien bug nest, the military base,
  the castle and the desert oil cluster.
- Not ported:
  - the nether dungeon and the tech fortress, which 1.12 never spawned;
  - the dev-only dungeon scanner/generator blocks, debug markers, and the building scan / worldgen test tools.
- JEI: every machine already has a category, so nothing new was needed.
- Entry 5 needs an in-game look:
  - Shishkebap flames, first and third person;
  - reload and charge sounds following the player;
  - the charge sound stopping when you let go;
  - swapping two guns between hands;
  - the mining outlines.
- Nothing from this session has been checked in game. The game tests only prove the code runs without errors. In game,
  check `/locate structure techguns:castle` and `techguns:military_base`, and `techguns:desert_oil_cluster` with an
  oil mod installed.
