package com.pantographinteract;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class PantographCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String TARGET_NAMESPACE = "electroenergetics";
    public static final String PATH_CONTAINS = "pantograph";

    private PantographCompat() {}

    public static void register() {
        int registered = 0;
        int namespaceBlocks = 0;
        int pantographBlocks = 0;

        LOGGER.info("[PantographInteract] Starting registration of pantograph interactions...");

        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(TARGET_NAMESPACE))
                continue;
            namespaceBlocks++;

            if (!id.getPath().contains(PATH_CONTAINS))
                continue;

            pantographBlocks++;
            LOGGER.info("[PantographInteract] Found pantograph block: {}", id);

            MovingInteractionBehaviour.REGISTRY.register(block, new PantographInteraction());
            LOGGER.info("[PantographInteract] Registered interaction for {}", id);
            registered++;
        }

        LOGGER.info("[PantographInteract] Registration complete. Found {} blocks in namespace, {} pantograph blocks, registered {} interactions",
            namespaceBlocks, pantographBlocks, registered);

        if (namespaceBlocks == 0) {
            LOGGER.warn("[PantographInteract] No blocks found in namespace '{}'", TARGET_NAMESPACE);
        } else if (registered == 0) {
            LOGGER.warn("[PantographInteract] No pantograph registered. All blocks in namespace:");
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id.getNamespace().equals(TARGET_NAMESPACE))
                    LOGGER.warn("  - {}", id);
            }
        }
    }
}
