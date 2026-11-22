package techguns.client.particle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.Techguns;
import techguns.client.particle.TGParticleSystemType.AlphaEntry;
import techguns.client.particle.TGParticleSystemType.ColorEntry;
import techguns.client.render.TGRenderHelper.RenderType;

/**
 * Handles the loading and storage of all ParticleSystem types. Loaded from
 * assets/techguns/particles/*.txt on every client resource reload.
 */
public class TGFX {

	public static final String FXLIST_DIR = "particles/";
	public static String[] FXFILES = { "fxlist.txt", "nuke.txt", "deathfx.txt", "impactfx.txt", "testfx.txt" };

	public static HashMap<String, TGFXType> FXList = new HashMap<String, TGFXType>();

	public static final ResourceManagerReloadListener RELOAD_LISTENER = TGFX::loadFXList;

	private static TGFXType get(String fx) {
		TGFXType t = FXList.get(fx.toLowerCase());
		if (t == null) {
			Techguns.LOGGER.warn("FX '{}' not found!", fx);
		}
		return t;
	}

	public static List<TGParticleSystem> createFX(Level world, String fx, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		TGFXType t = get(fx);
		return t == null ? null : t.createParticleSystems(world, posX, posY, posZ, motionX, motionY, motionZ);
	}

	public static List<TGParticleSystem> createFXOnEntity(Entity ent, String fx) {
		TGFXType t = get(fx);
		return t == null ? null : t.createParticleSystemsOnEntity(ent);
	}

	public static List<TGParticleSystem> createFXOnParticle(Level worldIn, TGParticle ent, String fx) {
		TGFXType t = get(fx);
		return t == null ? null : t.createParticleSystemsOnParticle(worldIn, ent);
	}

	public static List<TGParticleSystem> createFXOnEntityItemAttached(Entity ent, InteractionHand hand, String fx) {
		TGFXType t = get(fx);
		return t == null ? null : t.createParticleSystemsOnEntityItemAttached(ent, hand);
	}

	public static void loadFXList(ResourceManager manager) {
		FXList.clear();
		for (String filename : FXFILES) {
			ResourceLocation loc = Techguns.rl(FXLIST_DIR + filename);
			Optional<Resource> res = manager.getResource(loc);
			if (res.isEmpty()) {
				Techguns.LOGGER.error("Missing particle definition file {}", loc);
				continue;
			}
			try (InputStream in = res.get().open()) {
				loadFXListFile(in, filename);
			} catch (IOException | RuntimeException e) {
				Techguns.LOGGER.error("Failed reading particle file " + loc, e);
			}
		}
		Techguns.LOGGER.info("Loaded {} Techguns FX definitions", FXList.size());
	}

	public static boolean loadFXListFile(InputStream in, String filename) {
		Scanner sc = new Scanner(in, StandardCharsets.UTF_8);
		sc.useLocale(Locale.ENGLISH);
		sc.useDelimiter("(\\s*=\\s*)|\\s\\{|\\s*//.*|\\r\\n|\\s+"); //|^\\s*$		
		String error = "";
		String next = "";
		String value = "";
		int count = 0;
		try {
		while (sc.hasNextLine() && error.equals("")) {
			if (sc.hasNext()) next = sc.next();
			else break;
			//FXList
			if (next.toLowerCase().equals("fxlist") && sc.hasNext()) {
				String name = sc.next();
				TGParticleListType type = new TGParticleListType();
				boolean end = false;
				while (sc.hasNextLine() && !end && error.equals("")) {
					if (sc.hasNext() && !(next = sc.next()).equals("")) {
						if (next.equals("}")) {
							end = true;
						}else {
							type.addParticleSystem(next.toLowerCase());
						}
					}
				}
				count++;
				FXList.put(name.toLowerCase(), type);
			} else if (!next.equals("")) {
				String name = next;
				TGParticleSystemType type = new TGParticleSystemType();
				boolean end = false;
				while (sc.hasNextLine() && !end && error.equals("")) {
					if (sc.hasNext() && !(next = sc.next()).equals("")) {
						String key = next;
						switch(key.toLowerCase()) {
							case "extends":
								value = sc.next();
								if (FXList.containsKey(value.toLowerCase())) {
									TGFXType fxtype = FXList.get(value.toLowerCase());
									if (fxtype instanceof TGParticleSystemType) {
										type.extend((TGParticleSystemType)fxtype);
									}else {
										error = "'EXTENDS' must specifiy a ParticleSystem, not an FXList.";
									}
								}else {
									error = "ParticleSystem '"+value.toLowerCase()+"' not found. Can't extend.";
								}
								break;
							case "rendertype":
								value = sc.next();
								switch (value.toUpperCase().trim()) {
								case "ADDITIVE":
									type.renderType = RenderType.ADDITIVE;
									break;
								case "ALPHA":
									type.renderType = RenderType.ALPHA;
									break;
								case "ALPHA_SHADED":
									type.renderType = RenderType.ALPHA_SHADED;
									break;
								case "SOLID":
									type.renderType = RenderType.SOLID;
									break;
								case "NO_Z_TEST":
									type.renderType = RenderType.NO_Z_TEST;
									break;
								default:
									error = name;
									break;
								}
								break;
							case "texture":
								value = sc.next();
								// 1.21 resource paths must be lowercase; never let one bad entry abort the resource reload
								ResourceLocation tex = ResourceLocation.tryParse(value.toLowerCase(java.util.Locale.ROOT));
								if (tex != null) {
									type.texture = tex;
								} else {
									error = value;
								}
								break;
							case "rows":
								type.rows = sc.nextInt();
								break;
							case "columns":
								type.columns = sc.nextInt();
								break;
							case "frames":
								type.frames = sc.nextInt();
								break;
							case "hasvariations":
								type.hasVariations = sc.nextBoolean();
								break;
//							case "randomrotation":
//								type.randomRotation = sc.nextBoolean();
//								break;
							case "angle":
								type.angleMin = sc.nextFloat();
								type.angleMax = sc.nextFloat();
								break;
							case "anglerate":
								type.angleRateMin = sc.nextFloat();
								type.angleRateMax = sc.nextFloat();
								break;
							case "angleratedamping":
								type.angleRateDampingMin = sc.nextFloat();
								type.angleRateDampingMax = sc.nextFloat();
								break;
							case "lifetime":
								type.lifetimeMin = sc.nextInt();
								type.lifetimeMax = sc.nextInt();
								break;
							case "systemlifetime":
								type.systemLifetimeMin = sc.nextInt();
								type.systemLifetimeMax = sc.nextInt();
								break;
							case "animationspeed":
								type.animationSpeedMin = sc.nextFloat();
								type.animationSpeedMax = sc.nextFloat();
								break;
							case "size":
								type.sizeMin = sc.nextFloat();
								type.sizeMax = sc.nextFloat();
								break;
							case "sizerate":
								type.sizeRateMin = sc.nextFloat();
								type.sizeRateMax = sc.nextFloat();
								break;
							case "sizeratedamping":
								type.sizeRateDampingMin = sc.nextFloat();
								type.sizeRateDampingMax = sc.nextFloat();
								break;
							case "startsizerate":
								type.startSizeRateMin = sc.nextFloat();
								type.startSizeRateMax = sc.nextFloat();
								break;
							case "startsizeratedamping":
								type.startSizeRateDampingMin = sc.nextFloat();
								type.startSizeRateDampingMax = sc.nextFloat();
								break;
							case "alpha":
								type.alphaEntries.add(new AlphaEntry(sc.nextFloat(), sc.nextFloat()));
								break;
							case "color":
								type.colorEntries.add(new ColorEntry(sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextFloat()));
								break;
							case "particlecount":
								type.particleCountMin = sc.nextInt();
								type.particleCountMax = sc.nextInt();
								break;
							case "initialdelay":
								type.initialDelayMin = sc.nextInt();
								type.initialDelayMax = sc.nextInt();
								break;
							case "spawndelay":
								type.spawnDelayMin = sc.nextInt();
								type.spawnDelayMax = sc.nextInt();
								break;
							case "velocitytype":
								value = sc.next();
								switch (value.toUpperCase()) {
								case "NONE":
									type.velocityType = TGParticleSystemType.VEL_NONE;
									break;
								case "SPHERICAL":
									type.velocityType = TGParticleSystemType.VEL_SPHERICAL;
									break;
								case "ORTHO":
									type.velocityType = TGParticleSystemType.VEL_ORTHO;
									break;
								case "HEMISPHERICAL":
									type.velocityType = TGParticleSystemType.VEL_HEMISPHERICAL;
									break;
								case "OUTWARD":
									type.velocityType = TGParticleSystemType.VEL_OUTWARD;
									break;
								default:
									error = name;
									break;
								}
								break;
							case "velocity1":
								type.velocityDataMin[0] = sc.nextFloat();
								type.velocityDataMax[0] = sc.nextFloat();
								break;
							case "velocity2":
								type.velocityDataMin[1] = sc.nextFloat();
								type.velocityDataMax[1] = sc.nextFloat();
								break;
							case "velocity3":
								type.velocityDataMin[2] = sc.nextFloat();
								type.velocityDataMax[2] = sc.nextFloat();
								break;
							case "velocitydamping":
								type.velocityDampingMin = sc.nextFloat();
								type.velocityDampingMax = sc.nextFloat();
								break;
							case "volumetype":
								value = sc.next();
								switch (value.toUpperCase()) {
								case "SPHERE":
									type.volumeType = TGParticleSystemType.VOL_SPHERE;
									break;
								case "CYLINDER":
									type.volumeType = TGParticleSystemType.VOL_CYLINDER;
									break;
								case "CYLINDER2":
									type.volumeType = TGParticleSystemType.VOL_CYLINDER2;
									break;
								case "POINT":
									type.volumeType = TGParticleSystemType.VOL_POINT;
									break;
								case "HEMISPHERE":
									type.volumeType = TGParticleSystemType.VOL_HEMISPHERE;
									break;
								case "TRAIL":
									type.volumeType = TGParticleSystemType.VOL_TRAIL;
									break;
								default:
									error = name;
									break;
								}
								break;
							case "volume1":
								type.volumeData[0] = sc.nextFloat();
								break;
							case "volume2":
								type.volumeData[1] = sc.nextFloat();
								break;
							case "volume3":
								type.volumeData[2] = sc.nextFloat();
								break;
							case "gravity":
								type.gravity = sc.nextFloat();
								break;
							case "ishollow":
								type.isHollow = sc.nextBoolean();
								break;
							case "removeonground":
								type.removeOnGround = sc.nextBoolean();
								break;
							case "systemvelocityfactor":
								type.systemVelocityFactorMin = sc.nextFloat();
								type.systemVelocityFactorMax = sc.nextFloat();
								break;
							case "velocitydampingonground":
								type.velocityDampingOnGroundMin = sc.nextFloat();
								type.velocityDampingOnGroundMax = sc.nextFloat();
								break;
							case "offset":
								float x = sc.nextFloat();
								float y = sc.nextFloat();
								float z = sc.nextFloat();
								type.offset = new Vec3(x, y, z);
								break;
							case "particlesmovewithsystem":
								type.particlesMoveWithSystem = sc.nextBoolean();
								break;
							case "particlessticktosystem":
								type.particlesStickToSystem = sc.nextBoolean();
								break;
							case "groundaligned":
								type.groundAligned = sc.nextBoolean();
								break;
							case "streak":
								type.streak = sc.nextBoolean();
								break;
							case "attachedsystem":
								type.attachedSystem = sc.next();
								break;
							case "}":
								end = true;
								//System.out.println("Successfully parsed ParticleSystem "+name);
								break;
						}
						
					}
				}
				if (error.equals("")) {
					count++;
					FXList.put(name.toLowerCase(), type);
					//System.out.println("Parsed ParticleSystem" +name+" in file '"+filename+"'.");
				}else{
					Techguns.LOGGER.error("Error(s) while parsing particle "+name+" in file '"+filename+"'.");
					Techguns.LOGGER.error(error);
					sc.close();
					return false;
				}
			}
			
		}
		}catch (InputMismatchException e) {
			e.printStackTrace();
			sc.close();
			return false;
		}

		sc.close();
		//System.out.println("Successfully parsed file "+ filename+ ". " +count + " ParticleSystems loaded.");
		return true;

	}
	
	
}
