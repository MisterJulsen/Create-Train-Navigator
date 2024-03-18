package de.mrjulsen.crn.block.display;

import java.util.List;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.MutableComponent;

public class StationDisplayTarget extends DisplayTarget {
    @Override
    public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
    }

    @Override
    public DisplayTargetStats provideStats(DisplayLinkContext context) {
        return new DisplayTargetStats(2, 2, this);
    }
}