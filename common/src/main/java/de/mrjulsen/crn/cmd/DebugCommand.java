package de.mrjulsen.crn.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.debug.DebugOverlay;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;

public class DebugCommand {

    private static final String CMD_NAME = CreateRailwaysNavigator.MOD_ID;
    
    private static final String SUB_DEBUG = "debug";
    private static final String SUB_DISCORD = "discord";
    private static final String SUB_GITHUB = "github";

    private static final String SUB_RESET = "resetTrainPredictions";
    private static final String SUB_HARD_RESET = "hardResetTrainPredictions";
    private static final String SUB_TRAIN_DEBUG_OVERLAY = "trainDebugOverlay";
    private static final String SUB_TRAIN_OVERVIEW = "trainOverview";
    private static final String SUB_TEST = "test";
    
    @SuppressWarnings("all")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandSelection selection) {        
        
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(CMD_NAME)
            .then(Commands.literal(SUB_DEBUG)
                .requires(x -> x.hasPermission(3))
                .then(Commands.literal(SUB_HARD_RESET)
                    .executes(x -> hardReset(x.getSource()))
                )
                .then(Commands.literal(SUB_RESET)
                    .executes(x -> reset(x.getSource()))
                )
                .then(Commands.literal(SUB_TRAIN_DEBUG_OVERLAY)
                    .executes(x -> showTrainObservationOverlay(x.getSource()))
                )
                .then(Commands.literal(SUB_TRAIN_OVERVIEW)
                    .executes(x -> showTrainDebugScreen(x.getSource()))
                )
                .then(Commands.literal(SUB_TEST)
                    .executes(x -> printAllSignals(x.getSource()))
                )
            )
            .then(Commands.literal(SUB_DISCORD)
                .executes(x -> discord(x.getSource()))
            )
            .then(Commands.literal(SUB_GITHUB)
                .executes(x -> github(x.getSource()))
            )
        ;

        dispatcher.register(builder);
    }

    private static int discord(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.text("Redirecting to the discord server..."), false);
        Util.getPlatform().openUri(CreateRailwaysNavigator.DISCORD);
        return 1;
    }

    private static int github(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.text("Redirecting to the github repository..."), false);
        Util.getPlatform().openUri(CreateRailwaysNavigator.GITHUB);
        return 1;
    }

    private static int hardReset(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.text("All train predictions have been deleted."), false);
        TrainListener.data.clear();
        TrainListener.data.values().forEach(x -> x.hardResetPredictions());
        return 1;
    }

    private static int reset(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.text("All train predictions have been reset."), false);
        TrainListener.data.values().forEach(x -> x.resetPredictions());
        return 1;
    }

    private static int showTrainObservationOverlay(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.text("Visibility of the train debug overlay has been toggled."), false);
        DebugOverlay.toggle();
        return 1;
    }

    private static int showTrainDebugScreen(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.empty(), false);
        DataAccessor.getFromClient(cmd.getPlayerOrException(), null, ModAccessorTypes.SHOW_TRAIN_DEBUG_SCREEN, $ -> {});
        return 1;
    }

    private static int printAllSignals(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(TextUtils.empty(), false);
        return 1;
    }
}