package org.teacon.kbxhl;

import net.minecraft.nbt.ListNBT;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holder of configurable variables and some persistent data.
 */
public final class KBXHLConfig {
    Map<UUID, ListNBT> savedInv = new HashMap<>();
}
