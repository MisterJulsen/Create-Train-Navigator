package de.mrjulsen.crn.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.BasicDisplaySettings;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.registry.ModDisplayTypes;
import de.mrjulsen.crn.client.ber.variants.AbstractAdvancedDisplayRenderer;
import de.mrjulsen.crn.client.ber.variants.BERError;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public final class AdvancedDisplaysRegistry {

    public static record DisplayTypeResourceKey(EDisplayType category, String name) {
        private static final String NBT_ID = "DisplayId";
        @Deprecated private static final String LEGACY_NBT_ID = "Id";
        @Deprecated private static final String LEGACY_NBT_CATEGORY = "Category";

        @Override
        public final boolean equals(Object arg) {
            return arg instanceof DisplayTypeResourceKey o && category() == o.category() && name().equals(o.name());
        }

        @Override
        public final int hashCode() {
            return Objects.hash(category(), name());
        }

        public void toNbt(CompoundTag nbt) {
            nbt.putString(NBT_ID, getLocation().toString());
        }

        public ResourceLocation getLocation() {
            return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, category().getInfoTypeName() + "/" + name());
        }

        @Deprecated
        public static DisplayTypeResourceKey legacy_fromNbt(CompoundTag nbt) {
            return new DisplayTypeResourceKey(EDisplayType.getTypeById(nbt.getByte(LEGACY_NBT_CATEGORY)), new ResourceLocation(nbt.getString(LEGACY_NBT_ID)).getPath());
        }

        public static DisplayTypeResourceKey fromNbt(CompoundTag nbt) {
            String id = nbt.getString(NBT_ID);
            String[] data = new ResourceLocation(id).getPath().split("/");
            return new DisplayTypeResourceKey(data.length > 0 ? EDisplayType.getTypeByName(data[0]) : ModDisplayTypes.TRAIN_DESTINATION_SIMPLE.category(), data.length > 1 ? data[1] : ModDisplayTypes.TRAIN_DESTINATION_SIMPLE.name());
        }

        public String getTranslationKey() {
            return "display." + CreateRailwaysNavigator.MOD_ID + "." + category().getSerializedName() + "." + name();
        }

        @Override
        public final String toString() {
            return "DisplayType[" + category().getSerializedName() + "/" + name() + "]";
        }
    }

    protected static record DisplayRegistrationData<S extends IDisplaySettings, R extends AbstractAdvancedDisplayRenderer<S>>(Supplier<S> customizationSettings, Supplier<R> renderer, DisplayProperties properties) {}

    public static record DisplayProperties(boolean singleLined, @Nullable Function<AdvancedDisplayBlockEntity, Integer> platformDisplayTrainsCount) {}

    private static final Map<EDisplayType, Map<String, DisplayRegistrationData<?, ?>>> newDisplayTypes = new HashMap<>();

    public static <S extends IDisplaySettings, R extends AbstractAdvancedDisplayRenderer<S>> DisplayTypeResourceKey register(EDisplayType category, String name, Supplier<S> settings, Supplier<R> renderer, DisplayProperties properties) {
        DisplayTypeResourceKey key = new DisplayTypeResourceKey(category, name);
        Map<String, DisplayRegistrationData<?, ?>> reg = newDisplayTypes.computeIfAbsent(category, x -> new HashMap<>());

        if (reg.containsKey(name)) {
            throw new IllegalArgumentException("A display type with the id '" + key + "' is already registered!");
        }
        reg.put(name, new DisplayRegistrationData<>(settings, renderer, properties));
        return key;
    }

    public static boolean isRegietered(DisplayTypeResourceKey key) {
        return key != null && newDisplayTypes.containsKey(key.category()) && newDisplayTypes.get(key.category()).containsKey(key.name());
    }

    public static AbstractAdvancedDisplayRenderer<?> createRenderer(DisplayTypeResourceKey key) {
        if (!isRegietered(key)) {
            return new BERError();
        }
        return newDisplayTypes.get(key.category()).get(key.name()).renderer().get();
    }

    public static IDisplaySettings createSettings(DisplayTypeResourceKey key) {
        if (!isRegietered(key)) {
            return new BasicDisplaySettings();
        }
        return newDisplayTypes.get(key.category()).get(key.name()).customizationSettings().get();
    }

    public static DisplayProperties getProperties(DisplayTypeResourceKey key) {
        if (!isRegietered(key)) {
            return new DisplayProperties(true, $ -> 0);
        }
        return newDisplayTypes.get(key.category()).get(key.name()).properties();
    }

    public static Map<String, DisplayProperties> getAllOfType(EDisplayType type) {
        return newDisplayTypes.get(type).entrySet().stream().collect(Collectors.toMap(a -> a.getKey(), b -> b.getValue().properties()));
    }

    public static List<DisplayTypeResourceKey> getAllOfTypeAsKey(EDisplayType type) {
        return newDisplayTypes.get(type).entrySet().stream().map(x -> new DisplayTypeResourceKey(type, x.getKey())).toList();
    }

    public static List<String> getAllNamesOfType(EDisplayType type) {
        return newDisplayTypes.get(type).entrySet().stream().map(x -> x.getKey()).toList();
    }
}
