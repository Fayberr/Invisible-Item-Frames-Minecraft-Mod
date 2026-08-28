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

// In 26.1 the sign's visible geometry (post and board) isn't part of the
// chunk mesh anymore - the vanilla sign block model has no elements, and the
// whole sign (model + text) is drawn by the block entity renderer
// (AbstractSignRenderer#submitSignWithText). So hiding the chunk mesh via
// the block's render shape (see SignBlockMixin) is a no-op on this version,
// and Sodium doesn't help either since it just meshes whatever the
// blockstate model gives it, which is nothing here.
//
// So we hook the actual render source instead: wraps the submitSign call
// inside submitSignWithText and skips it while iif_invisible is set. Only
// the wooden model is skipped - submitSignText still runs, so the text keeps
// rendering, same as how a hidden item frame keeps rendering its held item.
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
