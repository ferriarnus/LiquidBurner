package com.ferriarnus.liquidburner;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlazeTank extends SmartFluidTank {

    public BlazeTank(int capacity, Consumer<FluidStack> updateCallback, Predicate<FluidStack> validator) {
        super(capacity, updateCallback);
        setValidator(validator);
    }
}
