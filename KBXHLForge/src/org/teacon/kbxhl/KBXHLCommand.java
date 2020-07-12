package org.teacon.kbxhl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.function.Predicate;

public final class KBXHLCommand {

    public KBXHLCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("kbxhl")
                .then(Commands.literal("start").requires(check("kbxhl.command.start")).executes(KBXHLCommand::gameStart))
                .then(Commands.literal("stop").requires(check("kbxhl.command.stop")).executes(KBXHLCommand::gameStop))
                .then(Commands.literal("top").requires(check("kbxhl.command.top")).executes(KBXHLCommand::gameRanking))
                .executes(KBXHLCommand::about));
    }

    private static Predicate<CommandSource> check(final String permission) {
        return source -> {
            try {
                return PermissionAPI.hasPermission(source.asPlayer(), permission);
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static int about(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("KuangBianXiaoHaiLuo v" + KBXHLForge.version), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int gameStart(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = context.getSource().asPlayer();
        if (KBXHLForge.theMod.theGame.currentPlayers.contains(player.getUniqueID())) {
            // Player has already started game, command fails.
            context.getSource().sendFeedback(new StringTextComponent("你已经开始游戏了"), false);
            return -1;
        } else {
            MinecraftForge.EVENT_BUS.post(new KBXHLEvent.Start(player));
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int gameStop(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = context.getSource().asPlayer();
        if (KBXHLForge.theMod.theGame.currentPlayers.contains(player.getUniqueID())) {
            MinecraftForge.EVENT_BUS.post(new KBXHLEvent.Stop(player));
            return Command.SINGLE_SUCCESS;
        } else {
            // Player has not started game yet, command fails.
            context.getSource().sendFeedback(new StringTextComponent("你没有在一场游戏中"), false);
            return -1;
        }
    }

    private static int gameRanking(CommandContext<CommandSource> context) {
        context.getSource().sendFeedback(new StringTextComponent("不好意思，排行榜还没写完"), false);
        return Command.SINGLE_SUCCESS;
    }

}
