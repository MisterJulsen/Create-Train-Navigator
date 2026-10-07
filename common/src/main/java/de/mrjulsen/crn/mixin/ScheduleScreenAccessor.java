package de.mrjulsen.crn.mixin;

import java.util.List;
import java.util.function.Consumer;

import net.createmod.catnip.data.IntAttached;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.trains.schedule.DestinationSuggestions;
import com.simibubi.create.content.trains.schedule.IScheduleInput;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;

@Mixin(ScheduleScreen.class)
public interface ScheduleScreenAccessor {

    @Accessor("destinationSuggestions")
    DestinationSuggestions crn$getDestinationSuggestions();

    @Accessor("destinationSuggestions")
    void crn$setDestinationSuggestions(DestinationSuggestions suggestions);

    @Accessor("onEditorClose")
    Consumer<Boolean> crn$getOnEditorClose();


    @Invoker("onDestinationEdited")
    void crn$onDestinationEdited(String text);

    @Invoker("getViableStations")
    List<IntAttached<String>> crn$getViableStations(IScheduleInput field);
}
