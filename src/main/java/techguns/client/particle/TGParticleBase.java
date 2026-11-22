package techguns.client.particle;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Minimal stand-in for the 1.12.2 vanilla Particle base: Techguns particles are managed and
 * rendered by {@link TGParticleManager}, never by the vanilla particle engine.
 */
public abstract class TGParticleBase {

	protected final Level world;
	protected final RandomSource rand = RandomSource.create();

	protected double posX, posY, posZ;
	protected double prevPosX, prevPosY, prevPosZ;
	protected double motionX, motionY, motionZ;

	protected boolean onGround = false;
	protected int particleAge = 0;
	protected int particleMaxAge;

	protected float particleRed = 1f, particleGreen = 1f, particleBlue = 1f, particleAlpha = 1f;
	protected float particleScale = 1f;

	private boolean isExpired = false;

	protected TGParticleBase(Level world, double x, double y, double z) {
		this.world = world;
		this.setPosition(x, y, z);
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
		this.particleMaxAge = (int) (4.0F / (this.rand.nextFloat() * 0.9F + 0.1F));
	}

	public void setPosition(double x, double y, double z) {
		this.posX = x;
		this.posY = y;
		this.posZ = z;
	}

	public void setExpired() {
		this.isExpired = true;
	}

	public boolean isAlive() {
		return !this.isExpired;
	}
}
