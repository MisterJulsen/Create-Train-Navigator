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
import de.mrjulsen.crn.block.properties.EDisplayType;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.ber.variants.BERError;
import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class AdvancedDisplaysRegistry {
    public static record DisplayTypeResourceKey(EDisplayType category, ResourceLocation id) {
        private static final String NBT_CATEGORY = "Category";
        private static final String NBT_ID = "Id";
        @Override
        public final boolean equals(Object arg) {
            return arg instanceof DisplayTypeResourceKey o && category == o.category && id.equals(o.id);
        }
        @Override
        public final int hashCode() {
            return Objects.hash(category, id);
        }
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putByte(NBT_CATEGORY, category().getId());
            nbt.putString(NBT_ID, id().toString());
            return nbt;
        }
        public static DisplayTypeResourceKey fromNbt(CompoundTag nbt) {
            return new DisplayTypeResourceKey(EDisplayType.getTypeById(nbt.getByte(NBT_CATEGORY)), new ResourceLocation(nbt.getString(NBT_ID)));
        }
        public String getTranslationKey() {
            return "display." + CreateRailwaysNavigator.MOD_ID + "." + category().getEnumValueName() + "." + id.getPath();
        }
    }
    
    /**
     * @param singleLined Whether the display can be connected vertically.
     * @param platformDisplayTrainsCount For Platform Displays only! Specifies how many trains can be shown on the display, depending on the properties of the display. If used correctly, this reduces network traffic, as data about trains that do not fit on the display are not transferred from the server.
     */
    public static record DisplayTypeInfo(boolean singleLined, Function<AdvancedDisplayBlockEntity, Integer> platformDisplayTrainsCount) {}

    private static final Map<EDisplayType, Map<ResourceLocation, Pair<Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean>>, DisplayTypeInfo>>> displayTypes = new HashMap<>();

    /**
     * Registers a new display type that can then be used in CRN.
     * @param category The display category to which the type should be assigned.
     * @param id The id of the display type.
     * @param displayRenderer The reference of the Renderer class that renders the contents of the display.
     * @param info Additional information about this display type
     * @return
     */
    public static DisplayTypeResourceKey registerDisplayType(EDisplayType category, ResourceLocation id, Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean>> displayRenderer, DisplayTypeInfo info) {
        Map<ResourceLocation, Pair<Supplier<IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean>>, DisplayTypeInfo>> reg = displayTypes.computeIfAbsent(category, x -> new HashMap<>());
        if (reg.containsKey(id)) {
            throw new IllegalArgumentException("A display type with the id '" + id + "' is already registered!");
        }
        reg.put(id, Pair.of(displayRenderer, info));
        return new DisplayTypeResourceKey(category, id);
    }

    public static boolean isRegietered(DisplayTypeResourceKey key) {
        return key != null && displayTypes.containsKey(key.category()) && displayTypes.get(key.category()).containsKey(key.id());
    }

    public static IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> getRenderer(DisplayTypeResourceKey key) {
        if (!isRegietered(key)) {
            return new BERError();
        }
        return displayTypes.get(key.category()).get(key.id()).getFirst().get();
    }

    public static DisplayTypeInfo getInfo(DisplayTypeResourceKey key) {
        if (!isRegietered(key)) {
            return new DisplayTypeInfo(true, $ -> 0);
        }
        return displayTypes.get(key.category()).get(key.id()).getSecond();
    }

    public static Map<ResourceLocation, DisplayTypeInfo> getAllOfType(EDisplayType type) {
        return displayTypes.get(type).entrySet().stream().collect(Collectors.toMap(a -> a.getKey(), b -> b.getValue().getSecond()));
    }

    public static List<DisplayTypeResourceKey> getAllOfTypeAsKey(EDisplayType type) {
        return displayTypes.get(type).entrySet().stream().map(x -> new DisplayTypeResourceKey(type, x.getKey())).toList();
    }

    public static List<ResourceLocation> getAllIdsOfType(EDisplayType type) {
        return displayTypes.get(type).entrySet().stream().map(x -> x.getKey()).toList();
    }
}
