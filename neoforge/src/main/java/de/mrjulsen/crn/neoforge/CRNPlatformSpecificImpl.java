package de.mrjulsen.crn.neoforge;

import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.nio.file.Path;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModServerConfig;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class CRNPlatformSpecificImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            CreateRailwaysNavigatorNeoForge.getModContainer().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-client.toml");
        }
        CreateRailwaysNavigatorNeoForge.getModContainer().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-common.toml");
        CreateRailwaysNavigatorNeoForge.getModContainer().registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-server.toml");
    }

    public static Optional<String> getLastKnownPlayerName(UUID uuid) {
        return Optional.ofNullable(UsernameCache.getLastKnownUsername(uuid));
    }

    public static Map<UUID, String> getAllKnownPlayers() {
        return UsernameCache.getMap();
    }

    public static GlobalStation getStationFromBlockEntity(BlockEntity be) {
        if (!(be instanceof StationBlockEntity stationBe))
			return null;

        return stationBe.getStation();
    }

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        return contraption.getBlockEntityClientSide(localPos);
    }

    public static boolean trainCarriesFilteredCargo(Level level, FilterItemStack filter, Train train) {
        for (Carriage carriage : train.carriages) {
            if (carriage.storage == null)
                continue;

            IItemHandlerModifiable inv = carriage.storage.getAllItems();
            if (inv != null) {
                for (int slot = 0; slot < inv.getSlots(); slot++) {
                    ItemStack stack = inv.extractItem(slot, 1, true);
                    if (!stack.isEmpty() && filter.test(level, stack))
                        return true;
                }
            }

            IFluidHandler tank = carriage.storage.getFluids();
            if (tank != null) {
                for (int slot = 0; slot < tank.getTanks(); slot++) {
                    FluidStack drain = tank.drain(1, FluidAction.SIMULATE);
                    if (!drain.isEmpty() && filter.test(level, drain))
                        return true;
                }
            }
        }
        return false;
    }
}
