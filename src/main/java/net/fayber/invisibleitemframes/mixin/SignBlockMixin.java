package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

// Makes signs render nothing while their iif_invisible blockstate property
// (added by SignBlockDefaultStateMixin onto the four concrete sign
// subclasses) is set - same trick vanilla uses for minecraft:end_portal.
//
// Mixing getRenderShape in at SignBlock works because none of the four
// concrete subclasses (standing, wall, both hanging variants) override it
// themselves, so ordinary virtual dispatch reaches this override by
// inheritance.
//
// That's NOT true of createBlockStateDefinition though: each concrete
// subclass overrides that directly without calling
// super.createBlockStateDefinition(builder), so an override added here would
// never run. That property registration lives on the four subclasses
// instead - see SignBlockDefaultStateMixin.
//
// Only the block's baked model / chunk mesh gets hidden this way. The block
// entity renderer is looked up by block entity type, not render shape, so
// the sign's text keeps rendering - same as how a hidden item frame's held
// item stays visible.
@Mixin(SignBlock.class)
public abstract class SignBlockMixin extends BaseEntityBlock {

    protected SignBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (state.getValue(SignProperties.INVISIBLE)) {
            return RenderShape.INVISIBLE;
        }
        return super.getRenderShape(state);
    }
}
