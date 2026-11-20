package com.pantographinteract;

import com.george_vi.electroenergetics.content.railway_electrification.pantograph.TrainPantographEntry;
import com.george_vi.electroenergetics.mixin_interfaces.IPantographList;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PantographDirection {

    public static final String MODE_KEY = "PantographMode";
    public static final int MANUAL = 0;
    public static final int FOLLOW = 1;
    public static final int OPPOSE = 2;
    public static final int SCHEDULE_STANDARD = 3;
    public static final int SCHEDULE_INVERTED = 4;
    public static final int MODE_COUNT = 5;
    public static final String AUTO_KEY = "PantographAutoRaised";
    public static final String MANUAL_KEY = "PantographManualRaised";

    private static final double MIN_SPEED = 0.01;
    private static final int CHECK_INTERVAL = 5;
    private static final int RESEND_INTERVAL = 40;
    private static final int STARTUP_TICKS = 20;
    private static final int RESTORE_TICKS = 2;

    private PantographDirection() {}

    public static boolean isAutomatic(int mode) {
        return mode == FOLLOW || mode == OPPOSE;
    }

    public static boolean isSchedule(int mode) {
        return mode == SCHEDULE_STANDARD || mode == SCHEDULE_INVERTED;
    }

    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.tickCount > RESTORE_TICKS || !(entity instanceof CarriageContraptionEntity carriageEntity))
            return;

        for (var actor : carriageEntity.getContraption().getActors()) {
            MovementContext context = actor.right;
            if (context == null)
                continue;

            Boolean raise = storedRaise(context);
            if (raise != null)
                context.disabled = !raise;
        }
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof CarriageContraptionEntity carriageEntity)
            || !(event.getEntity() instanceof ServerPlayer player))
            return;

        for (var actor : carriageEntity.getContraption().getActors()) {
            MovementContext context = actor.right;
            if (context == null)
                continue;

            Boolean raise = storedRaise(context);
            if (raise != null) {
                context.disabled = !raise;
                PacketDistributor.sendToPlayer(player,
                    new PantographSyncPayload(carriageEntity.getId(), context.localPos, context.disabled));
            }
        }
    }

    public static Boolean storedRaise(MovementContext context) {
        int mode = context.data.getInt(MODE_KEY);
        String key = mode == MANUAL ? MANUAL_KEY
            : isAutomatic(mode) ? AUTO_KEY
            : isSchedule(mode) ? PantographSchedule.RAISED_KEY : null;
        if (key == null || !context.data.contains(key))
            return null;
        return context.data.getBoolean(key);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof CarriageContraptionEntity carriageEntity))
            return;
        if (entity.tickCount > STARTUP_TICKS && entity.tickCount % CHECK_INTERVAL != 0)
            return;

        Carriage carriage = carriageEntity.getCarriage();
        if (carriage == null || carriage.train == null || !(carriage instanceof IPantographList list))
            return;

        double speed = carriage.train.speed;
        boolean moving = Math.abs(speed) > MIN_SPEED;
        boolean forward = speed > 0;
        boolean resend = entity.tickCount % RESEND_INTERVAL == 0;

        for (var actor : carriageEntity.getContraption().getActors()) {
            MovementContext context = actor.right;
            if (context == null)
                continue;

            int mode = context.data.getInt(MODE_KEY);
            if (mode == MANUAL)
                continue;

            TrainPantographEntry entry = list.getPantographState(context.localPos);
            if (entry == null)
                continue;

            Boolean raise = null;
            if (isAutomatic(mode)) {
                if (moving) {
                    boolean facesTravel = PantographInteraction.isDouble(context.state)
                        ? forward : entry.facingForward == forward;
                    raise = mode == FOLLOW ? facesTravel : !facesTravel;
                    context.data.putBoolean(AUTO_KEY, raise);
                } else if (context.data.contains(AUTO_KEY)) {
                    raise = context.data.getBoolean(AUTO_KEY);
                }
            } else if (isSchedule(mode)) {
                Boolean scheduled = PantographSchedule.get(carriage.train.id, mode);
                if (scheduled != null)
                    context.data.putBoolean(PantographSchedule.RAISED_KEY, scheduled);
                if (context.data.contains(PantographSchedule.RAISED_KEY))
                    raise = context.data.getBoolean(PantographSchedule.RAISED_KEY);
            }

            boolean send = resend;
            if (raise != null && context.disabled == raise) {
                PantographInteraction.apply(carriageEntity, context.localPos, !raise);
                send = true;
            }

            if (send) {
                PacketDistributor.sendToPlayersTrackingEntity(carriageEntity,
                    new PantographSyncPayload(carriageEntity.getId(), context.localPos, context.disabled));
            }
        }
    }
}
