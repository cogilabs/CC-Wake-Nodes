package com.cogilabs.ccwake;

import com.cogilabs.ccwake.registry.ModBlocks;
import com.cogilabs.ccwake.registry.ModBlockEntities;
import com.cogilabs.ccwake.registry.ModItems;
import com.cogilabs.ccwake.config.CcWakeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CcWakeMod.MOD_ID)
public class CcWakeMod {
    public static final String MOD_ID = "ccwake";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CcWakeMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CcWakeConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CcWakeConfig.COMMON_SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }
}
