package de.mrjulsen.crn.registry;

import java.util.function.Supplier;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.schedule.instruction.ResetTimingsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;
import net.minecraft.resources.ResourceLocation;

public class ModSchedule {

    static {
        registerInstruction("travel_section", TravelSectionInstruction::new);
        registerInstruction("reset_timings", ResetTimingsInstruction::new);
        
        registerCondition("dynamic_delay", DynamicDelayCondition::new);
    }

    private static void registerInstruction(String name, Supplier<? extends ScheduleInstruction> factory) {
        Schedule.INSTRUCTION_TYPES.add(Pair.of(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, name), factory));
    }    

    private static void registerCondition(String name, Supplier<? extends ScheduleWaitCondition> factory) {
        Schedule.CONDITION_TYPES.add(Pair.of(new ResourceLocation(CreateRailwaysNavigator.MOD_ID, name), factory));
    }

    public static void init() {}
}