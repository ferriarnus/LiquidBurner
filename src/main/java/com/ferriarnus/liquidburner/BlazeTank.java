package com.ferriarnus.liquidburner;

import com.simibubi.create.foundation.fluid.SmartFluidTank;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlazeTank extends SmartFluidTank {

    public BlazeTank(int capacity, Consumer<FluidStack> updateCallback, Predicate<FluidStack> validator) {
        super(capacity, updateCallback);
        setValidator(validator);
    }
}
