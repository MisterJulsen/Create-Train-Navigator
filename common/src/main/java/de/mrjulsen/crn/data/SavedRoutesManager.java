package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class SavedRoutesManager {
    private static final LinkedHashSet<ClientRoute> savedRoutes = new LinkedHashSet<>();
    private static MutableSingle<Boolean> isSynchronizing = new MutableSingle<Boolean>(false);

    public static void saveRoute(ClientRoute route) {
        route.addListener();
        savedRoutes.add(route);
    }

    public static void removeRoute(ClientRoute route) {
        route.close();
        savedRoutes.remove(route);
    }

    public static void removeAllRoutes() {
        savedRoutes.forEach(x -> x.closeAll());
        savedRoutes.clear();
    }

    public static boolean isSaved(ClientRoute route) {
        return savedRoutes.contains(route);
    }

    public static List<ClientRoute> getAllSavedRoutes() {
        return new ArrayList<>(savedRoutes);
    }

    @SuppressWarnings("resource")
    public static void push(boolean clear, Runnable andThen) {
        isSynchronizing.setFirst(true);
        DataAccessor.getFromServer(Minecraft.getInstance().player.getUUID(), ModAccessorTypes.GET_USER_SETTINGS, (settings) -> {
            Set<CompoundTag> currentValue = clear ? new HashSet<>() : settings.savedRoutes.getValue();
            currentValue.addAll(savedRoutes.stream().map(x -> x.toNbt()).toList());
            settings.savedRoutes.setValue(currentValue);
            settings.clientSave(() -> {
                isSynchronizing.setFirst(false);
                DLUtils.doIfNotNull(andThen, Runnable::run);
            });
        });
    }

    @SuppressWarnings("resource")
    public static void pull(boolean clear, Runnable andThen) {
        isSynchronizing.setFirst(true);
        DataAccessor.getFromServer(Minecraft.getInstance().player.getUUID(), ModAccessorTypes.GET_USER_SETTINGS, (settings) -> {
            Set<ClientRoute> currentValue = settings.savedRoutes.getValue().stream().map(x -> ClientRoute.fromNbt(x, true)).collect(Collectors.toSet());
            if (clear) {
                savedRoutes.clear();
            }
            savedRoutes.addAll(currentValue);
            isSynchronizing.setFirst(false);
            DLUtils.doIfNotNull(andThen, Runnable::run);
        });
    }

    public static boolean isSynchronizing() {
        return isSynchronizing.getFirst();
    }
}
