package net.fayber.invisibleitemframes.mixin.client.compat.sodium;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

// Gates the whole invisibleitemframes-sodium.mixins.json config on Sodium
// actually being installed. Without this, Mixin would try to load and
// transform net.caffeinemc.mods.sodium....BlockRenderer on every user's game,
// and crash at startup for anyone who doesn't have Sodium - the class simply
// wouldn't exist to attach to.
public final class SodiumCompatMixinPlugin implements IMixinConfigPlugin {

    private boolean sodiumLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.sodiumLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
