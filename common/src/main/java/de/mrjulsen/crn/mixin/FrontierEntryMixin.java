package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import de.mrjulsen.crn.util.IFrontierEntry;
import de.mrjulsen.crn.util.PenaltyResult;

@Mixin(targets = "com.simibubi.create.content.trains.entity.Navigation$FrontierEntry")
public abstract class FrontierEntryMixin implements IFrontierEntry {

    private PenaltyResult penaltyReasons;
    
    @Shadow
    private int penalty;

    @Override
    public PenaltyResult getPenaltyReasons() {
        return penaltyReasons;
    }

    @Override
    public void setPenaltyReasons(PenaltyResult list) {
        this.penaltyReasons = list;
    }

    @Override
    public int getPenalty() {
        return penalty;
    }

}
