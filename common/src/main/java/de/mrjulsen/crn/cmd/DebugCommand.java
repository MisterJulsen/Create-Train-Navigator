package de.mrjulsen.crn.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.schedule.TrainJourney;
import de.mrjulsen.crn.backend.TrainManager;
import de.mrjulsen.crn.backend.debug.BackendDebugOverlay;
import de.mrjulsen.crn.backend.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.debug.DebugOverlay;
import de.mrjulsen.crn.network.packets.pain.ShowTrainDebugScreenPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.network.chat.MutableComponent;

public class DebugCommand {

    private static final String CMD_NAME = CreateRailwaysNavigator.MOD_ID;
    
    private static final String SUB_DEBUG = "debug";
    private static final String SUB_DISCORD = "discord";
    private static final String SUB_GITHUB = "github";

    private static final String SUB_TRAIN_SCHEDULES = "trainSchedules";

    private static final String SUB_RESET = "resetTrainPredictions";
    private static final String SUB_HARD_RESET = "hardResetTrainPredictions";
    private static final String SUB_TRAIN_DEBUG_OVERLAY = "trainDebugOverlay";
    private static final String SUB_BACKEND_DEBUG_OVERLAY = "backendDebugOverlay";
    private static final String SUB_BACKEND_RESET = "resetBackendTimetables";
    private static final String SUB_BACKEND_HARD_RESET = "hardResetBackendData";
    private static final String SUB_TRAIN_OVERVIEW = "trainOverview";
    private static final String SUB_CLEAR_DEPARTURE_HISTORY = "clearDepartureHistory";
    private static final String SUB_BACKEND_DATA_DUMP = "backendDataDump";
    
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

                .then(Commands.literal(SUB_TRAIN_SCHEDULES)
                        .executes(x -> showTrainSchedules(x.getSource()))
                )
                .then(Commands.literal(SUB_TRAIN_DEBUG_OVERLAY)
                    .executes(x -> showTrainObservationOverlay(x.getSource()))
                )
                .then(Commands.literal(SUB_BACKEND_DEBUG_OVERLAY)
                    .executes(x -> showBackendDebugOverlay(x.getSource()))
                )
                .then(Commands.literal(SUB_BACKEND_RESET)
                    .executes(x -> resetBackend(x.getSource()))
                )
                .then(Commands.literal(SUB_BACKEND_HARD_RESET)
                    .executes(x -> hardResetBackend(x.getSource()))
                )
                .then(Commands.literal(SUB_TRAIN_OVERVIEW)
                    .executes(x -> showTrainDebugScreen(x.getSource()))
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
        Util.getPlatform().openUri(CreateRailwaysNavigator.DISCORD);
        return 1;
    }

    private static int github(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("Redirecting to the github repository..."), false);
        Util.getPlatform().openUri(CreateRailwaysNavigator.GITHUB);
        return 1;
    }

    private static int hardReset(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("All train predictions have been deleted."), false);
        TrainListener.resetTrainData();
        return 1;
    }

    private static int reset(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("All train predictions have been reset."), false);
        TrainListener.getAllTrainData().forEach(x -> x.softResetPredictions());
        return 1;
    }

    private static int showTrainObservationOverlay(CommandSourceStack cmd) throws CommandSyntaxException {
        if (Platform.getEnvironment() == Env.CLIENT) {            
            cmd.sendSuccess(() -> TextUtils.text("Visibility of the train debug overlay has been toggled."), false);
            DebugOverlay.toggle();
            return 1;
        } else {            
            cmd.sendFailure(TextUtils.text("Cannot open the train debug overlay in multiplayer."));  
        }
        return 0;
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

    private static int hardResetBackend(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.text("All learned backend data has been deleted."), false);
        TrainManager.getInstance().hardResetAll();
        return 1;
    }

    private static int showTrainDebugScreen(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> TextUtils.empty(), false);
        ModNetworkManager.SHOW_TRAIN_DEBUG_SCREEN.send(NetworkDirection.toPlayer(cmd.getPlayerOrException()), new ShowTrainDebugScreenPacketData());
        return 1;
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


    private static int showTrainSchedules(CommandSourceStack cmd) throws CommandSyntaxException {
        cmd.sendSuccess(() -> {
            MutableComponent text = TextUtils.empty();
            try {
            } catch (Throwable t) {
                System.err.println(t);
                t.printStackTrace();
            }
            return text;
        }, false);
        TrainManager.getInstance().softResetAll();
        return 1;
    }
}