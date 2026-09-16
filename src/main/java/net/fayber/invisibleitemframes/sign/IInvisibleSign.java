package net.fayber.invisibleitemframes.sign;

// Duck-typing interface mixed onto SignBlockEntity to store and query
// whether a sign has been toggled invisible without modifying BlockState.
public interface IInvisibleSign {
    boolean iif$isInvisible();
    void iif$setInvisible(boolean invisible);
}
