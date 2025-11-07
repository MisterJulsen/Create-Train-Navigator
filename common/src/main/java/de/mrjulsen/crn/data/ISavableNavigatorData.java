package de.mrjulsen.crn.data;

import java.util.List;

import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.ITimeSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface ISavableNavigatorData {

    /** All lines with information to be displayed in the overview. */
    List<SavableNavigatorDataLine> getOverviewData();
    /** Content of the title line. */
    SavableNavigatorDataLine getTitle();
    /** The value (usually the time at which the corresponding entry is relevant) by which the items are sorted and grouped. */
    long timeOrderValue();
    default long dayOrderValue() {
        ITimeSystem system = new ConfiguredTimeSystem();
        return (long)((timeOrderValue() + system.getDaytimeOffset()) / system.getTicksPerDay());
    }
    /** Custom value used for grouping with custom label. Default: {@code null} (grouped by time) */
    default Pair<String, MutableComponent> customGroup() {
        return null;
    }

    public static record SavableNavigatorDataLine(Component text, DLSprite icon) {}
}
