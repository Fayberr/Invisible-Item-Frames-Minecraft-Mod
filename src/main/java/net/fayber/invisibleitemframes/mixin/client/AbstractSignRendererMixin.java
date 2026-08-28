package net.fayber.invisibleitemframes.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * In 26.1 the sign's visible geometry (post and board) is NOT part of the
 * chunk mesh anymore - the vanilla sign block model has no elements and the
 * whole sign, model and text, is drawn by the block entity renderer
 * ({@code AbstractSignRenderer#submitSignWithText}). Hiding the chunk mesh
 * via the block's render shape (see {@code SignBlockMixin}) is therefore a
 * no-op on this version, and Sodium does not help either: it meshes whatever
 * the blockstate model set gives it, which is nothing here.
 *
 * <p>So the invisibility is applied at the actual render source: this wraps
 * the {@code submitSign} call inside {@code submitSignWithText} and skips it
 * while the sign's {@code iif_invisible} blockstate property is set. Only
 * the sign's wooden model is skipped; {@code submitSignText} still runs, so
 * the text keeps rendering, mirroring how a hidden item frame keeps
 * rendering its held item.
 */
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {

    @WrapOperation(
            method = "submitSignWithText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSign(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
            )
    )
    private void iif$skipModelWhenInvisible(AbstractSignRenderer instance,
                                            PoseStack poseStack, int packedLight, WoodType woodType,
                                            Model.Simple model,
                                            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                                            SubmitNodeCollector collector,
                                            Operation<Void> original,
                                            @Local(argsOnly = true) SignRenderState state) {
        if (isInvisible(state.blockPos)) {
            return;
        }
        original.call(instance, poseStack, packedLight, woodType, model, crumblingOverlay, collector);
    }

    private static boolean isInvisible(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        BlockState blockState = minecraft.level.getBlockState(pos);
        return blockState.hasProperty(SignProperties.INVISIBLE)
                && blockState.getValue(SignProperties.INVISIBLE);
    }
}
