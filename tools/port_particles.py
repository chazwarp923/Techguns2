"""Rename pass for the 1.12.2 particle code (techguns.client.particle).

Particles keep their own 1.12-style fields (posX, motionX...) through TGParticleBase, so only
accesses through Entity variables are rewritten. Usage:
    python -I tools/port_particles.py <src.java> <dst.java>
"""
import re
import sys

ENT = r'(?<![\w.])(entity|ent|elb|viewEnt|entityIn|viewEntity|this\.particleSystem\.entity|particleSystem\.entity|parentEntity|ply)'

RULES = [
    (r'\bVec3d\b', 'Vec3'),
    (r'\.rotatePitch\(', '.xRot('),
    (r'\.rotateYaw\(', '.yRot('),
    (r'\.crossProduct\(', '.cross('),
    (r'\.dotProduct\(', '.dot('),
    (r'\.squareDistanceTo\(', '.distanceToSqr('),
    (r'\bAxisAlignedBB\b', 'AABB'),
    (r'\bEntityLivingBase\b', 'LivingEntity'),
    (r'\bEntityPlayer\b', 'Player'),
    (r'\bEnumHand\b', 'InteractionHand'),
    (r'\bWorld\b', 'Level'),
    (r'\bMathHelper\b', 'Mth'),
    (r'new Random\(\)', 'TGParticleSystemType.RAND'),
    (r'\bRandom rand\b', 'RandomSource rand'),
    (ENT + r'\.posX\b', r'\1.getX()'), (ENT + r'\.posY\b', r'\1.getY()'), (ENT + r'\.posZ\b', r'\1.getZ()'),
    (ENT + r'\.prevPosX\b', r'\1.xo'), (ENT + r'\.prevPosY\b', r'\1.yo'), (ENT + r'\.prevPosZ\b', r'\1.zo'),
    (ENT + r'\.motionX\b', r'\1.getDeltaMovement().x'), (ENT + r'\.motionY\b', r'\1.getDeltaMovement().y'),
    (ENT + r'\.motionZ\b', r'\1.getDeltaMovement().z'),
    (ENT + r'\.rotationPitch\b', r'\1.getXRot()'), (ENT + r'\.rotationYawHead\b', r'\1.getYHeadRot()'),
    (ENT + r'\.rotationYaw\b', r'\1.getYRot()'),
    (ENT + r'\.prevRotationPitch\b', r'\1.xRotO'), (ENT + r'\.prevRotationYawHead\b', r'\1.yHeadRotO'),
    (ENT + r'\.isEntityAlive\(\)', r'\1.isAlive()'),
    (ENT + r'\.isDead\b', r'\1.isRemoved()'),
    (ENT + r'\.world\b', r'\1.level()'),
    (r'@SideOnly\(Side\.CLIENT\)\s*', ''),
]

IMPORTS = '''import java.util.ArrayList;
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
'''


def convert(s):
    s = re.sub(r'^import [^;]+;\s*\n', '', s, flags=re.M)
    for a, b in RULES:
        s = re.sub(a, b, s)
    s = re.sub(r'(package [^;]+;\s*\n)', lambda m: m.group(1) + '\n' + IMPORTS, s, count=1)
    return s


if __name__ == '__main__':
    src = open(sys.argv[1], encoding='utf-8').read()
    open(sys.argv[2], 'w', encoding='utf-8').write(convert(src))
    print('ported', sys.argv[1])
