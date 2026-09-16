package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.client.InvisibleItemFramesClient;
import net.fayber.invisibleitemframes.sign.IInvisibleSign;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Stores the invisible toggle flag directly on the block entity instead of
// polluting the global BlockState registry with extra states.
//
// saveAdditional is called by both disk persistence and getUpdateTag (for
// ClientboundBlockEntityDataPacket), so injecting into saveAdditional and
// loadAdditional keeps the client automatically synchronized.
//
// That sync alone isn't enough to make the client actually redraw, though.
// In 26.2 the sign's wooden model is baked into the chunk mesh (see
// ModelBlockRendererMixin), so unlike item frames (an entity, re-rendered
// live from its data every frame), a baked mesh only updates when something
// explicitly marks its section dirty. SignInteractionHandler still syncs via
// level.sendBlockUpdated(pos, state, state, 3), but old and new BlockState
// are the exact same interned instance - the property that used to make them
// different lived on BlockState and was deliberately removed from there (see
// the trapdoor/registry-shift fix) - so the client's block-update path sees
// no change and never re-meshes the section.
//
// loadAdditional is also where BlockEntity#loadWithComponents lands when a
// ClientboundBlockEntityDataPacket is applied client-side, so it's the one
// place that reliably fires exactly when the flag actually flips for a given
// client. On a real flip we bypass the BlockState-diff path entirely and
// force that section (and neighbors, so mesh seams don't tear) to rebuild
// directly - via InvisibleItemFramesClient, and only from this
// level.isClientSide() branch, so a dedicated server never resolves any
// client rendering class.
@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin implements IInvisibleSign {

    @Unique
    private boolean iif$invisible;

    @Override
    public boolean iif$isInvisible() {
        return this.iif$invisible;
    }

    @Override
    public void iif$setInvisible(boolean invisible) {
        this.iif$invisible = invisible;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void iif$saveAdditional(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("iif_invisible", this.iif$invisible);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void iif$loadAdditional(ValueInput input, CallbackInfo ci) {
        boolean newInvisible = input.getBooleanOr("iif_invisible", false);
        boolean changed = newInvisible != this.iif$invisible;
        this.iif$invisible = newInvisible;
        if (!changed) {
            return;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null && level.isClientSide()) {
            InvisibleItemFramesClient.signVisibilityChanged(self.getBlockPos());
        }
    }
}
