package net.fayber.invisibleitemframes;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;

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
 * and returns {@code FAIL}, so no vanilla use packet is sent at all and the
 * server acts on the payload alone, on the server thread, outside the shared
 * event chain.
 *
 * <p>Item frame interactions have no client-side event hook at all (see
 * {@link ItemFrameInteractionHandler}), so the keybind gesture for frames is
 * intercepted earlier, by a client mixin into
 * {@code MultiPlayerGameMode.interact}, which sends one of these payloads
 * directly and cancels the vanilla method before it can send its own packet.
 *
 * <p>The server-side event handlers ({@link ItemFrameInteractionHandler},
 * {@link SignInteractionHandler}) remain as the fallback for vanilla clients
 * (no mod installed) and for the plain/shift gestures on a modded client:
 * there the vanilla use/interact packet arrives (it already carries the
 * sneak flag) and the handler resolves plain vs. shift itself. Only the
 * keybind gesture - an arbitrary key the vanilla packet has no field for -
 * needs an explicit payload.
 *
 * <p>Because the keybind's held/not-held state cannot be re-verified on the
 * server (there is no synced vanilla state for it), the server-side handlers
 * below re-check only what IS knowable server-side: permission, empty hand
 * (for toggle), and interaction range - the same trust level already applied
 * to the plain vanilla-packet path (a modified client could always misreport
 * sneak too).
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
            case InteractPayload.FORCE_INTERACT_SIGN -> handleForceInteractSign(payload, player);
            case InteractPayload.FORCE_INTERACT_FRAME -> handleForceInteractFrame(payload, player);
            default -> { }
        }
    }

    private static void handleToggleSign(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!config.enableSignToggle || !SignInteractionHandler.hasTogglePermission(player, config)) {
            return;
        }
        if (!player.getMainHandItem().isEmpty()) {
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
        if (!player.getMainHandItem().isEmpty()) {
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
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        BlockPos signPos = BlockPos.of(payload.pos());
        BlockState state = player.level().getBlockState(signPos);
        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }
        boolean invisible = state.getValue(SignProperties.INVISIBLE);
        boolean clickThroughWouldApply = invisible ? config.clickThroughInvisibleSigns : config.clickThroughVisibleSigns;
        if (!clickThroughWouldApply) {
            return;
        }
        if (!player.isWithinBlockInteractionRange(signPos, 1.0)) {
            return;
        }
        SignInteractionHandler.clickThroughIfSigned(player, player.level(), signPos);
    }

    private static void handleClickThroughFrame(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!(player.level().getEntity(payload.entityId()) instanceof ItemFrame frame)) {
            return;
        }
        boolean clickThroughWouldApply = frame.isInvisible()
                ? config.clickThroughInvisibleFrames
                : config.clickThroughVisibleFrames;
        if (!clickThroughWouldApply) {
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

    private static void handleForceInteractSign(InteractPayload payload, ServerPlayer player) {
        BlockPos signPos = BlockPos.of(payload.pos());
        if (!player.isWithinBlockInteractionRange(signPos, 1.0)) {
            return;
        }
        SignInteractionHandler.forceInteractIfSigned(player, player.level(), signPos);
    }

    private static void handleForceInteractFrame(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (!(player.level().getEntity(payload.entityId()) instanceof ItemFrame frame)) {
            return;
        }
        if (frame instanceof GlowItemFrame && !config.affectGlowItemFrames) {
            return;
        }
        if (!player.isWithinEntityInteractionRange(frame, 1.0)) {
            return;
        }
        ItemFrameInteractionHandler.forceInteract(player, frame, InteractionHand.MAIN_HAND);
    }

    /** Payload sent by the mod's client to request one interaction action. */
    public record InteractPayload(int kind, long pos, int entityId) implements CustomPacketPayload {
        public static final int TOGGLE_SIGN = 0;
        public static final int TOGGLE_FRAME = 1;
        public static final int CLICK_THROUGH_SIGN = 2;
        public static final int CLICK_THROUGH_FRAME = 3;
        public static final int FORCE_INTERACT_SIGN = 4;
        public static final int FORCE_INTERACT_FRAME = 5;

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return INTERACT_TYPE;
        }
    }
}
