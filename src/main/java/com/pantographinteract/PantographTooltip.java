package com.pantographinteract;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class PantographTooltip {

    private static final ResourceLocation PANTOGRAPH_ITEM =
        ResourceLocation.fromNamespaceAndPath("electroenergetics", "pantograph");

    private PantographTooltip() {}

    public static void register() {
        Item pantograph = BuiltInRegistries.ITEM.getOptional(PANTOGRAPH_ITEM).orElse(null);
        if (pantograph == null || TooltipModifier.REGISTRY.get(pantograph) != null)
            return;
        TooltipModifier.REGISTRY.register(pantograph,
            new ItemDescription.Modifier(pantograph, FontHelper.Palette.STANDARD_CREATE));
    }
}
