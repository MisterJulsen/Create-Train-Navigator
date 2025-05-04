package de.mrjulsen.crn.mixin;

import java.util.ArrayList;
import java.util.List;

import net.createmod.catnip.data.IntAttached;
import org.spongepowered.asm.mixin.Mixin;
import com.simibubi.create.content.trains.schedule.IScheduleInput;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.schedule.instruction.IStationTagInstruction;
import de.mrjulsen.crn.data.schedule.instruction.ITrainNameInstruction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(ScheduleScreen.class)
public class ScheduleScreenMixin {

    public ScheduleScreenAccessor accessor() {
        return (ScheduleScreenAccessor)(Object)this;
    }

    public ScheduleScreen self() {
        return (ScheduleScreen)(Object)this;
    }
    
    public int getTopPos() {
        return ((AbstractContainerScreen<?>)(Object)this).topPos;
    }



    public List<IntAttached<String>> onGetViableStations(IScheduleInput field) {
        if (field instanceof IStationTagInstruction) {
            List<StationTag> stations = GlobalSettings.getInstance().getAllStationTags();
            List<IntAttached<String>> result = new ArrayList<>();
            for (int i = 0; i < stations.size(); i++) {
                result.add(IntAttached.with(i, stations.get(i).getTagName().get()));
            }
            return result;
        } else if (field instanceof ITrainNameInstruction) {
            return null; /* TODO */
            //return ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream().map(station -> IntAttached.with(0, station)).toList();
        }
        return null;
    }
}
