package de.mrjulsen.crn.block.behaviour;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class SteppedScrollValueBehaviour extends ScrollValueBehaviour {

    public static final int DEFAULT_MILESTONE_INTERVAL = 10;

    private final int step;
    private int milestoneInterval = DEFAULT_MILESTONE_INTERVAL;
    private Component caption = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.value");

    public SteppedScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot, int step) {
        super(label, be, slot);
        this.step = step;
        withFormatter(x -> String.valueOf(x * step));
    }

    public SteppedScrollValueBehaviour withMilestoneInterval(int increments) {
        this.milestoneInterval = increments;
        return this;
    }

    public SteppedScrollValueBehaviour betweenStepped(int min, int max) {
        between(Math.floorDiv(min, step), Math.floorDiv(max, step));
        return this;
    }

    public SteppedScrollValueBehaviour setValueCaption(Component caption) {
        this.caption = caption;
        return this;
    }

    public void setStepped(int value) {
        setValue(Math.floorDiv(value, step));
    }

    public int getStepped() {
        return getValue() * step;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, milestoneInterval, List.of(caption),
            new ValueSettingsFormatter(setting -> Component.literal(String.valueOf(setting.value() * step))));
    }
}
