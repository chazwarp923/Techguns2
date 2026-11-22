"""Mechanical first pass for porting a 1.12.2 source file to 1.21.1 (Mojang mappings + NeoForge).

Usage: python tools/port_names.py legacy/java/techguns/Foo.java src/main/java/techguns/Foo.java

It only renames well-known classes/methods/fields and rewrites imports; the result still needs
manual work (logic changes, registry, rendering...). Unknown imports are left in place so the
compiler points at them.
"""
import re
import sys

# simple class renames: old simple name -> (new simple name, new import)
CLASSES = {
    'EntityPlayer': ('Player', 'net.minecraft.world.entity.player.Player'),
    'EntityPlayerMP': ('ServerPlayer', 'net.minecraft.server.level.ServerPlayer'),
    'EntityPlayerSP': ('LocalPlayer', 'net.minecraft.client.player.LocalPlayer'),
    'EntityLivingBase': ('LivingEntity', 'net.minecraft.world.entity.LivingEntity'),
    'EntityLiving': ('Mob', 'net.minecraft.world.entity.Mob'),
    'EntityMob': ('Monster', 'net.minecraft.world.entity.monster.Monster'),
    'EntityCreature': ('PathfinderMob', 'net.minecraft.world.entity.PathfinderMob'),
    'EntityItem': ('ItemEntity', 'net.minecraft.world.entity.item.ItemEntity'),
    'Entity': ('Entity', 'net.minecraft.world.entity.Entity'),
    'World': ('Level', 'net.minecraft.world.level.Level'),
    'WorldServer': ('ServerLevel', 'net.minecraft.server.level.ServerLevel'),
    'IBlockAccess': ('BlockGetter', 'net.minecraft.world.level.BlockGetter'),
    'IBlockState': ('BlockState', 'net.minecraft.world.level.block.state.BlockState'),
    'Block': ('Block', 'net.minecraft.world.level.block.Block'),
    'Blocks': ('Blocks', 'net.minecraft.world.level.block.Blocks'),
    'BlockPos': ('BlockPos', 'net.minecraft.core.BlockPos'),
    'EnumFacing': ('Direction', 'net.minecraft.core.Direction'),
    'Vec3d': ('Vec3', 'net.minecraft.world.phys.Vec3'),
    'Vec3i': ('Vec3i', 'net.minecraft.core.Vec3i'),
    'Vec2f': ('Vec2', 'net.minecraft.world.phys.Vec2'),
    'AxisAlignedBB': ('AABB', 'net.minecraft.world.phys.AABB'),
    'MathHelper': ('Mth', 'net.minecraft.util.Mth'),
    'RayTraceResult': ('HitResult', 'net.minecraft.world.phys.HitResult'),
    'ItemStack': ('ItemStack', 'net.minecraft.world.item.ItemStack'),
    'Item': ('Item', 'net.minecraft.world.item.Item'),
    'Items': ('Items', 'net.minecraft.world.item.Items'),
    'NBTTagCompound': ('CompoundTag', 'net.minecraft.nbt.CompoundTag'),
    'NBTTagList': ('ListTag', 'net.minecraft.nbt.ListTag'),
    'NonNullList': ('NonNullList', 'net.minecraft.core.NonNullList'),
    'EnumHand': ('InteractionHand', 'net.minecraft.world.InteractionHand'),
    'EnumHandSide': ('HumanoidArm', 'net.minecraft.world.entity.HumanoidArm'),
    'EnumActionResult': ('InteractionResult', 'net.minecraft.world.InteractionResult'),
    'ActionResult': ('InteractionResultHolder', 'net.minecraft.world.InteractionResultHolder'),
    'SoundEvent': ('SoundEvent', 'net.minecraft.sounds.SoundEvent'),
    'SoundCategory': ('SoundSource', 'net.minecraft.sounds.SoundSource'),
    'SoundEvents': ('SoundEvents', 'net.minecraft.sounds.SoundEvents'),
    'SoundType': ('SoundType', 'net.minecraft.world.level.block.SoundType'),
    'ResourceLocation': ('ResourceLocation', 'net.minecraft.resources.ResourceLocation'),
    'DamageSource': ('DamageSource', 'net.minecraft.world.damagesource.DamageSource'),
    'MobEffects': ('MobEffects', 'net.minecraft.world.effect.MobEffects'),
    'PotionEffect': ('MobEffectInstance', 'net.minecraft.world.effect.MobEffectInstance'),
    'Potion': ('MobEffect', 'net.minecraft.world.effect.MobEffect'),
    'EntityEquipmentSlot': ('EquipmentSlot', 'net.minecraft.world.entity.EquipmentSlot'),
    'ITextComponent': ('Component', 'net.minecraft.network.chat.Component'),
    'TextFormatting': ('ChatFormatting', 'net.minecraft.ChatFormatting'),
    'ChatFormatting': ('ChatFormatting', 'net.minecraft.ChatFormatting'),
    'EnumParticleTypes': ('ParticleTypes', 'net.minecraft.core.particles.ParticleTypes'),
    'TileEntity': ('BlockEntity', 'net.minecraft.world.level.block.entity.BlockEntity'),
    'IInventory': ('Container', 'net.minecraft.world.Container'),
    'Container': ('AbstractContainerMenu', 'net.minecraft.world.inventory.AbstractContainerMenu'),
    'Slot': ('Slot', 'net.minecraft.world.inventory.Slot'),
    'InventoryPlayer': ('Inventory', 'net.minecraft.world.entity.player.Inventory'),
    'Minecraft': ('Minecraft', 'net.minecraft.client.Minecraft'),
    'GuiScreen': ('Screen', 'net.minecraft.client.gui.screens.Screen'),
    'GuiContainer': ('AbstractContainerScreen', 'net.minecraft.client.gui.screens.inventory.AbstractContainerScreen'),
    'Random': ('RandomSource', 'net.minecraft.util.RandomSource'),
    'SharedMonsterAttributes': ('Attributes', 'net.minecraft.world.entity.ai.attributes.Attributes'),
    'EntityDataManager': ('SynchedEntityData', 'net.minecraft.network.syncher.SynchedEntityData'),
    'DataParameter': ('EntityDataAccessor', 'net.minecraft.network.syncher.EntityDataAccessor'),
    'DataSerializers': ('EntityDataSerializers', 'net.minecraft.network.syncher.EntityDataSerializers'),
    'EnumDifficulty': ('Difficulty', 'net.minecraft.world.Difficulty'),
    'Material': ('Material', None),
}

# method / field renames (regex, replacement). Applied in order.
MEMBERS = [
    (r'\.world\b', '.level()'),
    (r'\bworld\.isRemote\b', 'level.isClientSide'),
    (r'\.isRemote\b', '.isClientSide'),
    (r'\.posX\b', '.getX()'), (r'\.posY\b', '.getY()'), (r'\.posZ\b', '.getZ()'),
    (r'\.motionX\b', '.getDeltaMovement().x'), (r'\.motionY\b', '.getDeltaMovement().y'), (r'\.motionZ\b', '.getDeltaMovement().z'),
    (r'\.rotationYaw\b', '.getYRot()'), (r'\.rotationPitch\b', '.getXRot()'),
    (r'\.rotationYawHead\b', '.getYHeadRot()'),
    (r'\.ticksExisted\b', '.tickCount'),
    (r'\.onGround\b', '.onGround()'),
    (r'\.getHeldItemMainhand\(\)', '.getMainHandItem()'),
    (r'\.getHeldItemOffhand\(\)', '.getOffhandItem()'),
    (r'\.getHeldItem\(', '.getItemInHand('),
    (r'\.setHeldItem\(', '.setItemInHand('),
    (r'\.getItemStackFromSlot\(', '.getItemBySlot('),
    (r'\.isSneaking\(\)', '.isShiftKeyDown()'),
    (r'\.getEyeHeight\(\)', '.getEyeHeight()'),
    (r'\.getLookVec\(\)', '.getLookAngle()'),
    (r'\.getDistanceSq\(', '.distanceToSqr('),
    (r'\.getDistance\(', '.distanceTo('),
    (r'\.attackEntityFrom\(', '.hurt('),
    (r'\.setDead\(\)', '.discard()'),
    (r'\.isDead\b', '.isRemoved()'),
    (r'\.spawnEntity\(', '.addFreshEntity('),
    (r'\.getBlockState\(', '.getBlockState('),
    (r'\.setBlockState\(', '.setBlockAndUpdate('),
    (r'\.setBlockToAir\(', '.removeBlock('),
    (r'\.isAirBlock\(', '.isEmptyBlock('),
    (r'\.getDefaultState\(\)', '.defaultBlockState()'),
    (r'\.withProperty\(', '.setValue('),
    (r'\.getValue\(', '.getValue('),
    (r'\.offset\(', '.relative('),
    (r'\.up\(\)', '.above()'), (r'\.down\(\)', '.below()'),
    (r'\.up\(', '.above('), (r'\.down\(', '.below('),
    (r'\.getTagCompound\(\)', '.getTag()'),
    (r'\.setTagCompound\(', '.setTag('),
    (r'\.getInteger\(', '.getInt('), (r'\.setInteger\(', '.putInt('),
    (r'\.setFloat\(', '.putFloat('), (r'\.setShort\(', '.putShort('), (r'\.setByte\(', '.putByte('),
    (r'\.setBoolean\(', '.putBoolean('), (r'\.setString\(', '.putString('), (r'\.setDouble\(', '.putDouble('),
    (r'\.setLong\(', '.putLong('), (r'\.setTag\(', '.put('), (r'\.hasKey\(', '.contains('),
    (r'\.getCompoundTag\(', '.getCompound('), (r'\.getTagList\(', '.getList('),
    (r'\.isCreativeMode\b', '.instabuild'),
    (r'\.capabilities\.', '.getAbilities().'),
    (r'\.inventory\.', '.getInventory().'),
    (r'\.rand\b', '.random'),
    (r'\.getRNG\(\)', '.getRandom()'),
    (r'\.playSound\(\(EntityPlayer\) ?null', '.playSound(null'),
    (r'MathHelper\.sqrt\(', 'Mth.sqrt((float)'),
    (r'\bMathHelper\.', 'Mth.'),
    (r'\.getTotalWorldTime\(\)', '.getGameTime()'),
    (r'\.getWorldTime\(\)', '.getDayTime()'),
    (r'\.getItemDamage\(\)', '.getDamageValue()'),
    (r'\.isPotionActive\(', '.hasEffect('),
    (r'\.addPotionEffect\(', '.addEffect('),
    (r'\.getActivePotionEffect\(', '.getEffect('),
    (r'\.setFire\(', '.igniteForSeconds('),
    (r'\.isBurning\(\)', '.isOnFire()'),
    (r'\.extinguish\(\)', '.clearFire()'),
    (r'\.isInWater\(\)', '.isInWater()'),
    (r'\.isWet\(\)', '.isInWaterRainOrBubble()'),
    (r'\.getEntityBoundingBox\(\)', '.getBoundingBox()'),
    (r'\.grow\(', '.inflate('),
    (r'\.getEntitiesWithinAABB\(', '.getEntitiesOfClass('),
    (r'\.getEntitiesWithinAABBExcludingEntity\(', '.getEntities('),
    (r'\.getEntitiesInAABBexcluding\(', '.getEntities('),
    (r'\.squareDistanceTo\(', '.distanceToSqr('),
    (r'\.hitVec\b', '.getLocation()'),
    (r'\.typeOfHit\b', '.getType()'),
    (r'\.entityHit\b', '.getEntity()'),
    (r'\.sideHit\b', '.getDirection()'),
    (r'\.getHorizontalAngle\(\)', '.toYRot()'),
    (r'\.getHorizontalIndex\(\)', '.get2DDataValue()'),
    (r'\.getOpposite\(\)', '.getOpposite()'),
    (r'\.getUnlocalizedName\(\)', '.getDescriptionId()'),
    (r'\.getMaxStackSize\(\)', '.getMaxStackSize()'),
    (r'\.getCount\(\)', '.getCount()'),
    (r'\.isEmpty\(\)', '.isEmpty()'),
    (r'\.isRiding\(\)', '.isPassenger()'),
    (r'\.isOnLadder\(\)', '.onClimbable()'),
    (r'\.isActiveItemStackBlocking\(\)', '.isBlocking()'),
    (r'\.getPrimaryHand\(\)', '.getMainArm()'),
    (r'\.getActiveItemStack\(\)', '.getUseItem()'),
    (r'\.getMaxHealth\(\)', '.getMaxHealth()'),
    (r'\.width\b', '.getBbWidth()'), (r'\.height\b', '.getBbHeight()'),
    (r'\.getEntityId\(\)', '.getId()'),
    (r'\.getEntityByID\(', '.getEntity('),
    (r'\.getEntityWorld\(\)', '.level()'),
    (r'\.dimension\b', '.level().dimension()'),
    (r'EnumHand\.MAIN_HAND', 'InteractionHand.MAIN_HAND'), (r'EnumHand\.OFF_HAND', 'InteractionHand.OFF_HAND'),
    (r'EnumHandSide\.RIGHT', 'HumanoidArm.RIGHT'), (r'EnumHandSide\.LEFT', 'HumanoidArm.LEFT'),
    (r'EnumActionResult\.SUCCESS', 'InteractionResult.SUCCESS'), (r'EnumActionResult\.PASS', 'InteractionResult.PASS'),
    (r'EnumActionResult\.FAIL', 'InteractionResult.FAIL'),
    (r'EnumFacing\.', 'Direction.'),
    (r'MobEffects\.BLINDNESS', 'MobEffects.BLINDNESS'),
    (r'MobEffects\.SLOWNESS', 'MobEffects.MOVEMENT_SLOWDOWN'),
    (r'MobEffects\.SPEED', 'MobEffects.MOVEMENT_SPEED'),
    (r'MobEffects\.STRENGTH', 'MobEffects.DAMAGE_BOOST'),
    (r'MobEffects\.INSTANT_HEALTH', 'MobEffects.HEAL'),
    (r'MobEffects\.INSTANT_DAMAGE', 'MobEffects.HARM'),
    (r'MobEffects\.JUMP_BOOST', 'MobEffects.JUMP'),
    (r'MobEffects\.NAUSEA', 'MobEffects.CONFUSION'),
    (r'MobEffects\.RESISTANCE', 'MobEffects.DAMAGE_RESISTANCE'),
    (r'MobEffects\.MINING_FATIGUE', 'MobEffects.DIG_SLOWDOWN'),
    (r'MobEffects\.HASTE', 'MobEffects.DIG_SPEED'),
    (r'EnumParticleTypes\.', 'ParticleTypes.'),
    (r'SoundCategory\.', 'SoundSource.'),
    (r'@SideOnly\(Side\.CLIENT\)\s*', ''),
    (r'@SideOnly\(Side\.SERVER\)\s*', ''),
]

STRIP_IMPORT_PREFIXES = ('net.minecraftforge.fml.relauncher', 'net.minecraft.', 'net.minecraftforge.', 'com.mojang.realmsclient',
                         'org.lwjgl.input', 'org.lwjgl.opengl')


def port(src: str) -> str:
    lines = src.split('\n')
    imports = []
    body = []
    for line in lines:
        m = re.match(r'\s*import\s+(static\s+)?([\w.]+)(\.\*)?;', line)
        if m:
            imports.append(line)
        else:
            body.append(line)
    text = '\n'.join(body)

    used_new_imports = set()
    kept_imports = []
    for imp in imports:
        m = re.match(r'\s*import\s+(static\s+)?([\w.]+)(\.\*)?;', imp)
        full = m.group(2)
        simple = full.split('.')[-1]
        if full == 'java.util.Random':
            used_new_imports.add('net.minecraft.util.RandomSource')
        elif full.startswith(STRIP_IMPORT_PREFIXES):
            if simple in CLASSES and CLASSES[simple][1]:
                used_new_imports.add(CLASSES[simple][1])
            else:
                kept_imports.append('// TODO port: ' + imp.strip())
        else:
            kept_imports.append(imp)

    for old, (new, _) in CLASSES.items():
        if old != new:
            text = re.sub(r'\b' + old + r'\b', new, text)
    for pat, rep in MEMBERS:
        text = re.sub(pat, rep, text)

    header, rest = text.split('\n', 1) if text.startswith('package') else ('', text)
    imp_block = '\n'.join(sorted('import ' + i + ';' for i in used_new_imports))
    out = header + '\n\n' + imp_block + '\n' + '\n'.join(kept_imports) + '\n' + rest
    return out


if __name__ == '__main__':
    src = open(sys.argv[1], encoding='utf-8').read()
    out = port(src)
    with open(sys.argv[2], 'w', encoding='utf-8', newline='\n') as f:
        f.write(out)
