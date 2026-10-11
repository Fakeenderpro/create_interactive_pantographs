package com.pantographinteract;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;

import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class PantographInstruction extends ScheduleInstruction {

    public static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath("create_interactive_pantographs", "pantograph");

    private static final ResourceLocation PANTOGRAPH_ITEM =
        ResourceLocation.fromNamespaceAndPath("electroenergetics", "pantograph");

    private static final String STANDARD = "Standard";
    private static final String INVERTED = "Inverted";
    private static final int KEEP = 0;
    private static final int RAISE = 1;
    private static final int LOWER = 2;

    @Override
    public Pair<ItemStack, Component> getSummary() {
        return Pair.of(icon(), summary());
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public ItemStack getSecondLineIcon() {
        return icon();
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    @Override
    public List<Component> getTitleAs(String type) {
        return List.of(Component.literal("Pantographs: ").withStyle(ChatFormatting.GOLD)
            .append(summary().withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        builder.addSelectionScrollInput(0, 56, (input, label) -> configure(input, "Standard pantographs", "Std"), STANDARD);
        builder.addSelectionScrollInput(60, 58, (input, label) -> configure(input, "Inverted pantographs", "Inv"), INVERTED);
    }

    @Override
    public DiscoveredPath start(ScheduleRuntime runtime, Level level) {
        UUID trainId = runtime.train.id;
        schedule(trainId, PantographDirection.SCHEDULE_STANDARD, intData(STANDARD));
        schedule(trainId, PantographDirection.SCHEDULE_INVERTED, intData(INVERTED));
        runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
        runtime.currentEntry++;
        return null;
    }

    private static void configure(SelectionScrollInput input, String title, String prefix) {
        input.forOptions(List.of(
            Component.literal(prefix + ": Keep"),
            Component.literal(prefix + ": Raise"),
            Component.literal(prefix + ": Lower")))
            .titled(Component.literal(title));
    }

    private static void schedule(UUID trainId, int mode, int value) {
        if (value == RAISE)
            PantographSchedule.set(trainId, mode, true);
        else if (value == LOWER)
            PantographSchedule.set(trainId, mode, false);
    }

    private MutableComponent summary() {
        List<String> parts = new ArrayList<>();
        describe(parts, "Standard", intData(STANDARD));
        describe(parts, "Inverted", intData(INVERTED));
        return Component.literal(parts.isEmpty() ? "No change" : String.join(", ", parts));
    }

    private static void describe(List<String> parts, String group, int value) {
        if (value == RAISE)
            parts.add(group + " raised");
        else if (value == LOWER)
            parts.add(group + " lowered");
    }

    private static ItemStack icon() {
        return BuiltInRegistries.ITEM.getOptional(PANTOGRAPH_ITEM)
            .map(item -> new ItemStack(item))
            .orElseGet(() -> new ItemStack(Items.IRON_BARS));
    }
}
