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

/**
 * Registers the {@code iif_invisible} blockstate property directly on all
 * four concrete sign block classes, and forces their real default state back
 * to visible.
 *
 * <p>This has to target the four concrete classes rather than the shared
 * {@link net.minecraft.world.level.block.SignBlock} superclass: each of them
 * overrides {@code createBlockStateDefinition} itself to add its own
 * properties (rotation/waterlogged/attachment) but none of them call
 * {@code super.createBlockStateDefinition(builder)} while doing it, so an
 * override mixed into {@code SignBlock} would never actually run. Each
 * concrete class's own override has an identical erased descriptor, so one
 * multi-target injection covers all four.
 *
 * <p>Separately, {@link net.minecraft.world.level.block.state.properties.BooleanProperty}'s
 * own "first" possible value is {@code true}, not {@code false}, so without
 * the second injection here every sign's default state would start out
 * marked invisible. Each concrete sign block also has an identical
 * {@code (WoodType, Properties)} constructor, so the same multi-target
 * approach forces the real default back to visible right after the vanilla
 * constructor finishes registering its own default state.
 */
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
