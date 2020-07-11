package org.teacon.kbxhl;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Event;

public abstract class KBXHLEvent extends Event {

    private final ServerPlayerEntity player;

    protected KBXHLEvent(ServerPlayerEntity player) {
        this.player = player;
    }

    public static final class Start extends KBXHLEvent {
        public Start(ServerPlayerEntity player) {
            super(player);
        }
    }

    public static final class Stop extends KBXHLEvent {
        public Stop(ServerPlayerEntity player) {
            super(player);
        }
    }
}
