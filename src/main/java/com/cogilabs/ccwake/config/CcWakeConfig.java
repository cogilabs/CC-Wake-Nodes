package com.cogilabs.ccwake.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CcWakeConfig {

    // ===== SERVER =====
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue MAX_LOADED_NODES;
    public static final ForgeConfigSpec.IntValue MAX_LOAD_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue DEFAULT_LOAD_RADIUS;
    public static final ForgeConfigSpec.IntValue CHUNK_OPS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue ADVANCED_WAKE_NODE_ENABLED;
    public static final ForgeConfigSpec.IntValue ADVANCED_WAKE_NODE_DEFAULT_RANGE;
    public static final ForgeConfigSpec.IntValue ADVANCED_WAKE_NODE_MAX_RANGE;
    public static final ForgeConfigSpec.BooleanValue ADVANCED_WAKE_NODE_ALLOW_RANGE_CHANGE_WHILE_LOADED;

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

        builder.comment("Advanced Wake Node configuration").push("advanced_wake_node");

        ADVANCED_WAKE_NODE_ENABLED = builder
                .comment("Enable the Advanced Wake Node block and its features.")
                .define("enabled", true);

        ADVANCED_WAKE_NODE_DEFAULT_RANGE = builder
                .comment("Default range for newly placed Advanced Wake Nodes (1, 3 or 5).")
                .defineInRange("default_range", 3, 1, 5);

        ADVANCED_WAKE_NODE_MAX_RANGE = builder
                .comment("Maximum range allowed for Advanced Wake Nodes (1, 3 or 5).")
                .defineInRange("max_range", 5, 1, 5);

        ADVANCED_WAKE_NODE_ALLOW_RANGE_CHANGE_WHILE_LOADED = builder
                .comment("Allow changing the range of an Advanced Wake Node while it is loaded.")
                .define("allow_range_change_while_loaded", true);

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
