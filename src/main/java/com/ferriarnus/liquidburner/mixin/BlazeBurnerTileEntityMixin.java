package com.ferriarnus.liquidburner.mixin;

import com.ferriarnus.liquidburner.BlazeTank;
import com.ferriarnus.liquidburner.recipe.FluidContainer;
import com.ferriarnus.liquidburner.recipe.LiquidBurning;
import com.ferriarnus.liquidburner.recipe.RecipeRegistry;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerTileEntityMixin extends SmartBlockEntity implements SidedStorageBlockEntity {
    @Unique
    BlazeTank tank = new BlazeTank(81000, fluidStack -> recipeFluids(), getFluidStackPredicate());

    @Shadow(remap = false)
    protected BlazeBurnerBlockEntity.FuelType activeFuel;
    @Shadow(remap = false)
    protected int remainingBurnTime;
    @Unique
    private LiquidBurning lb;

    public BlazeBurnerTileEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    public boolean recipeFluids() {
        if (this.tank.getFluidAmount() < this.tank.getCapacity()) {
            return false;
        }
        FluidContainer container = new FluidContainer(this.tank.getFluid());
        if (this.level == null)
            return false;
        Optional<LiquidBurning> recipe = this.level.getRecipeManager().getRecipeFor(RecipeRegistry.LIQUIDBURNING, container, this.level);
        if (recipe.isPresent()) {
            lb = recipe.get();
            BlazeBurnerBlockEntity.FuelType newFuel;
            int newBurnTime;
            if (lb.getSuperheattime() > 0) {
                newBurnTime = lb.getSuperheattime();
                newFuel = BlazeBurnerBlockEntity.FuelType.SPECIAL;
            } else {
                newBurnTime = lb.getBurntime();
                newFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
                lb = null;
            }

            if (newFuel.ordinal() < this.activeFuel.ordinal()) {
                return true;
            } else if (this.activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL && this.remainingBurnTime > 20) {
                return true;
            } else {
                if (newFuel == this.activeFuel) {
                    if (this.remainingBurnTime + newBurnTime > 10000) {
                        return false;
                    }

                    newBurnTime = Mth.clamp(this.remainingBurnTime + newBurnTime, 0, 10000);
                }
                this.activeFuel = newFuel;
                this.remainingBurnTime = newBurnTime;
                if (this.level.isClientSide) {
                    this.spawnParticleBurst(this.activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL);
                } else {
                    BlazeBurnerBlock.HeatLevel prev = this.getHeatLevelFromBlock();
                    this.playSound();
                    this.updateBlockState();
                    if (prev != this.getHeatLevelFromBlock()) {
                        this.level.playSound((Player)null, this.getBlockPos(), SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS, 0.125F + this.level.random.nextFloat() * 0.125F, 1.15F - this.level.random.nextFloat() * 0.25F);
                    }
                }
                tank.setFluid(FluidStack.EMPTY);
                return true;
            }
        }
        lb = null;
        return false;
    }

    @Unique
    private Predicate<FluidStack> getFluidStackPredicate() {
        if (this.level == null)
            return fluidStack -> true;
        return fluid -> this.level.getRecipeManager().getAllRecipesFor(RecipeRegistry.LIQUIDBURNING).stream().anyMatch(r -> r.getFluid().isFluidEqual(fluid));
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 5000), remap = false)
    public int liquidburner$addBurntime(int constant) {
        if (lb != null) {
            int temp = lb.getBurntime();
            lb = null;
            return temp;
        }
        return constant;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlockEntity;updateBlockState()V", shift = At.Shift.BEFORE, ordinal = 1), method = "tick", remap = false)
    public void liquidburner$appendTick(CallbackInfo ci) {
        if (this.remainingBurnTime <= 0) {
            recipeFluids();
        }
    }

    @Inject(at = @At("HEAD"), method = "read", remap = false)
    public void liquidburner$appendRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        this.tank.readFromNBT(compound.getCompound("tank"));
    }

    @Inject(at = @At("HEAD"), method = "write", remap = false)
    public void liquidburner$appendWrite(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        CompoundTag tag = new CompoundTag();
        this.tank.writeToNBT(tag);
        compound.put("tank", tag);
    }

    @Shadow(remap = false)
    public abstract void updateBlockState();

    @Shadow(remap = false)
    protected abstract void playSound();

    @Shadow(remap = false)
    public abstract BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock();

    @Shadow(remap = false)
    public abstract void spawnParticleBurst(boolean b);

    @Shadow(remap = false)
    public abstract void tick();

    @Override
    public @Nullable Storage<FluidVariant> getFluidStorage(Direction side) {
        return tank;
    }
}
