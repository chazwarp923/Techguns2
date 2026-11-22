package techguns.client;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.client.particle.TGFX;
import techguns.client.particle.TGParticleManager;
import techguns.client.particle.TGParticleSystem;
import techguns.util.EntityCondition;
import techguns.util.TGFXHooks;

/** Client implementation of the FX hooks (1.12.2 ClientProxy.createFX*). */
public class ClientFX implements TGFXHooks.Handler {

	public static final ClientFX INSTANCE = new ClientFX();

	private static void add(List<TGParticleSystem> systems, java.util.function.Consumer<TGParticleSystem> setup) {
		if (systems != null) {
			for (TGParticleSystem s : systems) {
				setup.accept(s);
				TGParticleManager.INSTANCE.addEffect(s);
			}
		}
	}

	@Override
	public void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz) {
		add(TGFX.createFX(world, name, x, y, z, mx, my, mz), s -> {
		});
	}

	@Override
	public void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float pitch, float yaw) {
		add(TGFX.createFX(world, name, x, y, z, mx, my, mz), s -> {
			s.rotationPitch = pitch;
			s.rotationYaw = yaw;
		});
	}

	@Override
	public void createFX(String name, Level world, double x, double y, double z, double mx, double my, double mz, float scale) {
		add(TGFX.createFX(world, name, x, y, z, mx, my, mz), s -> s.scale = scale);
	}

	@Override
	public void createFXOnEntity(String name, Entity ent) {
		add(TGFX.createFXOnEntity(ent, name), s -> s.condition = EntityCondition.ENTITY_ALIVE);
	}

	@Override
	public void createFXOnEntity(String name, Entity ent, float scale) {
		add(TGFX.createFXOnEntity(ent, name), s -> {
			s.scale = scale;
			s.condition = EntityCondition.ENTITY_ALIVE;
		});
	}

	@Override
	public void createFXOnEntityWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead,
			EntityCondition condition) {
		add(TGFX.createFXOnEntity(ent, name), s -> {
			s.entityOffset = new Vec3(offsetX, offsetY, offsetZ);
			s.attachToHead = attachToHead;
			s.condition = condition;
		});
	}

	@Override
	public boolean isClientPlayerAndIn1stPerson(Entity ent) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		return ent == mc.player && mc.options.getCameraType().isFirstPerson();
	}
}
