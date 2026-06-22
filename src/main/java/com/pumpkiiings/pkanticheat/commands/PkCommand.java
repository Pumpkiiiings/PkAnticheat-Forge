package com.pumpkiiings.pkanticheat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pumpkiiings.pkanticheat.AlertManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class PkCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pk")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> clearViolations(context, StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("history")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> showHistory(context, StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("ping")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> showPing(context, StringArgumentType.getString(context, "player")))))
        );
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context, String playerName) {
        return context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
    }

    private static int clearViolations(CommandContext<CommandSourceStack> context, String playerName) {
        ServerPlayer target = getPlayer(context, playerName);
        if (target == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

        AlertManager.clearViolations(target);
        context.getSource().sendSuccess(() -> Component.literal("Cleared violation history for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int showHistory(CommandContext<CommandSourceStack> context, String playerName) {
        ServerPlayer target = getPlayer(context, playerName);
        if (target == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

        Map<String, Integer> violations = AlertManager.getViolations(target);
        if (violations.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has no violations."), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.literal("Violation history for " + target.getName().getString() + ":"), false);
        for (Map.Entry<String, Integer> entry : violations.entrySet()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + entry.getKey() + ": " + entry.getValue() + " VL"), false);
        }

        return 1;
    }

    private static int showPing(CommandContext<CommandSourceStack> context, String playerName) {
        ServerPlayer target = getPlayer(context, playerName);
        if (target == null) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }

        int ping = target.latency;
        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "'s ping: " + ping + "ms"), false);
        return 1;
    }
}
