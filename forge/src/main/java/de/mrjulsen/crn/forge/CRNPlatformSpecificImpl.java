package de.mrjulsen.crn.forge;

import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.UsernameCache;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModServerConfig;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public class CRNPlatformSpecificImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-client.toml");
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC, CreateRailwaysNavigator.MOD_ID + "-server.toml");
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
 