package de.mrjulsen.crn.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.simibubi.create.content.trains.schedule.DestinationSuggestions;
import com.simibubi.create.content.trains.schedule.IScheduleInput;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.foundation.utility.IntAttached;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.schedule.instruction.ICustomSuggestionsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.IStationTagInstruction;
import de.mrjulsen.crn.data.schedule.instruction.ITrainNameInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
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

    @SuppressWarnings("resource")
    @Inject(method = "updateEditorSubwidgets", remap = false, at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lcom/simibubi/create/foundation/gui/ModularGuiLine;loadValues(Lnet/minecraft/nbt/CompoundTag;Ljava/util/function/Consumer;Ljava/util/function/Consumer;)V"), cancellable = true)
    public void onUpdateEditorSubwidgets(IScheduleInput field, CallbackInfo ci) {
        if (field instanceof ICustomSuggestionsInstruction) {
            accessor().crn$getEditorSubWidgets().forEach(e -> {
                if (!(e instanceof EditBox destinationBox))
                    return;
                accessor().crn$setDestinationSuggestions(new DestinationSuggestions(Minecraft.getInstance(), self(), destinationBox, Minecraft.getInstance().font, onGetViableStations(field), getTopPos() + 33));
                accessor().crn$getDestinationSuggestions().setAllowSuggestions(true);
                accessor().crn$getDestinationSuggestions().updateCommandInfo();
                destinationBox.setResponder(accessor()::crn$onDestinationEdited);
            });
        }

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
