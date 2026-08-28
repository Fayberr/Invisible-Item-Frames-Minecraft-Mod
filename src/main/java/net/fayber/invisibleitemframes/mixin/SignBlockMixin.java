package net.fayber.invisibleitemframes.mixin;

import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes signs render nothing while their {@code iif_invisible} blockstate
 * property (added by {@link SignBlockDefaultStateMixin} onto the four
 * concrete sign subclasses) is set, the same trick vanilla uses for
 * {@code minecraft:end_portal}.
 *
 * <p>Mixing {@code getRenderShape} in at {@link SignBlock} works because
 * none of the four concrete subclasses (standing, wall, and both hanging
 * variants) override it themselves, so ordinary virtual dispatch reaches
 * this override by inheritance.
 *
 * <p>Note this is NOT true of {@code createBlockStateDefinition}: each
 * concrete subclass overrides that one directly without ever calling
 * {@code super.createBlockStateDefinition(builder)}, so an override added
 * here would never run. That property registration lives directly on the
 * four subclasses instead, see {@link SignBlockDefaultStateMixin}.
 *
 * <p>Only the block's baked model / chunk mesh is hidden. The
 * {@code SignBlockEntityRenderer} is looked up by block entity type, not by
 * render shape, so the sign's text keeps rendering exactly like the item
 * frame's held item stays visible while the frame itself is hidden.
 */
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
