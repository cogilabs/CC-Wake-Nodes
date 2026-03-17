package com.cogilabs.ccwake.event;

import com.cogilabs.ccwake.CcWakeMod;
import com.cogilabs.ccwake.peripheral.WakeControllerPeripheralProvider;
import com.cogilabs.ccwake.peripheral.WakeNodePeripheralProvider;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = CcWakeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSetupEvents {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeComputerCraftAPI.registerPeripheralProvider(new WakeNodePeripheralProvider());
            ForgeComputerCraftAPI.registerPeripheralProvider(new WakeControllerPeripheralProvider());
        });
    }
}
