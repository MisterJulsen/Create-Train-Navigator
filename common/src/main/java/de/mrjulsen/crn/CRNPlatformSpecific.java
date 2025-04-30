package de.mrjulsen.crn;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;

public abstract class CRNPlatformSpecific {
    
    @ExpectPlatform
    public static Path getConfigDirectory() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static MinecraftServer getServer() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerConfig() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Optional<String> getLastKnownPlayerName(UUID uuid) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Map<UUID, String> getAllKnownPlayers() {
        throw new AssertionError();
    }
}
