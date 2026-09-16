package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.sign.IInvisibleSign;
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
        this.iif$invisible = input.getBooleanOr("iif_invisible", false);
    }
}
