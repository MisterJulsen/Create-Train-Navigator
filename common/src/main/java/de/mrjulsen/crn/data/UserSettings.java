package de.mrjulsen.crn.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;

public class UserSettings {

    private static final String FILENAME = CreateRailwaysNavigator.SHORT_MOD_ID + "_usersettings_";
    private static final int VERSION = 1;

    private static final String NBT_VERSION = "Version";
    private static final String NBT_DEPARTURE_IN = "DepartureIn";
    private static final String NBT_TRANSFER_TIME = "TransferTime";
    private static final String NBT_TRAIN_GROUPS = "ExcludedTrainGroups";
    private static final String NBT_SAVED_ROUTES = "SavedRoutes";
    private static final String NBT_SEARCH_DEPARTURE_TIME = "SearchDepartureIn";
    private static final String NBT_SEARCH_TRAIN_GROUPS = "SearchExcludedTrainGroups";

    private static final Map<UUID, UserSettings> settingsInstances = new LinkedHashMap<>();

    private final Collection<UserSetting<?>> allSettings = new ArrayList<>();
    private final UUID owner;
    private final boolean readOnly;

    // Settings
    public final UserSetting<Integer> navigationDepartureInTicks = registerSetting(new UserSetting<>(() -> 0, NBT_DEPARTURE_IN, (nbt, val, name) -> nbt.putInt(name, val), (nbt, name) -> nbt.getInt(name), (val) -> TimeUtils.parseDurationShort(val)));
    public final UserSetting<Integer> navigationTransferTime = registerSetting(new UserSetting<>(() -> 1000, NBT_TRANSFER_TIME, (nbt, val, name) -> nbt.putInt(name, val), (nbt, name) -> nbt.getInt(name), (val) -> TimeUtils.parseDurationShort(val)));
    public final UserSetting<Set<String>> navigationExcludedTrainGroups = registerSetting(new UserSetting<>(() -> new HashSet<>(), NBT_TRAIN_GROUPS,
    (nbt, val, name) -> {
        ListTag list = new ListTag();
        list.addAll(val.stream().map(x -> StringTag.valueOf(x)).toList());
        nbt.put(name, list);
    }, (nbt, name) -> {
        return nbt.getList(name, Tag.TAG_STRING).stream().filter(x -> GlobalSettings.hasInstance() ? GlobalSettings.getInstance().trainGroupExists(x.getAsString()) : true).map(x -> x.getAsString()).collect(Collectors.toSet());
    },(val) -> val.isEmpty() ? TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups.all").getString() : TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups.excluded", val.size()).getString()));

    public final UserSetting<Set<CompoundTag>> savedRoutes = registerSetting(new UserSetting<>(() -> new HashSet<>(), NBT_SAVED_ROUTES, (nbt, val, name) -> {
        ListTag list = new ListTag();
        list.addAll(val);
        nbt.put(name, list);
    }, (nbt, name) -> {
        return nbt.getList(name, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).collect(Collectors.toSet());
    },(val) -> TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".saved_routes.saved", val.size()).getString()));
    
    public final UserSetting<Integer> searchDepartureInTicks = registerSetting(new UserSetting<>(() -> 0, NBT_SEARCH_DEPARTURE_TIME, (nbt, val, name) -> nbt.putInt(name, val), (nbt, name) -> nbt.getInt(name), (val) -> TimeUtils.parseDurationShort(val)));
    public final UserSetting<Set<String>> searchExcludedTrainGroups = registerSetting(new UserSetting<>(() -> new HashSet<>(), NBT_SEARCH_TRAIN_GROUPS,
    (nbt, val, name) -> {
        ListTag list = new ListTag();
        list.addAll(val.stream().map(x -> StringTag.valueOf(x)).toList());
        nbt.put(name, list);
    }, (nbt, name) -> {
        return nbt.getList(name, Tag.TAG_STRING).stream().filter(x -> GlobalSettings.hasInstance() ? GlobalSettings.getInstance().trainGroupExists(x.getAsString()) : true).map(x -> x.getAsString()).collect(Collectors.toSet());
    },(val) -> val.isEmpty() ? TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups.all").getString() : TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".search_options.train_groups.excluded", val.size()).getString()));

    public UserSettings(UUID playerId, boolean readOnly) {
        this.owner = playerId;
        this.readOnly = readOnly;
    }

    public UUID getOwnerId() {
        return owner;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    private void checkReadOnly() {              
        if (isReadOnly()) {
            throw new IllegalAccessError("This instance of the user settings is read-only!");
        }
    }

    private static void update(UserSettings settings) {
        if (settingsInstances.containsKey(settings.getOwnerId())) {
            settingsInstances.get(settings.getOwnerId()).checkReadOnly();
        }
        settingsInstances.put(settings.getOwnerId(), settings);
    }

    protected <T extends UserSetting<?>> T registerSetting(T setting) {
        allSettings.add(setting);
        return setting;
    }


    public static UserSettings getSettingsFor(UUID playerId, boolean readOnly) {
        return settingsInstances.computeIfAbsent(playerId, x -> UserSettings.load(x, readOnly));
    }

    /** Client-side only! */
    public final void clientSave(Runnable andThen) throws RuntimeSideException {
        if (Platform.getEnv() == EnvType.SERVER) {
            throw new RuntimeSideException(true);
        }  
        checkReadOnly();
        DataAccessor.getFromServer(this, ModAccessorTypes.SAVE_USER_SETTINGS, $ -> DLUtils.doIfNotNull(andThen, x -> x.run()));
    }

    /** Server-side only! */
    public final synchronized void save() throws RuntimeSideException {
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }
        checkReadOnly();
        UserSettings.update(this);
        CompoundTag nbt = this.toNbt();    
        try {
            NbtIo.writeCompressed(nbt, new File(ModCommonEvents.getCurrentServer().get().getWorldPath(new LevelResource("data/" + FILENAME + getOwnerId() + ".nbt")).toString()));
            CreateRailwaysNavigator.LOGGER.info("Saved user settings.");
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to save user settings.", e);
        }
    }
    
    /** Server-side only! */
    public static UserSettings load(UUID playerId, boolean readOnly) throws RuntimeSideException {  
        if (!ModCommonEvents.hasServer()) {
            throw new RuntimeSideException(false);
        }

        File settingsFile = new File(ModCommonEvents.getCurrentServer().get().getWorldPath(new LevelResource("data/" + FILENAME + playerId + ".nbt")).toString());    
        
        if (settingsFile.exists()) {            
            try {
                return UserSettings.fromNbt(NbtIo.readCompressed(settingsFile), playerId, readOnly);
            } catch (IOException e) {
                CreateRailwaysNavigator.LOGGER.error("Cannot load user settings for player: " + playerId, e);
            }
        }
        return new UserSettings(playerId, readOnly);
    }

    public final CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        allSettings.forEach(x -> x.serialize(nbt));
        nbt.putInt(NBT_VERSION, VERSION); 
        return nbt;
    }

    public final static UserSettings fromNbt(CompoundTag nbt, UUID playerId, boolean readOnly) {
        UserSettings settings = new UserSettings(playerId, readOnly);
        @SuppressWarnings("unused") final int version = nbt.getInt(NBT_VERSION);
        settings.allSettings.forEach(x -> x.deserialize(nbt));
        return settings;
    }

    public static class UserSetting<T> {
        private T value;
        private final String serializationName;
        private final Supplier<T> defaultValue;
        private final ISerializationContext<T> serializer;
        private final BiFunction<CompoundTag, String, T> deserializer;
        private final Function<T, String> stringRepresentation;

        public UserSetting(Supplier<T> defaultValue, String serializationName, ISerializationContext<T> serializer, BiFunction<CompoundTag, String, T> deserializer, Function<T, String> stringRepresentation) {
            this.defaultValue = defaultValue;
            this.serializer = serializer;
            this.serializationName = serializationName;
            this.deserializer = deserializer;
            this.stringRepresentation = stringRepresentation;
            this.value = defaultValue.get();
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public void setToDefault() {
            this.value = getDefault();
        }

        public T getDefault() {
            return defaultValue.get();
        }

        private void serialize(CompoundTag nbt) {
            serializer.execute(nbt, value, getSerializationName());
        }

        private T deserialize(CompoundTag nbt) {
            return value = deserializer.apply(nbt, getSerializationName());
        }

        private String getSerializationName() {
            return serializationName;
        }

        @Override
        public String toString() {
            return stringRepresentation.apply(value);
        }
    }

    @FunctionalInterface
    private static interface ISerializationContext<T> {
        void execute(CompoundTag nbt, T value, String serializationName);
    }
}
