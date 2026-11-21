package com.pantographinteract;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class PantographText {

    private static final String PREFIX = "create_interactive_pantographs.";

    private PantographText() {}

    public static MutableComponent of(String key) {
        return Component.translatable(PREFIX + key);
    }
}
