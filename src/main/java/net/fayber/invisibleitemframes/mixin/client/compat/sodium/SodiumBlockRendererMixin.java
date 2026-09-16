package net.fayber.invisibleitemframes.mixin.client.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.fayber.invisibleitemframes.sign.IInvisibleSign;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sodium-only counterpart to ModelBlockRendererMixin. This whole class is only
// ever loaded/transformed if Sodium is present - see SodiumCompatMixinPlugin,
// which gates the config this mixin lives in on FabricLoader.isModLoaded("sodium")
// so a non-Sodium user never touches these classes at all.
//
// Sodium replaces vanilla's ModelBlockRenderer#tesselateBlock entirely with its
// own chunk-mesh compile pipeline (confirmed by decompiling the actual installed
// jar: zero references to ModelBlockRenderer anywhere in Sodium's own code), so
// the vanilla mixin is a no-op under Sodium. renderModel(BlockStateModel,
// BlockState, BlockPos, BlockPos) on Sodium's own BlockRenderer is the exact
// equivalent call site - same role as tesselateBlock, just Sodium's version.
//
// `level` comes from the protected field on AbstractBlockRenderContext (a
// LevelSlice - Sodium's thread-safe, chunk-compile-local snapshot), populated
// by BlockRenderer#prepare(...) before renderModel runs, mirroring exactly how
// vanilla's tesselateBlock receives its own BlockAndTintGetter parameter.
@Mixin(BlockRenderer.class)
public abstract class SodiumBlockRendererMixin {

    // Declared protected (not private) on AbstractBlockRenderContext, BlockRenderer's
    // superclass - Shadow resolves against the real runtime hierarchy, not just the
    // exact @Mixin target class, so this is fine even though it's inherited.
    @Shadow
    protected BlockAndTintGetter level;

    @Inject(
            method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iif$skipSignModelWhenInvisible(
            BlockStateModel model,
            BlockState state,
            BlockPos pos,
            BlockPos origin,
            CallbackInfo ci
    ) {
        if (state.getBlock() instanceof SignBlock && this.level != null) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof IInvisibleSign sign && sign.iif$isInvisible()) {
                ci.cancel();
            }
        }
    }
}
