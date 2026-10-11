package com.pantographinteract;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;

public class PantographInteraction extends MovingInteractionBehaviour {

    private static final ResourceLocation WRENCH = ResourceLocation.fromNamespaceAndPath("create", "wrench");

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand,
                                           BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (activeHand != InteractionHand.MAIN_HAND)
            return false;

        if (player.level().isClientSide())
            return true;

        MovementContext context = findContext(contraptionEntity, localPos);
        if (context == null)
            return false;

        if (player.isShiftKeyDown() || isWrench(player.getMainHandItem()))
            return cycleMode(player, contraptionEntity, context);

        int currentMode = context.data.getInt(PantographDirection.MODE_KEY);
        if (PantographDirection.isAutomatic(currentMode)) {
            player.displayClientMessage(
                Component.literal("Automatic mode, sneak-click or use a wrench to change it"), true);
            return true;
        }
        if (PantographDirection.isSchedule(currentMode)) {
            player.displayClientMessage(
                Component.literal("Schedule mode, sneak-click or use a wrench to change it"), true);
            return true;
        }

        boolean disable = !context.disabled;
        setPair(contraptionEntity, context, disable);

        player.level().playSound(null, player.blockPosition(),
            SoundEvents.CHAIN_HIT, SoundSource.NEUTRAL, 0.5f, disable ? 0.8f : 1.0f);
        player.displayClientMessage(Component.literal("Pantograph " + (disable ? "lowered" : "raised")), true);
        return true;
    }

    private static boolean cycleMode(Player player, AbstractContraptionEntity contraptionEntity,
                                     MovementContext context) {
        int mode = (context.data.getInt(PantographDirection.MODE_KEY) + 1) % PantographDirection.MODE_COUNT;
        setMode(context, mode);

        MovementContext partner = findPartner(contraptionEntity, context);
        if (partner != null)
            setMode(partner, mode);

        String message = switch (mode) {
            case PantographDirection.FOLLOW -> "Mode: automatic";
            case PantographDirection.OPPOSE -> "Mode: automatic (inverted)";
            case PantographDirection.SCHEDULE_STANDARD -> "Mode: schedule";
            case PantographDirection.SCHEDULE_INVERTED -> "Mode: schedule (inverted)";
            default -> "Mode: manual";
        };
        player.displayClientMessage(Component.literal(message), true);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.5f, 1.0f);
        return true;
    }

    private static void setMode(MovementContext context, int mode) {
        context.data.putInt(PantographDirection.MODE_KEY, mode);
        context.data.remove(PantographSchedule.RAISED_KEY);
    }

    private static boolean isWrench(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(WRENCH);
    }

    private static void setPair(AbstractContraptionEntity contraptionEntity, MovementContext context, boolean disabled) {
        apply(contraptionEntity, context.localPos, disabled);
        send(contraptionEntity, context.localPos, disabled);

        MovementContext partner = findPartner(contraptionEntity, context);
        if (partner != null) {
            apply(contraptionEntity, partner.localPos, disabled);
            send(contraptionEntity, partner.localPos, disabled);
        }
    }

    private static void send(AbstractContraptionEntity contraptionEntity, BlockPos localPos, boolean disabled) {
        PacketDistributor.sendToPlayersTrackingEntity(contraptionEntity,
            new PantographSyncPayload(contraptionEntity.getId(), localPos, disabled));
    }

    public static void apply(AbstractContraptionEntity contraptionEntity, BlockPos localPos, boolean disabled) {
        MovementContext context = findContext(contraptionEntity, localPos);
        if (context != null)
            context.disabled = disabled;
    }

    private static MovementContext findPartner(AbstractContraptionEntity contraptionEntity, MovementContext context) {
        BlockState state = context.state;
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING) || !isDouble(state))
            return null;

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        MovementContext partner = findContext(contraptionEntity, context.localPos.relative(facing.getOpposite()));
        if (partner == null || partner.state.getBlock() != state.getBlock())
            return null;
        if (!partner.state.hasProperty(HorizontalDirectionalBlock.FACING)
            || partner.state.getValue(HorizontalDirectionalBlock.FACING) != facing.getOpposite())
            return null;
        return partner;
    }

    static boolean isDouble(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && booleanProperty.getName().equals("double"))
                return state.getValue(booleanProperty);
        }
        return false;
    }

    private static MovementContext findContext(AbstractContraptionEntity contraptionEntity, BlockPos localPos) {
        for (var actor : contraptionEntity.getContraption().getActors()) {
            MovementContext context = actor.right;
            if (context != null && context.localPos.equals(localPos))
                return context;
        }
        return null;
    }
}
