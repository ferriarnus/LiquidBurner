package com.ferriarnus.liquidburner.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class LiquidBurning implements Recipe<FluidContainer> {

    private final FluidStack fluid;
    private final int burntime;
    private final int superheattime;
    private final ResourceLocation id;
    public static final RecipeSerializer<LiquidBurning> SERIALIZER = new Serializer();

    public LiquidBurning(FluidStack fluid, int burntime, int superheattime, ResourceLocation pRecipeId) {
        this.fluid = fluid;
        this.burntime = burntime;
        this.superheattime = superheattime;
        this.id = pRecipeId;
    }

    @Override
    public boolean matches(FluidContainer pContainer, Level pLevel) {
        if (pContainer.getStack().isFluidEqual(this.fluid)) {
            return true;
        }
        return false;
    }

    @Override
    public ItemStack assemble(FluidContainer pContainer, RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.LIQUIDBURNING;
    }

    public int getSuperheattime() {
        return superheattime;
    }

    public int getBurntime() {
        return burntime;
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public static class Serializer implements RecipeSerializer<LiquidBurning> {

        public Serializer() {

        }

        @Override
        public LiquidBurning fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            String rl = pSerializedRecipe.get("fluid").getAsString();
            if (!BuiltInRegistries.FLUID.containsKey(new ResourceLocation(rl))) {
                throw new JsonSyntaxException("Unknown fluid '" + rl + "'");
            }
            FluidStack stack = new FluidStack(BuiltInRegistries.FLUID.get(new ResourceLocation(rl)), 1000);
            int burntime = pSerializedRecipe.get("burntime").getAsInt();
            int superheattime = 0;
            if (pSerializedRecipe.has("superheattime")) {
                superheattime = pSerializedRecipe.get("superheattime").getAsInt();
            }
            return new LiquidBurning(stack, burntime, superheattime, pRecipeId);
        }

        @Override
        public LiquidBurning fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            ResourceLocation rl = pBuffer.readResourceLocation();
            if (!BuiltInRegistries.FLUID.containsKey(rl)) {
                throw new JsonSyntaxException("Unknown fluid '" + rl + "'");
            }
            FluidStack stack = new FluidStack(BuiltInRegistries.FLUID.get(rl), 1000);
            int burntime = pBuffer.readInt();
            int superheattime = pBuffer.readInt();
            return new LiquidBurning(stack, burntime, superheattime, pRecipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, LiquidBurning pRecipe) {
            pBuffer.writeResourceLocation(BuiltInRegistries.FLUID.getKey(pRecipe.fluid.getFluid()));
            pBuffer.writeInt(pRecipe.burntime);
            pBuffer.writeInt(pRecipe.superheattime);
        }
    }

}
