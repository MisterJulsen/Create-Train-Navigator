package de.mrjulsen.crn.api.core.notice;

import java.util.List;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * A notice about a station as a whole, shown on the station's displays whether or not trains call there, for example
 * "No service due to engineering works". Supplied by an {@link IStationNoticeProvider}.
 * <p>
 * The text is translated on the client with the display's language. A key no language file knows is shown as it is,
 * so a provider may pass plain text instead of a key.
 *
 * @param translationKey The translation key of the text, or the text itself.
 * @param args           Values filled into the text, in order.
 */
public record StationNotice(String translationKey, List<String> args) {

    private static final String NBT_KEY = "Key";
    private static final String NBT_ARGS = "Args";

    public StationNotice {
        translationKey = translationKey == null ? "" : translationKey;
        args = args == null ? List.of() : List.copyOf(args);
    }

    /** A notice with a translated text. */
    public static StationNotice translatable(String translationKey, String... args) {
        return new StationNotice(translationKey, List.of(args));
    }

    /** A notice with a fixed text. */
    public static StationNotice text(String text) {
        return new StationNotice(text, List.of());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_KEY, translationKey);
        nbt.put(NBT_ARGS, NbtHelper.writeStrings(args));
        return nbt;
    }

    public static StationNotice fromNbt(CompoundTag nbt) {
        return new StationNotice(nbt.getString(NBT_KEY), NbtHelper.readStrings(nbt, NBT_ARGS));
    }
}
