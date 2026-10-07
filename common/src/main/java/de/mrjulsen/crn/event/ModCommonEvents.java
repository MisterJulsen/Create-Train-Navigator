package de.mrjulsen.crn.event;

import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.AdvancedDisplayTarget;
import de.mrjulsen.crn.cmd.DebugCommand;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.web.WebServer;
import de.mrjulsen.mcdragonlib.internal.ClientWrapper;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class ModCommonEvents {

    private static MinecraftServer currentServer;

    public static void init() {

        LifecycleEvent.SETUP.register(() -> {
            CreateRailwaysNavigator.LOGGER.info("Welcome to the CREATE RAILWAYS NAVIGATOR mod by MRJULSEN.");
        });

        LifecycleEvent.SERVER_STARTED.register((server) -> {
            currentServer = server;
            AdvancedDisplayTarget.start();
            WebServer.start();
        });

        LifecycleEvent.SERVER_STOPPING.register((server) -> {
            GlobalSettings.clearInstance();
            AdvancedDisplayTarget.stop();
            WebServer.stop();
        });

        LifecycleEvent.SERVER_STOPPED.register((server) -> {
            currentServer = null;
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, context, selection) -> {
            DebugCommand.register(dispatcher, selection);
        });

        LifecycleEvent.SERVER_LEVEL_SAVE.register((server) -> {
            if (!getCurrentServer().isPresent()) return;
            if (server != getCurrentServer().get().overworld()) return;

            if (GlobalSettings.hasInstance()) GlobalSettings.getInstance().save();
        });
    }

    public static boolean hasServer() {
        return currentServer != null;
    }

    public static Optional<MinecraftServer> getCurrentServer() {
        return Optional.ofNullable(currentServer);
    }

    public static Level getPhysicalLevel() {
        return hasServer() ? getCurrentServer().get().overworld() : ClientWrapper.getClientLevel();
    }
}
