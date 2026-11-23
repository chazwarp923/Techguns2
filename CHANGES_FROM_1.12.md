# Techguns 1.21.1: changes from the 1.12.2 version

This port moves Techguns from Minecraft 1.12.2 / Forge to Minecraft 1.21.1 / NeoForge. Content and balance are kept
as close to 1.12.2 as the new game allows. This document lists what is different: what players will notice, then what
pack makers and server admins need to know, then the internal changes.

The 1.12.2 source is kept in `legacy/` for reference. It is not compiled.

---

## 1. Platform and dependencies

| | 1.12.2 | 1.21.1 |
|---|---|---|
| Minecraft / loader | 1.12.2, Forge 14.23.5.2807 | 1.21.1, NeoForge 21.1.267 |
| Java | 8 | 21 |
| Build | ForgeGradle | ModDevGradle |
| Mod version | `1.12.2-2.0.2.0` | `1.21.1-2.1.0.0` |
| Required mods | none | **Curios** (9.6.0+) |
| Optional mods | FTB Library (teams), JEI, CraftTweaker, Chisel, Thermal Expansion, Patchouli | FTB Teams, JEI, Patchouli, Ponder, Jade; recipes for Create, Mekanism and Immersive Engineering |

- **Curios is required.** The Techguns inventory tab was replaced by Curios slots (see 3.1).
- **FTB Teams** replaces FTB Library for the machine and turret security "team" levels.
- **Vanilla `/team` teams (new)** count too, with or without FTB Teams. Players on the owner's `/team` are team
  members (and allies), and players on a different `/team` are enemies. So all four machine security levels and all
  five turret PvP settings now work without any team mod. In 1.12 without FTB Library, the buttons only switched
  between public and owner-only, and between no PvP and everyone. Techguns gun and explosion damage already
  follows a `/team`'s friendly-fire setting, because the shooting player is the attacker, as with vanilla arrows.
- **JEI** shows recipes for every machine: ammo press, metal press, blast furnace, grinder, chem lab, reaction chamber,
  fabricator, charging station, ore drill, camo bench and upgrade bench. It is only needed at runtime.
- The dev client also loads Powah (to power machines for testing) and C2ME (to test worldgen under multithreaded
  chunk generation). The mod doesn't depend on either.

---

## 2. Registry names and world data

1.12 worlds cannot be converted: this is a new mod version for a new game version.

- **No more metadata.** In 1.12 many items and blocks were variants of one id separated by metadata, for example
  `techguns:itemshared`, `techguns:concrete`, `techguns:basicore` and `techguns:metalpanel`. Every variant now has its
  own id, for example `concrete_grey`, `concrete_brown_light_scaff`, `ladder_rusty`, `lamp_yellow`, `container_red`
  and `camonet_desert`. There are 362 items and 105 blocks, all translated.
- **The 3x3 door** (one block with a door type) is now four blocks: `door3x3_metal`, `door3x3_hangar`,
  `door3x3_hangar_down` and `door3x3_nether`. `bunkerdoor` was renamed `bunker_door`.
- **Machines** are separate blocks, for example `ammo_press`, `chem_lab` and `fabricator_controller`, instead of
  `basicmachine` / `simplemachine` / `multiblockmachine` with metadata.
- **Sound event names are lowercase**, as 1.21 requires.
- **Item NBT is now data components:** ammo, ammo variant, camo, mining head, charge and so on.
- **Player data** (toggles, radiation, food, safe mode and so on) is a NeoForge data attachment instead of a Forge
  capability, and is still kept on death.
- **Ore dictionary entries are now `c:` item tags**, such as `c:ingots/copper`. Recipes and ore clusters use tags,
  so other mods' metals and ores are accepted automatically.

---

## 3. Gameplay changes

### 3.1 Player inventory slots (Curios)
1.12 added its own inventory tab with face, back and hand slots, 8 ammo slots and 3 auto-food slots. These are now
Curios slots:
- face, back and hand items (gas mask, glider, jetpack, gloves and so on) use the Curios `head`, `back` and `hands`
  slots;
- the ammo and food slots are Techguns Curios slot types: `techguns_ammo` (8) and `techguns_food` (3);
- Curios handles syncing, rendering and dropping these slots on death.

Reload-from-slots, auto-feeding and ammo refunds work as before, using the Curios slots. The 1.12 config options
`DataWatcherID_FaceSlot`, `DataWatcherID_BackSlot` and `TechgunsGUI_TabID` are gone, because they only existed for the
old tab.

### 3.2 Guns and combat
- **Damage.** 1.12 replaced vanilla damage handling with a reflective copy of the code. The port does the same
  through NeoForge damage events: Techguns penetration against armor toughness, bypassing hit cooldowns and knockback
  multipliers. Techguns damage types are data-driven (`data/techguns/damage_type`), so death messages and damage-type
  tags work like vanilla's.
- **Muzzle-flash and TFG dynamic lights are not ported.** In 1.12 they only worked with the Albedo mod, which has no
  1.21 version.
- **Sounds.** Gun, reload and charge sounds follow the player and sit beside the gun, as in 1.12. The charge-up
  sound stops when charging stops.
- **Shishkebap.** Its flaming blade is shown while it has fuel. 1.12 switched it on and off with server packets; the
  client now does this itself.
- **Swapping hands** with guns in both hands carries their fire delays and recoil/reload animations along.
- **Ammo change crafting.** Crafting a gun with a different ammo type (for example a Rocket Launcher with a Tactical
  Nuke rocket) switches it to that ammo and loads the item used, and gives back the ammo it held, as in 1.12.
- **Gun sounds are no longer off to the left.** 1.12 played gun sounds one block to the side of the shooter's head,
  which 1.21's 3D audio pans fully left. Your own gun sounds now play centred, and other players' and mobs' gun
  sounds come from their position.
- **Vibrations (new).** Guns now set off sculk sensors and draw Wardens, which 1.12 didn't have. A shot sends its
  own `techguns:gunfire` vibration (frequency 3, like a bow shot) that sneaking does **not** hide, unlike vanilla's
  bow and crossbow shots. Players, NPCs and turrets all count. Reloading is a vanilla item-use vibration, so you
  can reload quietly while sneaking. Techguns explosions send vanilla's explosion vibration. Thrown grenades and
  bullets hitting blocks behave like a thrown snowball and a landing arrow. A Warden that hears a bullet land goes
  after the shooter.
- **Two-handed guns** block off-hand item, block and entity use, as in 1.12.
- **Safe mode.** The `techguns.allowunsafemode` permission now uses the NeoForge permission API. The
  `RestrictUnsafeModeToOP` config overrides it. Both are checked on toggle and on login.
- **Area mining tools** (mining drill with a radius) outline every block they would also break. The outline and the
  actual mining use the same rules.
- **Armor** stops working at 1 durability left, as in 1.12. Inventory icons are plain item models instead of the
  1.12 3D model overrides; legs and boots are fitted to the slot.

### 3.3 NPCs
- **Spawning** works as in 1.12: a placeholder spawn entry in each biome is replaced by an NPC picked by weight,
  depending on the danger level of the position (distance from world spawn and biome). The spawn weights and distance
  levels are still in the config.
- **The alien bug** is no longer a spider subclass. Its wall climbing was copied, so spider jockeys and other spider
  spawn effects no longer apply to it. It drops nothing.

### 3.4 Machines
- **Machine recipes are datapack JSON recipe types:** `techguns:metal_press`, `blast_furnace`, `grinder`,
  `fabricator`, `reaction_chamber`, `charging_station`, `chem_lab`, `camo_bench` and `upgrade_bench`. In 1.12 they were
  hard-coded in Java and changed through CraftTweaker. Now any datapack, or KubeJS / CraftTweaker through their
  datapack support, can add, change or remove them.
- **Power is Forge Energy (FE)**, which every 1.21 power mod uses. The `machinesNeedNoPower` config is unchanged.
- **The Blast Furnace is now the "Metal Blast Furnace"** (English name only, same id), because vanilla added its own
  Blast Furnace in 1.14.

### 3.5 Blocks
- **Waterlogging (new).** Sandbags and all six camo nets can hold water, so they work in and next to water like
  vanilla fences and walls. They become waterlogged when placed into water or filled with a bucket. Techguns
  ladders already could, because they are based on the vanilla ladder. Metal panels, containers and steel frames are
  full blocks, which can't hold water (like stone).

### 3.6 Advancements (new)
- A **Techguns advancement tab** follows the manual's progression guide, tiers 0 to 3: Hand Cannon, first armor
  set and machines; steel, obsidian steel, rifles and Nether-ready armor; Nether quartz, Jetpack, Cyber Demon and
  Cybernetic Parts; then Fabricator, Reaction Chamber, titanium, enriched uranium, tier 3 armor, the Tactical Nuke
  and the Anti Gravity Device. 26 advancements, data only, in English, German and Russian.

### 3.7 Radiation
Unchanged in behaviour: radioactive items in the inventory, radiation sickness, Rad-Away, rad pills, radiation
resistance, radiation potions (brewing recipes) and the HUD. Items get their radiation level through the radiation
registry as in 1.12, and the tooltip shows the level.

**With Mekanism installed (new),** the two radiation systems are linked. The doses stay separate (Techguns RAD and
Mekanism's Sv, each with its own meter and sickness), but protection and cures work on both:
- Techguns armor shields against Mekanism radiation. A piece's RAD resistance of 1 (a hazmat piece) counts as that
  slot's full share of Mekanism's hazmat suit (helmet 25%, chestplate 40%, leggings 20%, boots 15%); less is
  scaled down, more is capped. Full Techguns hazmat, HEV, Nether Combat or Power Armor Mk2 shields 100%, full Steam
  or Ranger Veteran armor 50%, Commando or Advanced Combat 25%. The gas mask counts as a helmet share.
- Mekanism's hazmat suit (and MekaSuit pieces with the radiation shielding unit) gives Techguns RAD resistance,
  1 per hazmat piece, the same as Techguns hazmat. It shows in the item tooltip.
- Rad-Away, rad pills and the RAD regeneration potion also lower the Mekanism dose, by the same share of
  Mekanism's scale as they take off ours (1000 RAD is the whole scale). Mekanism's scale is logarithmic, so
  Rad-Away's 300 RAD takes the dose down about a hundredfold, for example from 10 Sv to 0.08 Sv.
- The RAD resistance effect from rad pills and the RAD resistance potion also blocks Mekanism exposure:
  resistance / 4, so rad pills (III) block 75%. A full hazmat suit is 4 in our system and 100% in Mekanism's.

---

## 4. World generation

1.12 placed structures from its own world generator on a grid. They now use Minecraft's structure system, so
`/locate structure techguns:<name>` works. Biome filters are biome tags and spacing is vanilla random-spread
placement. All of it is datapack JSON in `data/techguns/worldgen/`.

### 4.1 Structures
- **Scanned structures** (houses, police station, gas station, factory, train station, survivor hideout, aircraft
  carrier and the nether buildings) are built from the original 1.12 block lists. The 1.12 blocks are converted
  through the vanilla flattening table plus a Techguns block table (`LegacyBlocks`). Each structure is placed one
  chunk at a time.
- **Code-built structures** are ported line by line: the alien bug nest, the military base and the castle. They're
  generated once from the structure's seed, so every chunk gets the same result.
- **The castle** uses the 1.12 dungeon templates, converted from the Java-serialized `.ser` files to JSON with
  `tools/dungeon_converter`.
- **The desert oil cluster** only generates when an oil fluid exists (`FluidListOilWorldspawn`), as in 1.12.
- **Structure sets:**
  - `overworld_small`;
  - `overworld_medium`: medium buildings 35, bug nest 20, desert oil cluster 15, military base 8, castle 8;
  - `nether_small` and `nether_medium`;
  - `aircraft_carrier`.

  These replace the 1.12 small/medium/big weights. The `StructureSpawnWeight{Small,Medium,Big}` config options were
  removed; use a datapack to change the weights or spacing. `SpawnStructures` and `SpawnOreClusterStructures` still
  switch structures on and off.
- **Ores** (tin, lead, titanium, uranium) are vanilla placed features, added through a NeoForge biome modifier.
  Copper ore is vanilla's, so `doOreGenCopper` is gone. `doOreGenTin`, `doOreGenLead`, `doOreGenTitanium` and
  `doOreGenUranium` work as in 1.12: each ore's placed feature starts with a `techguns:config_enabled` check, read
  as chunks generate (stone and deepslate variants both). Datapacks can use the same check in their own features.
- **Deepslate ores (new).** Each ore has a deepslate variant, placed into deepslate like vanilla's. Each spawn range
  reaches 64 blocks deeper (the world floor moved from y 0 to y -64). The count per chunk went up in proportion,
  so the density per layer is the same as 1.12:

  | Ore | 1.12 | 1.21.1 |
  |---|---|---|
  | Tin | y 5 to 59, 10 per chunk | y -59 to 59, 22 per chunk |
  | Lead | y 5 to 49, 8 per chunk | y -59 to 49, 19 per chunk |
  | Titanium | y 4 to 31, 5 per chunk | y -60 to 31, 16 per chunk |
  | Uranium | y 4 to 23, 4 per chunk | y -60 to 23, 17 per chunk |

  The tin and lead smelting recipes and the titanium reaction chamber recipe take the `c:ores/...` tags, so both
  variants (and other mods' ores) work.

### 4.2 Fixes compared to 1.12
- Doors in structures always have matching halves. 1.12 only stored part of each door's state in each half, which
  could produce mismatched or split doors after conversion.
- Side-by-side single chests become double chests, and fences, walls, panes and sandbags connect as soon as they are
  placed instead of after a block update.
- The dungeon maze generator checks the grid edge after an upward ramp; 1.12 could crash there.
- Skulls in templates become skeleton skulls with the right placement. The flattening table cannot convert 1.12 skulls.

### 4.3 Deliberate differences
- **The military camp's ground is levelled completely.** 1.12 allowed 4 blocks of height difference and smoothed it.
  Vanilla terrain adaptation blends the surroundings in, and trees inside the camp are cleared.
- **Tents use the biome's camo net colour.** 1.12 worked out the colour but always placed the woodland net.
- The alien bug nest keeps a 1.12 bug in its slime-ladder shaft, which fills every free spot next to a wall, because
  that is how 1.12 nests looked.

### 4.4 Not ported
- The **nether dungeon** and the **tech fortress**. 1.12 never spawned them: the nether dungeon was commented out and
  no preset used the tech fortress. Their templates are converted and load if someone wants to add them.
- The dev-only structure tools: dungeon scanner/generator blocks, debug marker blocks, building scan tool and world-gen
  test tool.

---

## 5. Configuration

The config is now NeoForge TOML (`techguns-common.toml`, `techguns-client.toml`). Option names are kept, grouped under
sections such as `general.RestrictUnsafeModeToOP`.

- **Removed:**
  - `StructureSpawnWeightSmall`, `StructureSpawnWeightMedium` and `StructureSpawnWeightBig`: structure sets replace
    them (see 4.1);
  - `DataWatcherID_FaceSlot`, `DataWatcherID_BackSlot` and `TechgunsGUI_TabID`: Curios replaces the tab (see 3.1).
- **Renamed:** `Techguns Spawnweight Overworld` / `Techguns Spawnweight NEther` became
  `Techguns_Spawnweight_Overworld` / `Techguns_Spawnweight_NEther`, because TOML keys can't contain spaces.
- Everything else (86 options in 1.12) keeps its name and meaning.

---

## 6. Mod compatibility

| 1.12 plugin | 1.21.1 |
|---|---|
| JEI | Ported. Recipe categories for all machines. |
| FTB Library teams | Ported to FTB Teams (optional). |
| Patchouli (Techguns Manual) | Ported. Since Patchouli 1.20, `book.json` lives in `data/` and the pages in `assets/`. The book now uses that layout, with flattened ids, Curios and FE wording, and fixed broken links. Craft it from an iron ingot, a glass pane and stone bullets or pistol rounds. |
| Jade (new) | Optional. Looking at a machine (or any part of a formed multiblock) shows its progress, owner and security level; Jade's own providers add energy, items and fluids. Toggle under Jade's plugin settings ("Techguns Machines"). |
| Mekanism radiation (new) | Optional. The two radiation systems are linked: armor protects against both, and Rad-Away and rad pills work on both; see 3.7. |
| Create, Mekanism, Immersive Engineering (new) | Data-only recipes in `data/techguns/recipe/compat/`, each loaded only when its mod is installed. All three already process Techguns ores and ingots through the shared `c:` tags; these fill the gaps. **Create:** presses tin, bronze, obsidian steel and titanium ingots into Techguns plates (steel and lead too, unless IE is installed: Create already presses those into IE plates). Heated mixing makes bronze (3 copper + 1 tin = 4) and obsidian steel (steel + obsidian). Create crushes Techguns lead, tin and uranium ores but only had uses for the crushed ore with another metal mod installed; without one, crushed tin and lead now smelt into Techguns ingots, crushed lead washes into lead nuggets and crushed uranium into yellowcake. **Immersive Engineering:** metal press plates for obsidian steel and titanium (IE already makes Techguns tin and bronze plates); obsidian steel in the alloy kiln and arc furnace. **Mekanism:** obsidian steel in the combiner. Titanium processing and enriched uranium stay with the Techguns reaction chamber. |
| Ponder (new) | Optional. Hold W over a fabricator, reaction chamber or ore drill part for an animated build guide: fabricator, reaction chamber, small and framed ore drill. |
| CraftTweaker (machine recipes, gun/armor stats, death effects, ore clusters) | Machine recipes are JSON recipe types and can be edited with any datapack tool. Gun, armor, death-effect and ore-cluster scripting is not ported, because it would need a CraftTweaker dependency. |
| Chisel (block groups) | Not ported. |
| Thermal Expansion (block registrations) | Not ported. |
| Albedo (dynamic lights) | Not ported, no 1.21 version. |

---

## 7. Client and rendering (internal)
- **The 1.12 Java gun and armor models are kept.** They render through a small `GlStateManager` stand-in
  (`client/render/gl`) that sends the old matrix, colour and texture calls to 1.21's `PoseStack` and render buffers.
  Guns render through a block-entity-without-level renderer (BEWLR) item extension.
- **OBJ guns** use a small OBJ mesh renderer instead of 1.12 baked models.
- **The particle system** (`.txt` FX definitions, death effects, gore, muzzle FX, item-attached FX) is ported. It
  draws through 1.21 render types and supports depth sorting (`ParticleDepthSortPasses` client config).
- **Textures updated for 1.14+ vanilla art.** The ores now use vanilla 1.21 stone and spot shapes in Techguns'
  colours (iron's shape for tin, gold's for lead, diamond's for titanium, redstone's for uranium), at vanilla's 16x16.
  The ingots and nuggets use the 1.21 iron ingot and nugget shapes, and the crate sides sit on 1.21 planks.
  The ore cluster backgrounds are 1.21 stone (netherrack for the nether crystal cluster), with their shading, glow
  and animation kept. The
  `oretitanium` item is now "Raw Titanium" with a raw-ore icon (same id). The unused Techguns copper ingot and copper
  ore files were removed.
- **Keybinds** are NeoForge key mappings with the same defaults: N night vision, B safe mode, V step assist, R reload, J jetpack.

---

## 8. Testing (new)
1.12 had no automated tests. There are now 38 headless game tests, run with `./gradlew runGameTestServer`. They run
in `run-gametest/` and take about one minute. GitHub Actions (`.github/workflows/build.yml`) runs the build and every game test on each push and pull
request. It doesn't keep built jars, because the license doesn't allow distributing builds.

- `WorldgenTests`: every dungeon template loads; the castle, military base and bug nest build for 8 seeds each; every
  scanned structure places in all 4 rotations with matching door halves.
- `GameplayTests`:
  - permissions and safe mode;
  - weapon swap;
  - ammo change refund, and the rocket launcher switching to nukes, loading and reloading them;
  - the Jade overlay's server data (owner, security, progress);
  - waterloggable sandbags, camo nets and ladders;
  - the advancements: all load, lead to the root, use real item and entity ids and are translated;
  - vibrations: a sneaking player's gunshot, a reload and a Techguns explosion each set off a sculk sensor, and
    the gunfire event's tags and frequency;
  - the Mekanism radiation bridge (with Mekanism installed): shielding values of Techguns armor, RAD resistance of
    Mekanism's hazmat suit, Mekanism's own radiate() letting nothing through full Techguns hazmat, Rad-Away
    lowering the Mekanism dose, and rad pills blocking 75% of new exposure;
  - the compat recipes: each is gated on its mod and loads exactly when it should. CI also runs the tests with
    Create, Mekanism and IE installed, and with Create alone (`-Pcompat_mods=true` or `=create`), where a recipe
    that fails to parse, or takes an input another recipe of that type already uses, fails the test;
  - off-hand rules;
  - area mining selection;
  - the Shishkebap ambient effect;
  - sounds and packet codecs;
  - entity conditions;
  - tracking sync.
- `DataTests`: all machine recipe types have recipes with results, all loot tables load, and every item and block is
  translated.
- `OreTests`: the deepslate ores match the stone ores in tags, tool level, drops, light and worldgen targets, and
  smelt or react like them. Each ore's placed feature has its config check, and placement stops while it's off.
- `ModelTests`: every registered block's blockstate and every item's model, with all parents and textures they reach,
  exists and is in the block atlas (the missing-texture checkerboard never shows on a server, so this is the only
  headless check), and no
  blockstate file is left without a block.
- `TeamTests`: `/team` members, other teams and players without a team get the right security access and turret
  PvP decisions, and Techguns damage is blocked between teammates when friendly fire is off.
- `GuideTests`: every item, recipe, multiblock block, armor-stand entity, image and link in the manual resolves; the
  manual recipes make the Techguns book; every Ponder schematic forms with the real multiblock code.

Client-only code (sound playback, rendering) is not covered, because NeoForge 1.21.1 has no client game tests.

`PORT_CHANGELOG.md` has the detailed log of the last porting sessions, including which files changed.
