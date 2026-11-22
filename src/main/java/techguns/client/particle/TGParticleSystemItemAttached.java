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
public class TGParticleSystemItemAttached extends TGParticleSystem {

	protected InteractionHand hand;
	
	protected LivingEntity elb;
	protected Player ply;
	
	public TGParticleSystemItemAttached(Entity entity, InteractionHand hand, TGParticleSystemType type) {
		super(entity, type);
		this.hand=hand;
		if(this.entity instanceof Player) {
			this.ply = (Player) this.entity;
			this.elb=null;
		} else {
			this.elb= (LivingEntity) this.entity;
			this.ply=null;
		}
		this.itemAttached=true;
		float s = 1.0f; // + (Keybinds.X*10f);
		this.posX= type.offset.x; //* s + Keybinds.X;
		this.posY= type.offset.y; //* s + Keybinds.Y;
		this.posZ= type.offset.z; // * s + Keybinds.Z;
		this.scale=s;
	}

	@Override
	protected void addEffect(ITGParticle s) {
		List<ITGParticle> list = new ArrayList<>();
		s.setItemAttached();
		list.add(s);
		TGEntityParticles.get(this.entity).addEffectHand(hand, list);
	}
	
	
}
