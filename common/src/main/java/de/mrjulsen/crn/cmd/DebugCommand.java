package de.mrjulsen.crn.cmd;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.debug.BackendDebugOverlay;
import de.mrjulsen.crn.core.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.commands.SharedSuggestionProvider;

public class DebugCommand {

    private static final String CMD_NAME = CreateRailwaysNavigator.MOD_ID;
    
    private static final String SUB_DEBUG = "debug";
    private static final String SUB_DISCORD = "discord";
    private static final String SUB_GITHUB = "github";

    private static final String SUB_BACKEND_DEBUG_OVERLAY = "backendDebugOverlay";
    private static final String SUB_BACKEND_RESET = "resetBackendTimetables";
    private static final String SUB_BACKEND_HARD_RESET = "hardResetBackendData";
    private static final String SUB_CLEAR_DEPARTURE_HISTORY = "clearDepartureHistory";
    private static final String SUB_BACKEND_DATA_DUMP = "backendDataDump";

    private static final String ARG_TRAIN = "train";

    private static final SuggestionProvider<CommandSourceStack> TRAIN_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(TrainManager.getInstance().getAllTrains().stream().map(TrackedTrain::getTrainName), builder);


    @SuppressWarnings("all")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandSelection selection) {        
        
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(CMD_NAME)
            .then(Commands.literal(SUB_DEBUG)
                .requires(x -> x.hasPermission(3))
                .then(Commands.literal(SUB_BACKEND_DEBUG_OVERLAY)
                    .executes(x -> showBackendDebugOverlay(x.getSource()))
                )
                .then(Commands.literal(SUB_BACKEND_RESET)
                    .executes(x -> resetBackend(x.getSource()))
                    .then(Commands.argument(ARG_TRAIN, StringArgumentType.greedyString())
                        .suggests(TRAIN_SUGGESTIONS)
                        .executes(x -> resetBackendTrain(x.getSource(), StringArgumentType.getString(x, ARG_TRAIN)))
                    )
                )
                .then(Commands.literal(SUB_BACKEND_HARD_RESET)
                    .executes(x -> hardResetBackend(x.getSource()))
                    .then(Commands.argument(ARG_TRAIN, StringArgumentType.greedyString())
                        .suggests(TRAIN_SUGGESTIONS)
                        .executes(x -> hardResetBackendTrain(x.getSource(), StringArgumentType.getString(x, ARG_TRAIN)))
                    )
                )
                .then(Commands.literal(SUB_CLEAR_DEPARTURE_HISTORY)
                    .executes(x -> clearDepartureHistory(x.getSource()))
                )
                .then(Commands.literal(SUB_BACKEND_DATA_DUMP)
                    .executes(x -> toggleBackendDataDump(x.getSource()))
                )
                .then(NavigatorDebugCommand.navigate())
                .then(NavigatorDebugCommand.index())
                .then(NavigatorDebugCommand.dump())
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
        cmd.sendSuccess(() -> TextUtils.text("Redirecting to the discord server..."), false);
        ClientWrapper.openUrl(CreateRailwaysNavigator.DISCORD);
        return 1;
    }

    private static int github(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("Redirecting to the github repository..."), false);
        ClientWrapper.openUrl(CreateRailwaysNavigator.GITHUB);
        return 1;
    }

    private static int showBackendDebugOverlay(CommandSourceStack cmd) throws CommandSyntaxException {
        if (Platform.getEnvironment() == Env.CLIENT) {
            cmd.sendSuccess(() -> TextUtils.text("Visibility of the backend debug overlay has been toggled."), false);
            BackendDebugOverlay.toggle();
            return 1;
        } else {
            cmd.sendFailure(TextUtils.text("Cannot open the backend debug overlay in multiplayer."));
        }
        return 0;
    }

    private static int resetBackend(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("All backend timetables have been reset to the current real-time data."), false);
        TrainManager.getInstance().softResetAll();
        return 1;
    }

    private static int resetBackendTrain(CommandSourceStack cmd, String trainName) throws CommandSyntaxException {
        Optional<TrackedTrain> train = findTrain(trainName);
        if (train.isEmpty()) {
            cmd.sendFailure(TextUtils.text("No tracked train named '" + trainName + "'."));
            return 0;
        }
        TrainManager.getInstance().softReset(train.get().getTrainId());
        cmd.sendSuccess(() -> TextUtils.text("The timetable of '" + train.get().getTrainName() + "' has been reset to the current real-time data."), false);
        return 1;
    }

    private static int hardResetBackend(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("All learned backend data has been deleted."), false);
        TrainManager.getInstance().hardResetAll();
        return 1;
    }

    private static int hardResetBackendTrain(CommandSourceStack cmd, String trainName) throws CommandSyntaxException {
        Optional<TrackedTrain> train = findTrain(trainName);
        if (train.isEmpty()) {
            cmd.sendFailure(TextUtils.text("No tracked train named '" + trainName + "'."));
            return 0;
        }
        TrainManager.getInstance().hardReset(train.get().getTrainId());
        cmd.sendSuccess(() -> TextUtils.text("All learned data of '" + train.get().getTrainName() + "' has been deleted."), false);
        return 1;
    }

    private static Optional<TrackedTrain> findTrain(String trainName) {
        return TrainManager.getInstance().getAllTrains().stream()
            .filter(x -> x.getTrainName().equalsIgnoreCase(trainName))
            .findFirst();
    }

    private static int clearDepartureHistory(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("The departure history has been deleted."), false);
        TrainManager.getInstance().getDepartureLog().clear();
        return 1;
    }

    private static int toggleBackendDataDump(CommandSourceStack cmd) throws CommandSyntaxException {
        if (BackendDiagnosticsRecorder.isActive()) {
            BackendDiagnosticsRecorder.stop();
            cmd.sendSuccess(() -> TextUtils.text("Backend diagnostics recording stopped."), false);
            return 1;
        }

        return BackendDiagnosticsRecorder.start()
            .map(file -> {
                cmd.sendSuccess(() -> TextUtils.text("Backend diagnostics recording started: " + file), false);
                return 1;
            })
            .orElseGet(() -> {
                cmd.sendFailure(TextUtils.text("Unable to start backend diagnostics recording (backend inactive?)."));
                return 0;
            });
    }


}