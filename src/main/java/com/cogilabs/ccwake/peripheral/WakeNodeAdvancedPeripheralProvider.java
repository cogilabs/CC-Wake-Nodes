package com.cogilabs.ccwake.peripheral;

import com.cogilabs.ccwake.block.WakeNodeAdvancedBlockEntity;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class WakeNodeAdvancedPeripheralProvider implements IPeripheralProvider {

    @NotNull
    @Override
    public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos pos, @NotNull Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof WakeNodeAdvancedBlockEntity advancedNode) {
            return LazyOptional.of(() -> new WakeNodeAdvancedPeripheral(advancedNode));
        }
        return LazyOptional.empty();
    }
}
