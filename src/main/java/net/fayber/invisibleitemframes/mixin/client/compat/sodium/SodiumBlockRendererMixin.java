package net.fayber.invisibleitemframes.mixin.client.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fayber.invisibleitemframes.sign.IInvisibleSign;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
// BlockRenderer's `level` field is declared on its superclass AbstractBlockRenderContext,
// which causes Mixin to throw InvalidMixinException when shadowed directly.
// Instead, we inject into prepare/release (both declared directly on BlockRenderer)
// to capture the LevelSlice (which implements BlockAndTintGetter) per chunk compilation pass.
@Mixin(BlockRenderer.class)
public abstract class SodiumBlockRendererMixin {

    @Unique
    private BlockAndTintGetter iif$level;

    @Inject(method = "prepare", at = @At("RETURN"))
    private void iif$captureLevelSlice(
            ChunkBuildBuffers buffers,
            LevelSlice slice,
            TranslucentGeometryCollector collector,
            CallbackInfo ci
    ) {
        this.iif$level = slice;
    }

    @Inject(method = "release", at = @At("RETURN"))
    private void iif$releaseLevelSlice(CallbackInfo ci) {
        this.iif$level = null;
    }

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
        if (state.getBlock() instanceof SignBlock && this.iif$level != null) {
            BlockEntity be = this.iif$level.getBlockEntity(pos);
            if (be instanceof IInvisibleSign sign && sign.iif$isInvisible()) {
                ci.cancel();
            }
        }
    }
}

