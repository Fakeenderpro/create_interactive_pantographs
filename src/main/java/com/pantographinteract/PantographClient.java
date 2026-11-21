package com.pantographinteract;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = "create_interactive_pantographs", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PantographClient {

    private static final ResourceLocation LAYER =
        ResourceLocation.fromNamespaceAndPath("create_interactive_pantographs", "pantograph_overlay");

    private static final long FADE_MILLIS = 250;

    private static int hoveredEntity = -1;
    private static BlockPos hoveredPos;
    private static long hoverStart;

    private PantographClient() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, PantographClient::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.gameMode == null
            || mc.gameMode.getPlayerMode() == GameType.SPECTATOR || !GogglesItem.isWearingGoggles(mc.player)) {
            hoveredPos = null;
            return;
        }

        AbstractContraptionEntity entity = null;
        BlockHitResult hit = null;
        Couple<Vec3> rayInputs = ContraptionHandlerClient.getRayInputs(mc.player);
        Vec3 origin = rayInputs.getFirst();
        Vec3 end = rayInputs.getSecond();
        AABB area = new AABB(origin, end).inflate(16);
        double best = Double.MAX_VALUE;

        Map<Integer, WeakReference<AbstractContraptionEntity>> contraptions =
            ContraptionHandler.loadedContraptions.get(mc.level);
        for (WeakReference<AbstractContraptionEntity> reference : contraptions.values()) {
            AbstractContraptionEntity candidate = reference.get();
            if (candidate == null || !candidate.getBoundingBox().intersects(area))
                continue;
            BlockHitResult candidateHit = ContraptionHandlerClient.rayTraceContraption(origin, end, candidate);
            if (candidateHit == null)
                continue;
            double distance = candidate.toGlobalVector(candidateHit.getLocation(), 1.0f).distanceTo(origin);
            if (distance < best) {
                best = distance;
                entity = candidate;
                hit = candidateHit;
            }
        }

        if (entity == null || hit == null) {
            hoveredPos = null;
            return;
        }

        if (mc.hitResult instanceof BlockHitResult worldHit && worldHit.getType() == HitResult.Type.BLOCK
            && worldHit.getLocation().distanceTo(origin) < best) {
            hoveredPos = null;
            return;
        }

        MovementContext context = PantographInteraction.findContext(entity, hit.getBlockPos());
        if (context == null
            || !(MovingInteractionBehaviour.REGISTRY.get(context.state.getBlock()) instanceof PantographInteraction)) {
            hoveredPos = null;
            return;
        }

        MovementContext partner = PantographInteraction.findPartner(entity, context);
        BlockPos hoverKey = partner != null && partner.localPos.asLong() < context.localPos.asLong()
            ? partner.localPos : context.localPos;

        if (hoveredPos == null || !hoveredPos.equals(hoverKey) || hoveredEntity != entity.getId()) {
            hoveredPos = hoverKey;
            hoveredEntity = entity.getId();
            hoverStart = System.currentTimeMillis();
        }

        int mode = context.data.getInt(PantographDirection.MODE_KEY);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("    ")
            .append(PantographText.of("overlay.mode").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" "))
            .append(PantographDirection.modeName(mode).withStyle(ChatFormatting.GOLD)));

        draw(graphics, mc, tooltip);
    }

    private static void draw(GuiGraphics graphics, Minecraft mc, List<Component> tooltip) {
        Font font = mc.font;
        int tooltipTextWidth = 0;
        for (Component line : tooltip) {
            int lineWidth = font.width(line);
            if (lineWidth > tooltipTextWidth)
                tooltipTextWidth = lineWidth;
        }

        int tooltipHeight = 8;
        if (tooltip.size() > 1)
            tooltipHeight += 2 + (tooltip.size() - 1) * 10;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        CClient config = AllConfigs.client();
        int offsetX = config.overlayOffsetX.get();
        int offsetY = config.overlayOffsetY.get();
        int posX = Math.min(width / 2 + offsetX, width - tooltipTextWidth - 20);
        int posY = Math.min(height / 2 + offsetY, height - tooltipHeight - 20);

        float fade = Mth.clamp((System.currentTimeMillis() - hoverStart) / (float) FADE_MILLIS, 0.0f, 1.0f);

        boolean custom = config.overlayCustomColor.get();
        Color background = custom
            ? new Color(config.overlayBackgroundColor.get())
            : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(0.75f);
        Color borderTop = custom
            ? new Color(config.overlayBorderColorTop.get())
            : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
        Color borderBottom = custom
            ? new Color(config.overlayBorderColorBot.get())
            : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

        graphics.pose().pushPose();
        graphics.pose().translate(Math.pow(1.0 - fade, 3.0) * Math.signum(offsetX + 0.5f) * 8.0, 0.0, 0.0);

        Color fadedBackground = background.scaleAlpha(fade);
        Color fadedTop = borderTop.scaleAlpha(fade);
        Color fadedBottom = borderBottom.scaleAlpha(fade);

        GuiGameElement.of(AllItems.GOGGLES.asStack()).at(posX + 10, posY - 16, 450).render(graphics);
        RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1,
            fadedBackground.getRGB(), fadedTop.getRGB(), fadedBottom.getRGB(), font);

        graphics.pose().popPose();
    }
}
