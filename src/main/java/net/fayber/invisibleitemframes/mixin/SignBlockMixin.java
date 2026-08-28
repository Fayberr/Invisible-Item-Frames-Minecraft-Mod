package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

// Makes signs render nothing while iif_invisible is set - same trick vanilla
// uses for end_portal. Text keeps rendering since SignBlockEntityRenderer is
// looked up by block entity type, not render shape (only the baked model /
// chunk mesh gets hidden, same idea as the item frame's held item staying
// visible while the frame itself is hidden).
//
// This can target SignBlock directly because none of the four concrete
// subclasses (standing/wall/both hanging variants) override getRenderShape,
// so normal virtual dispatch reaches it by inheritance. That's NOT true for
// createBlockStateDefinition though - each subclass overrides that itself
// without calling super, so an override here would never run. That's why
// the property registration lives on the subclasses instead, see
// SignBlockDefaultStateMixin.
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
