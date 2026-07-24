package de.mrjulsen.crn.data.settings;

import java.util.List;

import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.ITimeSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface ISavableNavigatorData {

    List<SavableNavigatorDataLine> getOverviewData();
    SavableNavigatorDataLine getTitle();
    long timeOrderValue();
    default long dayOrderValue() {
        ITimeSystem system = DLTime.defaultTimeSystem();
        return (long)((timeOrderValue() + system.getDaytimeOffset()) / system.getTicksPerDay());
    }
    default Pair<String, MutableComponent> customGroup() {
        return null;
    }

    public static record SavableNavigatorDataLine(Component text, DLSprite icon) {}
}
