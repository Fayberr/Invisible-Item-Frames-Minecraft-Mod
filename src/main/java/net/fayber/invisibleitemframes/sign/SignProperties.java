package net.fayber.invisibleitemframes.sign;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

// shared blockstate property marking a sign as toggled invisible. lives here
// rather than inside the mixin so SignBlockMixin (adds it to every sign's
// state definition) and the interaction handler (reads/flips it) reference
// the exact same BooleanProperty instance.
public final class SignProperties {
    private SignProperties() {}

    // true once a player has toggled this sign hidden
    public static final BooleanProperty INVISIBLE = BooleanProperty.create("iif_invisible");
}
