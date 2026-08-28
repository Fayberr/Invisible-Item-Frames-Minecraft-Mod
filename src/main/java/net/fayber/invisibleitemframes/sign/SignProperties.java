package net.fayber.invisibleitemframes.sign;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Shared blockstate property that marks a sign as toggled invisible by this
// mod. Declared here (rather than inside the mixin) so both
// net.fayber.invisibleitemframes.mixin.SignBlockMixin, which adds it
// to every sign's state definition, and the interaction handler, which reads
// and flips it, reference the exact same BooleanProperty instance.
public final class SignProperties {
    private SignProperties() {}

    // True when a player has shift right-clicked this sign with an empty hand to hide it.
    public static final BooleanProperty INVISIBLE = BooleanProperty.create("iif_invisible");
}
