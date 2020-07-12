package org.teacon.kbxhl;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class KBXHLScoreManager {

    static final float MAX_SCORE = 651.0F;

    private final Map<ServerPlayerEntity, Integer> scores = new WeakHashMap<>();
    private final Map<ServerPlayerEntity, Instant> startTime = new WeakHashMap<>();
    private final Map<ServerPlayerEntity, ServerBossInfo> bossBars = new WeakHashMap<>();
    private final Map<ServerPlayerEntity, Integer> pendingScores = new WeakHashMap<>();

    public void add(ServerPlayerEntity player, int score) {
        this.pendingScores.compute(player, (k, v) -> v == null ? score : v + score);
        this.bossBars.computeIfAbsent(player, k -> {
           ServerBossInfo bossBar = new ServerBossInfo(new StringTextComponent("狂扁小海螺")
                   .applyTextStyle(TextFormatting.GOLD)
                   .applyTextStyle(TextFormatting.BOLD),
                   BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
           bossBar.setPercent(0F);
           bossBar.addPlayer(k);
           return bossBar;
        });
        this.startTime.computeIfAbsent(player, k -> Instant.now());
    }

    public Duration finish(ServerPlayerEntity player) {
        scores.remove(player);
        pendingScores.remove(player);
        Optional.ofNullable(bossBars.remove(player)).ifPresent(v -> v.removePlayer(player));
        return Optional.ofNullable(startTime.remove(player)).map(v -> Duration.between(v, Instant.now())).orElse(Duration.ZERO);
    }

    public int getScoreFor(ServerPlayerEntity player) {
        return this.scores.getOrDefault(player, 0);
    }

    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            final ArrayList<ServerPlayerEntity> gameOverPlayers = new ArrayList<>();
            this.pendingScores.forEach((player, score) -> {
                int newScore = this.scores.getOrDefault(player, 0) + score;
                this.bossBars.computeIfPresent(player, (k, v) -> {
                    v.setPercent(newScore / MAX_SCORE);
                    return v;
                });
                this.scores.put(player, newScore);
                if (newScore >= MAX_SCORE) {
                    gameOverPlayers.add(player);
                }
            });
            gameOverPlayers.forEach(player -> MinecraftForge.EVENT_BUS.post(new KBXHLEvent.Stop(player)));
            this.pendingScores.clear();
        }
    }
}
