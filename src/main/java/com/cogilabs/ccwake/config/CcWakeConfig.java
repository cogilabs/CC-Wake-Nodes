package com.cogilabs.ccwake.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CcWakeConfig {

    // ===== SERVER =====
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue MAX_LOADED_NODES;
    public static final ForgeConfigSpec.IntValue MAX_LOAD_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue DEFAULT_LOAD_RADIUS;
        public static final ForgeConfigSpec.IntValue CHUNK_OPS_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Wake Nodes server configuration").push("chunk_loading");

        MAX_LOADED_NODES = builder
                .comment("Maximum number of nodes that can be chunk-loaded simultaneously.")
                .defineInRange("max_loaded_nodes", 16, 1, 256);

        MAX_LOAD_DURATION_SECONDS = builder
                .comment("Maximum duration in seconds allowed for loadFor().")
                .defineInRange("max_load_duration_seconds", 300, 1, 86400);

        DEFAULT_LOAD_RADIUS = builder
                .comment("Radius of chunks loaded around the computer. 0 = only the computer's chunk.")
                .defineInRange("default_load_radius", 0, 0, 8);

        CHUNK_OPS_PER_TICK = builder
                .comment("Maximum number of queued chunk load/unload operations processed per server tick.")
                .defineInRange("chunk_ops_per_tick", 3, 1, 64);

        builder.pop();

        SERVER_SPEC = builder.build();
    }

    // ===== COMMON =====
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_NODE_LOGS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Wake Nodes common configuration").push("logging");

        ENABLE_NODE_LOGS = builder
                .comment("Enable server-side log messages for wake node events.")
                .define("enable_node_logs", true);

        builder.pop();

        COMMON_SPEC = builder.build();
    }
}
