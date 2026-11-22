package techguns.tileentities.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A running machine job (1.12 MachineOperation): the consumed inputs (per multiplier step), the outputs and the
 * stack multiplier from machine upgrades.
 */
public class MachineOperation {

	protected List<ItemStack> inputs = new ArrayList<>();
	protected List<ItemStack> outputs = new ArrayList<>();
	protected List<FluidStack> fluidInputs = new ArrayList<>();
	protected List<FluidStack> fluidOutputs = new ArrayList<>();
	protected int stackMultiplier = 1;
	protected int powerPerTick = 0;

	public MachineOperation() {
	}

	public MachineOperation(ItemStack output, ItemStack... inputs) {
		this.outputs.add(output);
		this.inputs.addAll(Arrays.asList(inputs));
	}

	public MachineOperation(List<ItemStack> outputs, List<ItemStack> inputs) {
		this.outputs.addAll(outputs);
		this.inputs.addAll(inputs);
	}

	public MachineOperation setStackMultiplier(int mult) {
		this.stackMultiplier = mult;
		return this;
	}

	public int getStackMultiplier() {
		return stackMultiplier;
	}

	public int getPowerPerTick() {
		return powerPerTick * this.stackMultiplier;
	}

	public MachineOperation setPowerPerTick(int powerPerTick) {
		this.powerPerTick = powerPerTick;
		return this;
	}

	public List<ItemStack> getInputs() {
		return inputs;
	}

	public List<ItemStack> getOutputs() {
		return outputs;
	}

	public List<FluidStack> getFluidInputs() {
		return fluidInputs;
	}

	public List<FluidStack> getFluidOutputs() {
		return fluidOutputs;
	}

	public ItemStack getItemInputI(int i) {
		return i < inputs.size() ? inputs.get(i).copy() : ItemStack.EMPTY;
	}

	public ItemStack getItemOutputI(int i) {
		return i < outputs.size() ? outputs.get(i).copy() : ItemStack.EMPTY;
	}

	/** first output, multiplied */
	public ItemStack getItemOutput0() {
		if (outputs.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return outputs.get(0).copyWithCount(outputs.get(0).getCount() * stackMultiplier);
	}

	public List<ItemStack> getOutputsWithMult() {
		List<ItemStack> ret = new ArrayList<>();
		outputs.forEach(o -> ret.add(o.copyWithCount(o.getCount() * stackMultiplier)));
		return ret;
	}

	public FluidStack getFluidOutput0() {
		if (fluidOutputs.isEmpty() || fluidOutputs.get(0).isEmpty()) {
			return FluidStack.EMPTY;
		}
		FluidStack f = fluidOutputs.get(0);
		return f.copyWithAmount(f.getAmount() * stackMultiplier);
	}

	public int getNeededAmountItem(int index) {
		return index < inputs.size() ? inputs.get(index).getCount() * stackMultiplier : 0;
	}

	public int getNeededAmountFluid(int index) {
		return index < fluidInputs.size() ? fluidInputs.get(index).getAmount() * stackMultiplier : 0;
	}

	/* ---- nbt ---- */

	private static ListTag writeItems(List<ItemStack> list, HolderLookup.Provider reg) {
		ListTag l = new ListTag();
		for (ItemStack s : list) {
			l.add(s.isEmpty() ? new CompoundTag() : s.save(reg));
		}
		return l;
	}

	private static List<ItemStack> readItems(ListTag l, HolderLookup.Provider reg) {
		List<ItemStack> ret = new ArrayList<>();
		for (int i = 0; i < l.size(); i++) {
			ret.add(ItemStack.parseOptional(reg, l.getCompound(i)));
		}
		return ret;
	}

	private static ListTag writeFluids(List<FluidStack> list, HolderLookup.Provider reg) {
		ListTag l = new ListTag();
		for (FluidStack s : list) {
			l.add(s.isEmpty() ? new CompoundTag() : s.save(reg));
		}
		return l;
	}

	private static List<FluidStack> readFluids(ListTag l, HolderLookup.Provider reg) {
		List<FluidStack> ret = new ArrayList<>();
		for (int i = 0; i < l.size(); i++) {
			ret.add(FluidStack.parseOptional(reg, l.getCompound(i)));
		}
		return ret;
	}

	public void writeToNBT(CompoundTag compound, HolderLookup.Provider reg) {
		CompoundTag tags = new CompoundTag();
		tags.putByte("stackMultiplier", (byte) stackMultiplier);
		tags.putInt("powerPerTick", powerPerTick);
		tags.put("InputItems", writeItems(inputs, reg));
		tags.put("OutputItems", writeItems(outputs, reg));
		tags.put("FluidsIn", writeFluids(fluidInputs, reg));
		tags.put("Fluids", writeFluids(fluidOutputs, reg));
		this.writeSubclassData(tags, reg);
		compound.put("operation", tags);
	}

	protected void writeSubclassData(CompoundTag tags, HolderLookup.Provider reg) {
	}

	public void readFromNBT(CompoundTag compound, HolderLookup.Provider reg) {
		CompoundTag tags = compound.getCompound("operation");
		this.stackMultiplier = Math.max(1, tags.getByte("stackMultiplier"));
		this.powerPerTick = tags.getInt("powerPerTick");
		this.inputs = readItems(tags.getList("InputItems", Tag.TAG_COMPOUND), reg);
		this.outputs = readItems(tags.getList("OutputItems", Tag.TAG_COMPOUND), reg);
		this.fluidInputs = readFluids(tags.getList("FluidsIn", Tag.TAG_COMPOUND), reg);
		this.fluidOutputs = readFluids(tags.getList("Fluids", Tag.TAG_COMPOUND), reg);
		this.readSubclassData(tags, reg);
	}

	protected void readSubclassData(CompoundTag tags, HolderLookup.Provider reg) {
	}
}
