package com.pantographinteract;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PantographSyncPayload(int entityId, BlockPos localPos, boolean disabled)
    implements CustomPacketPayload {

    public static final Type<PantographSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("create_interactive_pantographs", "pantograph_sync"));

    public static final StreamCodec<FriendlyByteBuf, PantographSyncPayload> CODEC =
        StreamCodec.ofMember(PantographSyncPayload::write, PantographSyncPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeBlockPos(localPos);
        buf.writeBoolean(disabled);
    }

    private static PantographSyncPayload read(FriendlyByteBuf buf) {
        return new PantographSyncPayload(buf.readVarInt(), buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PantographSyncPayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND)
            return;
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity instanceof AbstractContraptionEntity contraptionEntity)
                PantographInteraction.apply(contraptionEntity, payload.localPos(), payload.disabled());
        });
    }
}
