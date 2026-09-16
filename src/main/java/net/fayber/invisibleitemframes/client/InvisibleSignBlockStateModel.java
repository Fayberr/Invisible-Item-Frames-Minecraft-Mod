package net.fayber.invisibleitemframes.client;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fayber.invisibleitemframes.sign.IInvisibleSign;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Fabric Rendering API wrapper for sign BlockStateModels.
 * Intercepts chunk mesh quad emission for all FRAPI-compliant renderers (Vanilla/Indigo, Sodium, Iris).
 * When the sign is marked invisible via IInvisibleSign, emits zero quads,
 * omitting the wooden geometry while preserving text/wax rendering.
 */
public class InvisibleSignBlockStateModel extends WrapperBlockStateModel {

    public InvisibleSignBlockStateModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    private static boolean isInvisible(BlockAndTintGetter level, BlockPos pos) {
        if (level != null && pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IInvisibleSign sign && sign.iif$isInvisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest
    ) {
        if (isInvisible(level, pos)) {
            return;
        }
        super.emitQuads(emitter, level, pos, state, random, cullTest);
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        if (isInvisible(level, pos)) {
            return Boolean.FALSE;
        }
        return super.createGeometryKey(level, pos, state, random);
    }
}
