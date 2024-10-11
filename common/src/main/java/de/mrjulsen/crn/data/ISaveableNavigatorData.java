package de.mrjulsen.crn.data;

import java.util.List;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface ISaveableNavigatorData {

    /** All lines with information to be displayed in the overview. */
    List<SaveableNavigatorDataLine> getOverviewData();
    /** Content of the title line. */
    SaveableNavigatorDataLine getTitle();
    /** The value (usually the time at which the corresponding entry is relevant) by which the items are sorted and grouped. */
    long timeOrderValue();
    default long dayOrderValue() {
        return (timeOrderValue() + DragonLib.DAYTIME_SHIFT) / DragonLib.TICKS_PER_DAY;
    }
    /** Custom value used for grouping with custom label. Default: {@code null} (grouped by time) */
    default Pair<String, MutableComponent> customGroup() {
        return null;
    }

    public static record SaveableNavigatorDataLine(Component text, Sprite icon) {}
}
