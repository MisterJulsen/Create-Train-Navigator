package de.mrjulsen.crn;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.nio.file.Path;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.tterrag.registrate.util.entry.RegistryEntry;

import de.mrjulsen.crn.block.display.AdvancedDisplaySource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public static GlobalStation getStationFromBlockEntity(BlockEntity be) {
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

    @ExpectPlatform
    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static final RegistryEntry<AdvancedDisplaySource> registerDisplaySource() {
        throw new AssertionError();
    }
}
