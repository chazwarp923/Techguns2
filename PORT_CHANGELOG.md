# Port changelog (unattended session)

Running log of changes made while working unattended, for review. Everything here is committed on `port/neoforge-1.21.1`.
Newest entries at the bottom. Every entry was compile-checked (`gradlew compileJava processResources`); an entry's
heading says whether it was checked in game. Since entry 30, the client test harness (`runClientTest`) also covers
models, textures, sounds, particles, structure search and screenshots of every item, gun, block, mob and screen.

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

### 11. Vanilla /team support (all 28 tests pass; not tested in game)

- `factions/TGTeams` now checks vanilla scoreboard teams alongside FTB Teams, and either one can say yes. Same
  `/team` means team member, and so also ally (vanilla has no ally rank). Two different `/team`s means enemies,
  unless FTB Teams makes them allies. No team means neutral.
- The owner can be offline: the UUID becomes a scoreboard name through the online player list, then the server's
  profile cache. Unknown players have no team.
- Removed the "no FTB Teams, so everyone counts as a team member" fallback. The security button now cycles all four
  levels and the turret PvP button all five settings, with or without FTB Teams. In a world that had FTB Teams
  removed, a machine left on security 1 or 2 now admits only the owner and their `/team`, where before it admitted
  everyone.
- Guns needed no change. Techguns damage sources name the shooting player as attacker, so `ServerPlayer.hurt`
  applies the team's friendly-fire rule, as for arrows. NPCs and turrets use vanilla targeting, which already skips
  entities on the same scoreboard team.
- New `gametest/TeamTests.java`, with `vanillaTeamsCount` and `gunDamageHonoursFriendlyFire`. The test server has
  no profile cache, so the team test supplies the name lookup (`TGTeams.setNameLookupForTests`). A run with the
  same-team check broken failed exactly the teammate checks.

### 12. Nuke rockets, ammo switch loading, gun sound direction, atlas sprites (all 29 tests pass; sound not tested in game)

- **Reported:** Tactical Nuke rockets wouldn't load into the rocket launcher. The ammo switch recipe
  (`techguns:ammo_change_crafting`) did set the nuke variant, but it emptied the gun and used up the nuke in the
  grid. 1.12 loaded that item into the gun. With one nuke rocket there was nothing left to reload with. This
  affected every ammo switch (incendiary and explosive magazines too). `AmmoSwitchRecipe.assemble` now loads the
  ingredient: a full clip for guns that reload from one item, `clipsize / ammoCount` otherwise (1.12 gave 1, which
  is the same for those guns). Ammo the gun held is still refunded. Only the plain Rocket Launcher takes nukes; the
  guided missile launcher and Grim Reaper don't, as in 1.12.
- New test `GameplayTests.rocketLauncherSwitchesToNukes`: the recipe exists, gives the nuke variant and loads it,
  and a reload takes a nuke rocket, not a plain one.
- **Reported:** gun sounds always came from the left. `TGSound` placed gun position sounds at
  `polarOffsetXZ(x, z, 1, yaw)`, one block to the player's left (same code as 1.12). The local player's own gun
  sounds are now listener-relative (centred), and other entities' come from their middle.
- **Reported:** bullet projectiles showed the missing texture. `bullet1.png`, `dirt3x3.png`, `dustclouds02.png` and
  `muzzleflash_minigun.png` failed with FileNotFoundException at 21:34:54, the same second my `processResources` run
  rewrote `build/resources/main` while the dev client was open. Entity and FX textures load on first use and stay
  missing for the session. Not a mod bug: F3+T or a restart fixes it, and a jar can't hit it.
- The same log showed real startup warnings: the steam, T3 and T4 armor item models used
  `techguns:models/armor/*` as particle sprites, which aren't in the block atlas. The nether metal lava border used
  1.12's `minecraft:blocks/lava_still`. Added the three armor textures to `assets/minecraft/atlases/blocks.json` as
  single sprites, and changed the lava path to `minecraft:block/lava_still`.
- `ModelTests` now also checks that each model texture is in the block atlas (our directory and single sources,
  vanilla's `block/` and `item/`). With the two fixes reverted it reported exactly those 5 problems.

### 13. Jade overlay (all 30 tests pass; overlay not tested in game)

- Optional dependency on Jade 15.10.6 (Modrinth maven): `compileOnly` and `localRuntime` in `build.gradle`,
  `jade_version` in `gradle.properties`, optional `jade` entry in `neoforge.mods.toml`.
- `plugins/jade/TGJadePlugin` (`@WailaPlugin`, loaded only by Jade) registers one provider, `techguns:machine`. Its
  server side runs for every `BasicOwnedTileEnt` and multiblock part, and its client side draws:
  - a progress bar with "Progress: N%" while a machine works;
  - the owner's name and the security level, in the same colours as the machine GUI.
- `plugins/jade/MachineInfo` writes that data on the server. It has no Jade imports, so the game test can call it.
  A multiblock part shows its controller, and an unformed part shows nothing. Owner names come from
  `TGTeams.lookupName` (online players, then the profile cache), now public as `TGTeams.playerName`.
- No energy, item or fluid code: Jade's universal providers read the NeoForge capabilities the machines already
  expose, including through multiblock parts.
- Lang: `techguns.jade.progress` and `config.jade.plugin_techguns.machine`, in en/de/ru.
- New test `GameplayTests.jadeMachineInfo`. The test server log shows Jade loading `TGJadePlugin` with no errors.

### 14. Waterlogging (all 31 tests pass; not tested in game)

- `BlockSandbags` and `BlockCamoNet` (wall and top, all camo types) implement `SimpleWaterloggedBlock` with
  `waterlogged`: set from the fluid at the placement position, water ticks scheduled in `updateShape`, and
  `getFluidState` returns a water source. Bucket fill and empty come from `SimpleWaterloggedBlock`. Their
  blockstates are multipart and don't mention the property, so no model changes were needed.
- Ladders (`BlockTGLadder`, `BlockSlimyLadder`) extend vanilla `LadderBlock`, which was already waterloggable.
- Metal panels (containers, large panel, steel frames) are full cubes, so waterlogging doesn't apply.
- New test `GameplayTests.waterloggableBlocks`: all 12 blocks have the property and hold water when waterlogged, and
  sandbags and camo nets placed into a water source start waterlogged (but not in air).
- **Reported:** ladder items rendered outside their inventory slot. The four Techguns ladder items used the 3D block
  model (`ladder_base`, which sits against one edge of the block) with 1.12's GUI rotation and offset. They're now
  flat `item/generated` sprites of the ladder texture, like the vanilla ladder and the slimy ladder. The 1.12
  ladder textures fill their whole square, so the GUI view is scaled to 0.8 to leave a margin in the slot. Placed
  ladders are unchanged.

### 15. Progression advancements (all 32 tests pass; not tested in game)

- 26 advancements in `data/techguns/advancement/main/`, generated by a script (kept outside the repo). They follow
  the manual's guide entries (tier 0 to 3). The root has the dark grey concrete background and is granted on
  joining (`minecraft:tick`, no toast or chat message). **Reported:** the tab was missing in game. The root first
  needed an iron ingot, and Minecraft only shows a tab once its root is earned, as with vanilla's Nether tab. Every other advancement is a task, goal or challenge on `inventory_changed`; the Cyber Demon one uses
  `player_killed_entity`. "Full set" advancements need all four pieces in the inventory, and any one listed set
  counts. The Fabricator and Reaction Chamber steps check for the controller item: a multiblock-formed trigger
  would need code.
- Titles and descriptions as `advancements.techguns.<id>.title/description`, in en, de and ru.
- New test `DataTests.advancementsLoad`: every file loaded, its parent chain reaches `main/root`, and the keys
  exist in all three languages, and the root is granted on joining. It also checks every item and entity id by hand, because 1.21.1 loaded a criterion
  with `techguns:no_such_item` without any log line ("Loaded 1583 advancements"). With that typo planted, the test
  failed with "jetpack: unknown item techguns:no_such_item".

### 16. Compat recipes for Create, Mekanism and Immersive Engineering (33 tests pass in every setup; not tested in game)

- 19 recipes in `data/techguns/recipe/compat/<modid>/`, generated by a script (kept outside the repo). Each one's
  first condition is `neoforge:mod_loaded` for its mod; some also have `neoforge:not mod_loaded` for mods that
  already handle the same input. The formats were copied from each mod's own recipes (Create 6.0.10, Mekanism
  10.7.19, IE 12.4.2).
- Surveyed what the mods already do: all three process Techguns ores and crush Techguns ingots through `c:` tags,
  and IE's plate recipes have `c:plates/*` tag results, so they already make Techguns tin and bronze plates. Found a
  real gap: Create crushes Techguns tin, lead and uranium ore, but its smelting and washing recipes for the crushed
  ore only exist for Mekanism, IE, IC2, Thermal and Oreganized metals. Without one of those the crushed ore was
  useless; the new recipes fill exactly that case.
- `build.gradle`: `-Pcompat_mods=true` (or a list such as `create`) adds the mods to the dev runs from the Modrinth
  maven. Off by default. Create 6.0.10 needs Ponder 1.0.82+, so the dev Ponder moved from 1.0.69
  (`Ponder-NeoForge-1.21.1`) to 1.0.92 (`net.createmod.ponder:ponder-neoforge`) and Flywheel from 1.0.0-9 to 1.0.7.
  The Ponder scenes compile unchanged.
- New test `DataTests.compatRecipes`. It checks that every file is gated on its folder's mod and loads exactly when
  its conditions hold. For single-input smelting, blasting, pressing and washing it also checks that no other
  recipe of that type already takes the input. Runs: all mods 11 loaded, Create only 14, Mekanism only 1, IE only 4,
  none 0. Planted bugs (a renamed Mekanism field, and the steel press without its IE exclusion) failed with
  "should load but didn't" and "duplicates create:pressing/compat/immersiveengineering/plate_steel".
- CI runs the game tests three times: plain, all compat mods, and Create only. A crash while loading mods made
  `runGameTestServer` exit 0, so each step now also requires "All N required tests passed" in its log.

### 17. Gunfire vibrations (all 37 tests pass; not tested in game)

- New game event `techguns:gunfire` (`TGGameEvents`, radius 16). It's in `#minecraft:vibrations` and
  `#minecraft:warden_can_listen`, and has frequency 3 in the `neoforge:vibration_frequencies` data map (NeoForge
  reads frequencies from there; an unlisted event would be 0). It's a separate event because vanilla's
  `projectile_shoot` is in `#minecraft:ignore_vibrations_sneaking`, and a gunshot shouldn't be hidden by sneaking.
- Sent once per shot (not per shotgun pellet) at the shooter's eyes, from `GenericGun.shootGun` (players, NPCs,
  turrets) and `GenericGunCharge.releaseUsing` (charged shots).
- Vanilla events elsewhere: a reload sends `item_interact_finish` (from `startReloadEffects`, so both reload paths).
  `TGExplosion` sends `explode`, since it doesn't go through vanilla's `Explosion.explode()`. A thrown grenade sends
  `projectile_shoot`. A bullet hitting a block sends `projectile_land`. Bullets are plain entities, not vanilla
  `Projectile`s, so vanilla sent none of these; the shooter is named as the source, as vanilla does with an arrow's
  owner.
- **Fixed a crash found by the new test.** A `TGExplosion` with neither a projectile nor an exploder threw a
  NullPointerException building its damage source (`TGDamageSource.levelOf`). The bioblob's poison burst had the
  same bug when it had no attacker. Both now pass the world in through new `causeExplosionDamage(Level, ...)` and
  `causePoisonDamage(Level, ...)` overloads.
- Four new tests in `GameplayTests`: a sneaking fake player fires a pistol 5 blocks from a sculk sensor (aimed up,
  so the bullet lands far away), a standing reload, a Techguns explosion, and the event's tags and frequency.
  Planted bugs (gunfire sent as `projectile_shoot`; the explosion event removed) failed with "didn't hear a sneaking
  player's gunshot" and "didn't hear an explosion".

### 18. Mekanism radiation bridge (all 38 tests pass with and without Mekanism; not tested in game)

- `techguns.plugins.mekanism.MekanismRadiationBridge`, registered only when Mekanism is loaded. It compiles against
  Mekanism's API jar (`mekanism:Mekanism:1.21.1-10.7.19.85:api` from modmaven.dev, `compileOnly`). The item
  capability `mekanism:radiation_shielding` isn't in the API (it's in `mekanism.common.capabilities.Capabilities`),
  so the bridge creates it by the same name and type, which NeoForge resolves to the same instance.
- Techguns armor to Mekanism: every Techguns armor piece with RAD resistance, and the gas mask, gets the shielding
  capability: `min(1, resistance) x` the slot's share of Mekanism's hazmat suit (0.25/0.4/0.2/0.15, read from
  `ItemHazmatSuitArmor.getShieldingByArmor`). A broken piece gives 0, like its RAD resistance. Mekanism adds up armor
  and Curios slots and caps at 1 (`RadiationUtil.getRadiationResistance`), so the gas mask in its Curios slot counts.
- Mekanism to Techguns: an `ItemAttributeModifierEvent` gives other mods' armor with the capability
  `RADIATION_RESISTANCE = shielding / slot share` in its slot. Techguns items are skipped, since they already have
  their own resistance and would otherwise count twice.
- The doses aren't merged. Shared cures came next (entry 19).
- New test `GameplayTests.mekanismRadiationBridge` (helper `MekanismRadiationChecks`, only loaded with Mekanism). It
  checks shielding values, the RAD resistance of the four Mekanism hazmat pieces, and end to end: Mekanism's
  `IRadiationManager.radiate(player, 1.0)` gives a bare player 1 Sv and a player in full Techguns hazmat 0. With
  the bridge's registration commented out, all ten checks failed ("full Techguns hazmat let 1.0 Sv through").
  CI's all-compat-mods run covers it.

### 19. Shared radiation cures with Mekanism (all 38 tests pass with and without Mekanism; not tested in game)

- Asked how to link the doses; chose "two doses, shared cures" over making either mod's dose the only one or
  mirroring exposure into both.
- `TGRadiationSystem.externalRadRegeneration`: a hook `RadRegenerationEffect` calls with the RAD it just removed.
  The Mekanism bridge sets it, so core code still has no Mekanism references.
- Cures: Mekanism's dose severity is `log10(dose / 1e-5) / log10(100 / 1e-5)` (`RadiationScale`, the minimum
  magnitude from `IRadiationManager`). The bridge maps 1000 RAD to that whole range: removing N RAD multiplies the
  Mekanism dose by `10^(-7 x N / 1000)`, floored at Mekanism's baseline. Rad-Away (300 RAD): 10 Sv becomes 0.079 Sv.
- Rad pills: while the RAD resistance effect is active, a player tick takes back `min(1, (amplifier + 1) / 4)` of
  any Mekanism dose gained since the last check (rad pills III: 75%). Only the effect counts, since armor already
  shields through Mekanism. The last dose is kept in a `WeakHashMap` per player.
- Test additions in `MekanismRadiationChecks`: Rad-Away ticks take a player from 400 to 100 RAD and from 10 Sv to
  the expected 0.0794 Sv; with rad pills III, Mekanism's `radiate(player, 1.0)` adds 0.25 Sv. Planted bugs (hook not
  set, effect blocking 0) failed with "10.0 instead of 0.0794" and "1.0 instead of 0.25".

### 20. Armor and gear bonuses as vanilla attributes (all 39 tests pass; not tested in game)

- `GenericArmor.getDefaultAttributeModifiers` adds health, knockback resistance, jump strength, safe fall distance,
  fall damage multiplier, block break speed, submerged mining speed and creative flight next to armor and RAD
  resistance. The values come from `getBonus(stack, type, false, null)`, so powered armor without power and broken
  armor give their reduced values; a power change changes the stack, so vanilla re-applies them. Percentages use
  `ADD_MULTIPLIED_BASE`, which sums across pieces like 1.12 did.
- `TGCurioItem.getAttributeModifiers` gives the antigrav pack `creative_flight` while it has charge and the working
  gloves `block_break_speed`, next to the gas mask's RAD resistance.
- `ArmorEvents` no longer sets health and knockback modifiers each tick or toggles `mayfly`: NeoForge's
  `Player.mayFly()` reads `creative_flight`, and `ServerPlayer` ends the flight when it drops. The jump, fall and
  break speed events now only use powered armor's power, plus the jump pack's toggle-dependent boost and cushion
  (`ArmorBonusUtil.getSlotBonus`). Sprint-doubled speed, step assist, night vision and cooling are unchanged.
- The hand-written tooltip lines for the moved bonuses are gone (the attribute list shows them).
- New test `armorBonusAttributes` applies each item's modifiers as equipping does and checks the totals. Planted bugs
  (armor jump bonus 0, antigrav flight never given) failed with "scout jump strength: 0.42 instead of 0.52; the
  antigrav pack doesn't let the player fly".

### 21. Gun stats as a data map (all 41 tests pass; not tested in game)

- `GunStat` (items/guns): 22 named stats, each reading and writing a `GenericGun` field. Each stat records whether
  it's a whole number, its minimum, and which guns have it (shotgun, charge or melee tool only).
- `GunStats`: the `techguns:gun_stats` item data map (`Map<GunStat, Double>`, synced with mandatory sync). On
  `DataMapsUpdatedEvent` (server reload and client sync) every gun is reset to its code values, then the map is
  applied. Code values are captured in common setup, before any data map exists.
- `data/techguns/data_maps/item/gun_stats.json` lists all 42 guns with their code values. The test
  `gunStatsMatchCode` keeps it equal to TGuns and writes the correct file when they differ, which is also how it
  was generated.
- `gunStatsOverride` feeds `GunStats.apply` an override map: damage 99, fire delay 2.6 (rounds to 3), bullet speed
  -1 (raised to 0.01), pellets on the pistol (ignored), pellets on the combat shotgun; then an empty map, which must
  restore the code values. Planted bugs (shipped damage drifting from the code; no reset before applying) failed
  with "pistol damage 9.0 instead of 8.0" and "pistol damage once the entry is gone: 99.0 instead of 8.0". The
  first mutation check ran both bugs together and passed, because code values were then captured after the JSON
  was applied. That's why they're now captured in common setup.
- Removed the 1.12 CraftTweaker leftovers `EnumGunStat` and `GenericGun.setGunStat`, which nothing called.
- Magazine size isn't a stat: it's the item's max durability, fixed at registration.

### 22. Static machine parts as JSON block models (all 42 tests pass; not tested in game)

- Only four machines had Java models: the ammo press, metal press, chemical lab and fabricator (plus the turret
  base). The proposal's "77 files, 18k lines" counted guns, armor and NPCs too. The fabricator is left as it is: it's
  a formed multiblock drawn from its controller and reaches beyond one block.
- Converter (`machine_models.py`, not committed; the JSON is the source now):
  - parses the Techne-style constructors (`new ModelRenderer(this, u, v)`, `addBox`, `setRotationPoint`,
    `setRotation`, `mirror` in statement order);
  - takes the unconditional `X.render(scale)` calls at the start of `render()` as the static parts;
  - rebuilds each `ModelPart.Cube` polygon with vanilla's UV layout under RenderMachine's transform;
  - for each face, searches the JSON direction, UV order and UV rotation, and the element's 45 degree rotation sign,
    and keeps only a face whose FaceBakery bake (re-implemented from the 1.21.1 source) has the same 4 positions,
    texture coordinates and facing;
  - folds 90 degree Y rotations into the box and keeps 45 degree ones as JSON element rotations. No other rotations
    were present.
  - Result: ammo press 31 elements, metal press 29, chem lab 46 plus 11 glass (translucent child of a
    `neoforge:composite` model). The item models add the moving parts at rest (ammo press pistons, metal press
    plungers) and use `item/basicmachine`'s display transforms.
- Java models keep only the moving parts (about 940 lines removed). The three blocks render with `RenderShape.MODEL`,
  their blockstates point at the new models, and the item BEWLRs and `RenderMachine.ItemRenderer` are gone.
- Rotation: `BasicTGTileEntity.getModelData()` reports `ROTATION`. `readClientData` re-meshes the block when the
  rotation changes. `RotatedBlockModel` (wrapped in `ModelEvent.ModifyBakingResult`) turns the quads by -90 x r
  degrees around the block centre, with quad directions turned clockwise, cached per rotation and render type. That
  matches RenderMachine's `Y(90 x r)` inside its Z/Y flip.
- `ModelTests` now checks composite children and face texture variables, carrying variables down from inheriting
  models so templates (camo net tops, lamp base) pass. A bad texture variable planted in the chem lab glass failed
  with "models/block/chem_lab.json (glass) -> face texture #tax isn't defined".
- Reaction chamber (found in game while testing this): the formed chamber's flat panels showed a light and a dark
  triangle. `reactionchamber.obj` had smoothed vertex normals: 320 of 324 chamber triangles had diagonal normals,
  and 12 of those were nearest to "up" instead of their real side. NeoForge's OBJ loader takes each quad's direction,
  and so its flat shade, from those normals. Every face now has its geometric normal (23 distinct). No other OBJ had
  the problem. New test `objNormalsMatchFaces` (42 tests) checks every OBJ face; the original file failed it with
  "330 faces with normals off the face".
- Needs an in-game look:
  - each machine next to how it looked before;
  - all four wrench rotations, with the moving parts lining up with the static ones;
  - the item icons;
  - the chem lab glass over the liquids.

### 23. Scanned structures as vanilla structure templates (all 43 tests pass; not tested in game)

- The 18 scanned structures were 1.12 text block lists (`assets/techguns/structures/`, x,y,z,palette index) with
  their palettes in `TGStructureDefs`. They're now `data/techguns/structure/scanned/<name>.nbt` templates.
  `ScannedStructure` reads a template from the server's template manager (again after `/reload`) and places it with
  the same 1.12 rules as before: clear above, foundation, attached blocks in a second pass, rotation around the
  centre.
- Each 1.12 palette entry that chose its block at placement became a named rule (`ScannedStructure.marker`) and a
  data-mode structure block with that name in the template. There are 31 rules over 10 structures; 8 structures have
  none.
- Door halves and double chests are now set in the template, so `pairChests`, `matchDoorHalves` and
  `PlaceContext`'s chest and door maps are gone. `MBlock.WithData` loads a template block's block entity data, and
  containers with a loot table get their seed from the position as before. `MBlock.VanillaSpawner` is gone (the
  ghast spawner's vanilla spawner is template data now).
- Conversion: a temporary game test wrote each template from the old scan and palette at rotation 0. Block entities
  were placed at a scratch spot to save their data, and the loot seed was dropped. Where a scan listed a position
  twice (38 in the carrier, 1 each in the factory and train station: air, then a crate or spawner), the block placed
  last was kept. The train station's blocks reach y 6, so its template is 7 high; the code's size is unchanged.
- Checks:
  - A temporary test placed every structure the old way and from the template, in 4 rotations x 2 variants (seed,
    biome, ore roll), and compared every block state and block entity: 121,307 blocks, all equal. With template
    block entity data dropped, the same test failed (chests without loot tables).
  - After the palettes were rewritten as rules, placements at a fixed position hashed the same as with the
    exported version (136 placements; the desert oil cluster needs an oil mod, its 3 rules were checked by hand).
  - New test `scannedTemplatesMatchRules` (43 tests). A misspelled rule failed it with "no rule for the data
    marker "cobble_or_mossy"" and "rule "cobble_or_mosy" isn't used by the template".
- `netherdungeon_entrance` was removed: it had no palette anywhere, and 1.12 never placed the nether dungeon.
- Needs an in-game look: a few structures found with `/locate structure techguns:<name>`, and one loaded with a
  structure block (`techguns:scanned/police_station`) to see that the templates can be edited.

### 24. Taller castle staircases (all 44 tests pass; not tested in game)

- Reported in game: you hit your head on every step of the castle's staircases.
- Measured in the built castle (3 seeds): every step had exactly 3 or more free blocks above it, and many had
  exactly 3. That covered all the cobblestone steps underground and most spruce and stone brick steps. Three is the
  bare minimum, so you bump when jumping or sprinting up.
- The RAMP segment of each castle template (`ncdung1`, `nclower1`, `ncmid1`, `ncupper1`, `nctop1`) now has 4 free
  blocks above every step. That includes the top step, whose flight can go on into the next ramp. The ceiling is
  cut along the slope, 26 blocks in all; the underground ramp's ceiling now steps up with the stairs. Only the
  RAMP layer lines of the JSON changed.
- Kept: the two top slabs holding `ncmid1`'s loot chests over the second step (3.5 blocks: a top slab only fills
  the upper half).
- New test `castleStairHeadroom` (44 tests): builds the castle for 3 seeds and checks that every step of a flight
  has 3.5 blocks from its top to the lowest collision above. With the 1.12 templates it failed (3.0).
- Needs an in-game look: walk up a few castle staircases, including two ramps in a row.

### 25. Fabric port content: parts, raw ores, raw steel (all 44 tests pass; not tested in game)

- From the 1.19 Fabric port (last commit 2023-05-03): 10 new items, using its textures and English names.
  Advanced circuit board, power armor plating Mk2, nether armor plating, steel chainsaw blades, steel power hammer
  head, raw steel and raw obsidian steel ingots, raw tin, raw lead, raw uranium. Raw titanium is our `oretitanium`.
- Recipes match the Fabric port: 23 new recipes (parts, raw steel, raw ore smelting, raw uranium yellowcake, 8 T4
  armor pieces) and 17 changed (advanced circuit instead of elite in 12, steel heads in the 4 chainsaw and power hammer
  recipes, the gauss rifle grinder output).
- Ore loot tables: raw item with fortune, the ore with silk touch. New tags `c:circuits/advanced`,
  `c:raw_materials/{tin,lead,uranium}`.
- `OreTests.deepslateOresMatchStoneOres` checks both drops for stone and deepslate ores. A silk-touch entry that
  drops the raw item makes it fail.
- The Fabric port's blaster shotgun is our scatter beam rifle, already ported and named "Blaster Shotgun". It gets a
  recipe of our own (none in 1.12 or the Fabric port): the blaster rifle's, with two laser barrels instead of a
  barrel and a carbon plate; with an empty energy cell it comes unloaded. It grinds like the blaster rifle.
- The rest of the Fabric port content followed: ammo bench and ammo boxes (entry 26), grappling hook (entry 27),
  magic rifle (entry 28).

### 26. Fabric port content: ammo bench and ammo boxes (all 48 tests pass; checked in game)

- Ammo bench: block (Fabric model, textures and shape, faces the player), `AmmoBenchMenu` and `AmmoBenchScreen`.
  They follow vanilla's stonecutter menu and screen, with three input slots painted over its single one, as in
  the Fabric port. The Fabric port's craft packet was unfinished and unused; the recipe buttons use vanilla's menu
  button click. No block entity.
- Recipe type `techguns:ammo_bench` with `core`, `casing` and `powder` ingredients (the Fabric recipes had them
  fixed in code), 5 recipes with the Fabric counts. An item goes into an input slot when some recipe takes it
  there. JEI category added.
- Ammo boxes: `ItemAmmoBox`, contents in component `techguns:ammo_box_contents` (vanilla bundle contents). Capacity
  counts a full stack of anything as one stack; the Fabric port counted in items, so a box of 16-stacking items
  held 4x too many. Own tooltip drawn like the bundle's, since vanilla's marks a bundle full at one stack.
- `InventoryUtil`: reloading checks and takes ammo from the boxes after the ammo slots, before the main inventory;
  returned ammo and empty magazines go into the boxes after the ammo slots.
- New `AmmoBenchTests` (4 tests). The bench test caught a bug: in the result slot, `container` named the slot's own
  container, so taking the result used no inputs. Removing the box from the ammo use made the gun reload for free,
  and the reload test failed.
- Checked in game: the bench's screen and the bench model's facing are fine; the ammo box's tooltip, clicking ammo
  into it and reloading from it work.

### 27. Fabric port content: grappling hook (all 50 tests pass; works in game)

- `GrapplingHook` gun (Fabric stats): holding use fires `GrapplingHookProjectile`; letting go fires nothing.
  `startCharge` clears the fire delay that `GenericGunCharge.use` has just set, or the shot never happens; a game
  test caught it.
- `GrapplingHookProjectile`: the server decides. Its state (flying, on a block, on an entity, pulling an entity),
  target and anchor are synced entity data. Pulling sets the motion (`hurtMarked` sends it to players) with the
  Fabric speeds and limits, and clears fall distance. The hook goes when the shooter stops using the gun.
- Client: shooter id to hook map, so the gun model hides its hook and spins its wheel while the hook is out.
  `RenderGrapplingHookProjectile` draws the hook model and the steel wire (two crossed quads) from the gun.
- Models converted from the Fabric port by script. `ModelRenderer.addCuboid` takes Fabric's float sizes and
  per-axis expansion (same box: it scales about the part origin by (size + 2e) / size). The parts each rotate on one
  axis, so the rotation order difference doesn't matter.
- Sounds `guns.grapplinghookfire` and `guns.grapplinghookpull`, textures, lang, gun_stats entry; our own recipe.
- New `GrapplingHookTests` (2 tests). Removing the entity pull makes `hookPullsEntity` fail. The tests first counted
  the hooks of tests running alongside; they now count only their own player's.
- Tried in game: it works. Not looked at in detail: the wire's start at the gun in first and third person, the
  wheel spin, and how the pull feels (the Fabric port's speed of 2 blocks a tick).

### 28. Fabric port content: magic rifle (all 53 tests pass; checked in game)

- The Fabric port's magic rifle was unfinished (no names, recipes or sounds of its own). Ported: the gun (Fabric
  stats, 6 textures as camos), 3 magic bullets (ammo type `MAGIC_BULLETS`, variants `fire` and `lightning`, with
  ammo-change recipes), `MagicRifleProjectile` with the three kinds, the trail and explosion particle systems, and
  the charge circle. Names and recipes are ours.
- `MagicRifleProjectile`: kind and charge are in the spawn data and saved (the Fabric port read the charge back as
  a byte, so its clients always had 0). Explosions use `TGExplosion` with no block damage. It gained
  `excludedEntities` (the plain bolt leaves out the entity it hit, as the Fabric port's explosion did) and
  `entityFireTime` (the Fabric port's incendiary explosion; its block burning never ran, since that explosion broke
  no blocks). Unlike the Fabric port's plain bolt explosion, ours checks line of sight and the damage factor, like
  every other Techguns explosion. Fire time keeps the Fabric port's `3 + (int) charge * 7` (10 seconds only at full
  charge). Lightning lingers 5 to 20 ticks (half the Fabric port's 10 to 40, as it was too much in game), a strike every 8
  ticks and random ones around.
- fxlist: the Fabric port's magic rifle entries appended, without its 4 `Brightness` lines (a lighting key our
  particles don't have). 3 more fx textures came along. Fabric's charge particle `tfgcharge` doesn't exist in its
  fxlist, so it showed nothing; left out.
- Charge circle: `ScreenEffect` gained the Fabric fade types `RISE_SMOOTH` and `RISE_SMOOTH_AND_PULSE_FAST`,
  `setDynamicScale` (grows along the curve; its min and max are stored but unused, as in the Fabric port),
  `setDrawMode3p`, and a looping frame for rising fades. New `VariableScreenEffect` picks the effect by the loaded
  ammo variant. `RenderGunBase.setChargeFx` and `setChargeFXPos3P` draw it while charging, given the charge going
  on past 1 so it pulses while held.
- Model converted by script. One part has texture offset -16, kept as in the Fabric port.
- New `MagicRifleTests` (3 tests). Removing the hit-entity exclusion, the explosion fire or the lingering each makes
  its test fail.
- Checked in game: the model and camos, the charge circle, the trails and explosions are fine. The lightning was
  too much, so it now lingers half as long as in the Fabric port (5 to 20 ticks).

### 29. Fix: machine fronts textured wrong (ammo press, metal press, chemical laboratory) (all 53 tests pass; checked in game)

- Seen in game: the side of the ammo and metal press facing the player when placed had its textures mapped wrong.
- Cause: the script that made the JSON models in entry 22's commit (`c3a9421`) checked each face against its own
  copy of vanilla's `ModelPart.Cube`, and that copy had the south face's corners in the wrong order (`v6, v3, v4, v5`
  instead of `v3, v4, v5, v6`). Every face from a cube's south side, which the machine renderer turns to the
  block's north side (the front at placement), got its texture turned 90 degrees, and the check passed anyway.
- Fix: the script's cube copy now matches the vanilla source face for face (its JSON face baking was checked
  against `FaceBakery`, `BlockFaceUV` and `FaceInfo` too). The 6 models (block and item, all three machines) were
  made again from the Java models as they were before that commit. Only faces from cube south sides changed (north,
  or west/east on parts turned 90 degrees); the shapes didn't. The chemical laboratory's front had the same fault.

### 30. Automated client test harness, jetpack sounds (all 53 tests pass; client run: all 126 checks pass, no Techguns warnings in the log)

- `./gradlew runClientTest` starts a real client in its own game dir `run-clienttest/` (system property
  `techguns.clientTest`; `techguns.client.test.ClientTestHarness`). It makes a fresh world (seed 20261123,
  creative), works through a script on the client tick and quits by itself after about 7 minutes. Sound is muted.
- Checks: every Techguns item and block state has a model without missing textures (custom-renderer items are
  skipped); every gun and camo texture exists; every sound event is in sounds.json and its files exist; the
  particle effect lists load; every Techguns structure is found by the same search as `/locate`, in the overworld
  or the nether (the desert oil cluster only when an oil mod is loaded).
- Screenshots on a platform at y 200: every item in grids; every gun held, fired and in third person; every block
  entity block, machines in all 4 wrench rotations, and its screen if it has one; every mob; every particle
  effect; the Patchouli manual and the 3 Ponder scenes. A step that throws is reported and skipped.
- Output: `run-clienttest/clienttest/report.txt` (fails first; also Techguns errors and warnings from the log,
  including stack traces through Techguns code) and about 450 PNGs in `run-clienttest/clienttest/screenshots/`.
  `run-clienttest/` is git-ignored. The exit code is 0 even with fails: read the report's first line.
- It found, and these are fixed:
  - `guns.shishkebabfire` named two files `shishkebap_swing_b/c`, which don't exist (a 1.12 typo); only one of
    the three swing sounds played.
  - The explosive military crate's loot table pointed at `techguns:military_crate/explosives`; the table is
    `explosive`, so the crate dropped nothing when broken without silk touch.
  - The jetpack played no sounds in the port. Now, as in 1.12 (`TGExtendedPlayerClient#setJumpkeyPressed`), a
    boost plays `jetpack.start` and loops `jetpack.loop` on the player, and stopping ends the loop and plays the
    stop sound, all at 0.75 volume, for every player in view (`client.audio.JetpackLoopSound`, which follows the
    boost flag each client tick). 1.12 played `jetpack.end`, which sounds.json never had, so its stop sound was
    silent; `TGSounds.JETPACK_END` is now `jetpack.stop`, the entry with the `jetpack_stop_*` files. The harness
    checks the three sounds play.
  - `guns.cyberdemonblasterreload`, the nether blaster's reload, was registered but had no sounds.json entry, so
    it reloaded silently (in 1.12 too). There's no reload file of its own, so it uses the alien blaster's
    (`alienblaster_reload`), the gun with the same fire rate, clip and reload time. Our choice, not from 1.12.

### 31. Screenshot review; harness spawns equipped mobs and whole 3x3 doors

- Went through the client test screenshots. Fine: all 417 items (no missing textures), every machine in its 4
  rotations, the multiblock parts, every screen, the Jade overlay (owner, tank, energy), the Patchouli manual's
  category page, the 3 Ponder scenes, the mobs, the particle effects, and the Shishkebap flames in first and third
  person.
- Two harness gaps, not mod bugs, fixed in `ClientTestHarness`:
  - Mobs were made with `EntityType.create` and never finalized, so NPCs showed empty-handed. They now go through
    `EventHooks.finalizeMobSpawn` with `MobSpawnType.COMMAND`, as `/summon` does, and carry their guns and armor.
  - 3x3 doors were placed as their default state, an invisible slave block. The harness now places the whole door
    as `ItemDoor3x3` does (master in the middle) and clears the 3x3x3 around the show spot between blocks,
    master first, then removes loose items.
- Checked, no change: removing any slave block of a 3x3 door by any means (`/fill`, `/setblock`, a structure
  replacing it) makes the master drop the door item (`BlockDoor3x3.onRemove` calls `destroyBlock(master, true)`).
  Vanilla does the same: both commands place with flag 2, `Level.setBlock` clears `UPDATE_SUPPRESS_DROPS` before
  the neighbour shape updates, and `Block.updateOrDestroy` destroys the other door or bed half with drops.
- What the screenshots can't show: the grappling hook's wire (it fires only while use is held, the harness clicks
  once), the chem lab's liquids (its tanks are empty), formed multiblocks (parts are placed one at a time), and
  anything that moves or makes sound.

### 32. Create, Mekanism and Immersive Engineering in the dev runs by default (53 tests pass; client run: all 126 checks pass)

- `gradle.properties`: `compat_mods=true`, so `runClient`, `runClientTest` and `runGameTestServer` load all three
  for testing the compat recipes and the Mekanism radiation bridge. `-Pcompat_mods=false` leaves them out; a list
  such as `-Pcompat_mods=create` still picks some.
- CI's plain game test run now passes `-Pcompat_mods=false`, so it still tests without them.

### 33. NeoForge requirement lowered to 21.1.265

- `neo_version=21.1.265`: the mod is now built and tested against 21.1.265, and the jar's `neoforge.mods.toml`
  requires `[21.1.265,)` (the range comes from `neo_version`). It compiles, all 53 game tests pass with the compat
  mods loaded, and the client test passes on 21.1.265.

### 34. Fix: antigrav start sound and ring for any creative flight (all 54 tests pass)

- **Reported:** the antigrav pack's start sound played for any creative flight. `ArmorEvents.tickFlight` played
  `items.antigravstart` and the `AntiGravRing` effect whenever the player started flying with NeoForge's
  `creative_flight` attribute above 0, whatever gave it. 1.12 also played them in creative mode with the pack on.
- Now only when the pack is what lets the player fly: `ArmorEvents.fliesByTechgunsGear` needs a Techguns
  `CREATIVE_FLIGHT` bonus (a charged antigrav pack) and `abilities.mayfly` off. Creative and spectator mode set
  `mayfly`, and another mod's `creative_flight` gives no Techguns bonus. Fly speed handling is unchanged.
- New test `GameplayTests.antigravSoundOnlyForPackFlight`: a charged pack in survival counts; the pack in creative,
  an empty pack, and another source's `creative_flight` don't. Without the `mayfly` check it fails with "creative
  flight with the pack on counts as pack flight".
- Seen while testing: `MagicRifleTests.lightningBoltLingers` failed once in about eight runs with "lightning
  strikes: 1". Fixed in entry 35.

### 35. Fix: lightning magic bullet lingered past what it hit (all 54 tests pass in every CI setup)

- `lightningBoltLingers` failed in CI (Create-only run of `ac227c0`) and now and then locally. Run 60 times in one
  server, with the test's entity search limited to its own structure, it failed every time: the bolt lingered at
  x 9.7 instead of the wall at x 6.
- Cause: `GenericProjectile.tick` read the motion before `onHit` and moved the projectile by it afterwards.
  `MagicRifleProjectile.onHit` puts a lingering lightning bolt at the hit spot with no motion, so the tick carried it
  one tick's travel (about 3.7 blocks) on, through the wall. In game, its follow-up strikes came down behind the
  wall or block it hit. The test only failed when nothing within 3 blocks behind the wall let a strike land.
- Fix: the tick reads the motion again after `onHit`, and doesn't turn a stopped projectile. Other projectiles
  don't change their motion in `onHit` (the grenade bounce spawns a new projectile), so they move as before.
- `MagicRifleTests`: entity searches cover only the test's own 7x9x7 structure plus a block (tests run side by side;
  the old 16 to 24 block box saw the neighbours' bolts and lightning), built from relative coordinates because
  `getBounds()` starts a block above relative y 0, where the strikes land, and lightning bolts have no size.
  `lightningBoltLingers` also checks that the bolt lingers at the wall. Without the fix all 61 copies failed with
  "the bolt lingers at (9.6, ...), not at the wall"; with it all pass.

## Where things stand

- Every structure 1.12 actually spawned is now ported: all scanned structures, the alien bug nest, the military base,
  the castle and the desert oil cluster. The client harness finds all of them with the `/locate` search (the desert
  oil cluster only with an oil mod).
- Not ported:
  - the nether dungeon and the tech fortress, which 1.12 never spawned;
  - the dev-only dungeon scanner/generator blocks, debug markers, and the building scan / worldgen test tools;
  - CraftTweaker support for gun, armor and death-effect stats (would need a new dependency; gun stats are a data
    map and machine recipes are datapack recipes already).
- JEI: every machine already has a category, so nothing new was needed.
- Checked in game: entries 26 to 30, and through the harness screenshots everything listed in entry 31.
- Still needs a look in game:
  - reload and charge sounds following the player, and the charge sound stopping when you let go;
  - swapping two guns between hands; charge guns and grenades not bobbing while in use;
  - the mining outlines;
  - walking up the castle staircases (entry 24);
  - the grappling hook's wire start, wheel spin and pull (entry 27);
  - formed multiblocks next to how they looked before, and the chem lab glass over liquids (entry 22);
  - a structure loaded with a structure block (entry 23);
  - the compat recipes and the Mekanism radiation bridge with those mods installed (entries 16, 18, 19).
