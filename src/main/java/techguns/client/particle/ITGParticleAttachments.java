package techguns.client.particle;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import techguns.client.render.TGRenderHelper;
import techguns.client.render.TGRenderHelper.RenderType;
import techguns.util.EntityCondition;
import techguns.util.MathUtil;
/**
 * Implemented by capabilities that store entityAttachedParticle systems
 *
 */
public interface ITGParticleAttachments {
	
	public default void tickParticles() {
		if(this.getEntityParticles()!=null) {
			Iterator<ITGParticle> it = getEntityParticles().iterator();
			while(it.hasNext()) {
				ITGParticle p = it.next();
				
				p.updateTick();
				if(p.shouldRemove()) {
					it.remove();
				}
			}
		}
		
		if(this.getEntityParticlesMH()!=null) {
			Iterator<ITGParticle> it = getEntityParticlesMH().iterator();
			while(it.hasNext()) {
				ITGParticle p = it.next();
				
				p.updateTick();
				if(p.shouldRemove()) {
					it.remove();
				}
			}
		}
		
		if(this.getEntityParticlesOH()!=null) {
			Iterator<ITGParticle> it = getEntityParticlesOH().iterator();
			while(it.hasNext()) {
				ITGParticle p = it.next();
				
				p.updateTick();
				if(p.shouldRemove()) {
					it.remove();
				}
			}
		}
		
		if(this.getParticleSysMainhand()!=null) {
			Iterator<TGParticleSystem> it = getParticleSysMainhand().iterator();
			while(it.hasNext()) {
				TGParticleSystem p = it.next();
				
				p.updateTick();
				if(p.shouldRemove()) {
					it.remove();
				}
			}
		}
		if(this.getParticleSysOffhand()!=null) {
			Iterator<TGParticleSystem> it = getParticleSysOffhand().iterator();
			while(it.hasNext()) {
				TGParticleSystem p = it.next();
				
				p.updateTick();
				if(p.shouldRemove()) {
					it.remove();
				}
			}
		}
	}
	
	public default void clearAttachedSystemsHand(InteractionHand hand) {
		if(hand==InteractionHand.MAIN_HAND) {
			if (this.getParticleSysMainhand()!=null) {
				this.getParticleSysMainhand().clear();
			}
		} else {
			if (this.getParticleSysOffhand()!=null) {
				this.getParticleSysOffhand().clear();
			}
		}
	}
	
	public default void addSystemsHand(InteractionHand hand, List<TGParticleSystem> systems) {
		
		if(hand==InteractionHand.MAIN_HAND) {
			this.getOrInitParticleSysMainhand().clear();
			this.getParticleSysMainhand().addAll(systems);
		} else {
			this.getOrInitParticleSysOffhand().clear();
			this.getParticleSysOffhand().addAll(systems);
		}
	}
	
	public default void addEffectHand(InteractionHand hand, List<ITGParticle> effects) {
		
		if(hand==InteractionHand.MAIN_HAND) {
			this.getOrInitEntityParticlesMH().addAll(effects);
		} else {
			this.getOrInitEntityParticlesOH().addAll(effects);
		}
	}
	
	public List<ITGParticle> getEntityParticles();
	
	public List<ITGParticle> getEntityParticlesMH();
	
	public List<ITGParticle> getEntityParticlesOH();
	
	public List<TGParticleSystem> getParticleSysMainhand();
	
	public List<TGParticleSystem> getParticleSysOffhand();
	
	public List<ITGParticle> getOrInitEntityParticles();

	public List<ITGParticle> getOrInitEntityParticlesOH();

	public List<ITGParticle> getOrInitEntityParticlesMH();
	
	public List<TGParticleSystem> getOrInitParticleSysMainhand();
	
	public List<TGParticleSystem> getOrInitParticleSysOffhand();

}
