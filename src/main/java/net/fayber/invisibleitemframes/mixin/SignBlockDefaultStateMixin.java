package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Registers the iif_invisible blockstate property directly on all four
// concrete sign block classes, and forces their real default state back to
// visible.
//
// Has to target the four concrete classes rather than the shared SignBlock
// superclass: each one overrides createBlockStateDefinition itself to add
// its own properties (rotation/waterlogged/attachment) without ever calling
// super.createBlockStateDefinition(builder), so an override mixed into
// SignBlock would never run. Each concrete class's override has an identical
// erased descriptor though, so one multi-target injection covers all four.
//
// Separately, BooleanProperty's "first" possible value is true, not false,
// so without the second injection below every sign would default to
// invisible. Each concrete sign block also shares an identical
// (WoodType, Properties) constructor, so the same multi-target trick forces
// the real default back to visible right after the vanilla constructor
// finishes registering its own default state.
@Mixin({StandingSignBlock.class, WallSignBlock.class, CeilingHangingSignBlock.class, WallHangingSignBlock.class})
public abstract class SignBlockDefaultStateMixin extends Block {

    protected SignBlockDefaultStateMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(
        method = "createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V",
        at = @At("TAIL")
    )
    private void invisibleItemFrames$addInvisibleProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(SignProperties.INVISIBLE);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V",
        at = @At("TAIL")
    )
    private void invisibleItemFrames$forceVisibleDefault(WoodType woodType, BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(this.defaultBlockState().setValue(SignProperties.INVISIBLE, false));
    }
}
