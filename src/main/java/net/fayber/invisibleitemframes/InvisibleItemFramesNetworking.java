package net.fayber.invisibleitemframes;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;

/**
 * Client-to-server payloads for the interaction features.
 *
 * <p>Why this exists: Fabric's {@code UseEntityCallback} only fires on the
 * server, but {@code UseBlockCallback} fires on BOTH sides, and the client
 * side runs inside {@code MultiPlayerGameMode.useItemOn} where a non-PASS
 * result cancels vanilla processing (so no sign edit screen prediction) while
 * still sending the use packet through the prediction mechanism. Relying on
 * the event alone leaves the sign toggle exposed to whatever other listeners
 * are registered before this mod, which is exactly what made the sign toggle
 * a no-op in practice. Instead, the mod's client sends one of these payloads
 * and returns {@code FAIL} from the event, so no vanilla use packet is sent
 * at all and the server acts on the payload alone, on the server thread,
 * outside the shared event chain.
 *
 * <p>The server-side event handlers remain as the fallback for vanilla
 * clients (no mod installed): there the vanilla use packet arrives and the
 * handler performs the toggle/click-through directly. The two paths can never
 * both fire for one click: a modded client returns FAIL and sends no vanilla
 * packet, a vanilla client sends no payload.
 */
public final class InvisibleItemFramesNetworking {
    private InvisibleItemFramesNetworking() {}

    public static final CustomPacketPayload.Type<InteractPayload> INTERACT_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(InvisibleItemFramesMod.MOD_ID, "interact"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, InteractPayload> INTERACT_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InteractPayload::kind,
                    ByteBufCodecs.LONG, InteractPayload::pos,
                    ByteBufCodecs.VAR_INT, InteractPayload::entityId,
                    InteractPayload::new);

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(INTERACT_TYPE, INTERACT_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(INTERACT_TYPE,
                (payload, context) -> handleOnServer(payload, context.player()));
    }

    private static void handleOnServer(InteractPayload payload, ServerPlayer player) {
        switch (payload.kind()) {
            case InteractPayload.TOGGLE_SIGN -> handleToggleSign(payload, player);
            case InteractPayload.TOGGLE_FRAME -> handleToggleFrame(payload, player);
            case InteractPayload.CLICK_THROUGH_SIGN -> handleClickThroughSign(payload, player);
            case InteractPayload.CLICK_THROUGH_FRAME -> handleClickThroughFrame(payload, player);
            default -> { }
        }
    }

    private static void handleToggleSign(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!config.enableSignToggle || !SignInteractionHandler.hasTogglePermission(player, config)) {
            return;
        }
        // The gesture is re-checked here so a modded client cannot toggle
        // without actually sneaking with an empty hand.
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        BlockPos pos = BlockPos.of(payload.pos());
        if (!player.isWithinBlockInteractionRange(pos, 1.0)) {
            return;
        }
        SignInteractionHandler.toggleSignIfSigned(player.level(), pos, player);
    }

    private static void handleToggleFrame(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!config.enableItemFrameToggle || !SignInteractionHandler.hasTogglePermission(player, config)) {
            return;
        }
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (!(player.level().getEntity(payload.entityId()) instanceof ItemFrame frame)) {
            return;
        }
        if (frame instanceof GlowItemFrame && !config.affectGlowItemFrames) {
            return;
        }
        if (!player.isWithinEntityInteractionRange(frame, 1.0)) {
            return;
        }
        ItemFrameInteractionHandler.toggleFrame(frame);
        InvisibleItemFramesMod.LOGGER.info("{} toggled an item frame at ({}, {}, {}) {}",
                player.getGameProfile().name(), (int) frame.getX(), (int) frame.getY(), (int) frame.getZ(),
                frame.isInvisible() ? "invisible" : "visible");
    }

    private static void handleClickThroughSign(InteractPayload payload, ServerPlayer player) {
        if (!InvisibleItemFramesConfig.get().clickThroughSigns) {
            return;
        }
        BlockPos signPos = BlockPos.of(payload.pos());
        if (!player.isWithinBlockInteractionRange(signPos, 1.0)) {
            return;
        }
        SignInteractionHandler.clickThroughIfSigned(player, player.level(), signPos);
    }

    private static void handleClickThroughFrame(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!config.clickThroughInvisibleFrames) {
            return;
        }
        if (!(player.level().getEntity(payload.entityId()) instanceof ItemFrame frame) || !frame.isInvisible()) {
            return;
        }
        if (frame instanceof GlowItemFrame && !config.affectGlowItemFrames) {
            return;
        }
        if (!player.isWithinEntityInteractionRange(frame, 1.0)) {
            return;
        }
        BlockPos framePos = BlockPos.containing(frame.getX(), frame.getY(), frame.getZ());
        ItemFrameInteractionHandler.clickThrough(player, player.level(), framePos);
    }

    /** Payload sent by the mod's client to request one interaction action. */
    public record InteractPayload(int kind, long pos, int entityId) implements CustomPacketPayload {
        public static final int TOGGLE_SIGN = 0;
        public static final int TOGGLE_FRAME = 1;
        public static final int CLICK_THROUGH_SIGN = 2;
        public static final int CLICK_THROUGH_FRAME = 3;

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return INTERACT_TYPE;
        }
    }
}
