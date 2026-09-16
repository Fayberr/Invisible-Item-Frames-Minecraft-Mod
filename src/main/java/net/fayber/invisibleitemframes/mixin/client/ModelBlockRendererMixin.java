package net.fayber.invisibleitemframes.mixin.client;

import net.fayber.invisibleitemframes.sign.IInvisibleSign;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// In 26.2 the wooden sign model is rendered as a chunk-mesh block model via
// ModelBlockRenderer#tesselateBlock (text is still handled by AbstractSignRenderer).
//
// When the sign block entity is marked invisible, we cancel tesselateBlock so
// no quads are emitted to the chunk mesh. Text and wax continue rendering via
// the block entity renderer.
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {

    @Inject(
            method = "tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iif$skipSignModelWhenInvisible(
            BlockQuadOutput output,
            float f, float g, float h,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            BlockStateModel model,
            long seed,
            CallbackInfo ci
    ) {
        if (state.getBlock() instanceof SignBlock && level != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IInvisibleSign sign && sign.iif$isInvisible()) {
                ci.cancel();
            }
        }
    }
}
