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

// Client-to-server payloads for the interaction features.
//
// Why we need our own packet: in fabric-api 26.x both UseBlockCallback and
// UseEntityCallback fire client-side too, so we can read the keybind there
// (it's just an arbitrary key, vanilla's packets have no field for it).
// Whenever the gesture isn't PLAIN, the client sends one of these payloads
// and returns FAIL, which cancels the vanilla use/interact before it sends
// its own packet - so no double rotation/click-through/prediction - and the
// server acts on our payload directly instead of trusting client state.
//
// The vanilla event handlers below still handle everything else (plain
// clicks, shift, and vanilla-only clients that don't send this packet at
// all): only the keybind gesture actually needs this custom payload.
//
// The keybind's held/not-held state can't be verified server-side (no
// synced vanilla field for it), so every handler here re-checks whatever
// IS knowable server-side before acting: permission, empty hand, range.
// Same trust level the vanilla-packet path already gets - a modified
// client could lie about sneak too, this is nothing new.
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
        if (config.requireEmptyHandForToggle && !player.getMainHandItem().isEmpty()) {
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
        if (config.requireEmptyHandForToggle && !player.getMainHandItem().isEmpty()) {
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
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        // Don't just trust the client's own gating - re-check server-side too,
        // otherwise a modified client could force the editor open with an
        // item in hand even when the config says that shouldn't happen.
        if (config.requireEmptyHandForInteraction && !player.getMainHandItem().isEmpty()) {
            return;
        }
        BlockPos signPos = BlockPos.of(payload.pos());
        if (!player.isWithinBlockInteractionRange(signPos, 1.0)) {
            return;
        }
        SignInteractionHandler.forceInteractIfSigned(player, player.level(), signPos);
    }

    private static void handleForceInteractFrame(InteractPayload payload, ServerPlayer player) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (config.requireEmptyHandForInteraction && !player.getMainHandItem().isEmpty()) {
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
