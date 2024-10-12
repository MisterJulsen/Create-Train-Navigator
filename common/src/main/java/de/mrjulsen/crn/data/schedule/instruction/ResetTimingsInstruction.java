package de.mrjulsen.crn.data.schedule.instruction;

import java.util.List;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Pair;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ResetTimingsInstruction extends ScheduleInstruction implements IStationTagInstruction, IPredictableInstruction {
    

    public ResetTimingsInstruction() {
    }

    @Override
    public Pair<ItemStack, Component> getSummary() {
        return Pair.of(new ItemStack(Blocks.BARRIER), TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule.instruction." + getId().getPath()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "reset_timings");
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    @Override
	public List<Component> getTitleAs(String type) {
        return List.of(TextUtils.translate(CreateRailwaysNavigator.MOD_ID + ".schedule." + type + "." + getId().getPath()).withStyle(ChatFormatting.GOLD));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        ClientWrapper.initResetTimingsInstruction(this, builder);
	}

    @Override
    public void run(ScheduleRuntime runtime, TrainData data, Train train, int index) {
        DLUtils.doIfNotNull(data, x -> {
            data.resetPredictions();
        });
    }

    @Override
    public void predict(TrainData data, ScheduleRuntime runtime, int indexInSchedule, Train train) {
    }
}