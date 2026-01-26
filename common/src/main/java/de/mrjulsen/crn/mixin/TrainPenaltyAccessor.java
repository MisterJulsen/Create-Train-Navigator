package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.entity.Train;

@Mixin(Train.Penalties.class)
public interface TrainPenaltyAccessor {
    @Accessor("MANUAL_TRAIN") public static int manualTrain() { throw new AssertionError(); }
    @Accessor("IDLE_TRAIN") public static int idleTrain() { throw new AssertionError(); }
    @Accessor("ARRIVING_TRAIN") public static int arrivingTrain() { throw new AssertionError(); }
    @Accessor("WAITING_TRAIN") public static int waitingTrain() { throw new AssertionError(); }
    @Accessor("ANY_TRAIN") public static int anyTrain() { throw new AssertionError(); }
    @Accessor("RED_SIGNAL") public static int redSignal() { throw new AssertionError(); }
    @Accessor("REDSTONE_RED_SIGNAL") public static int redstoneRedSignal() { throw new AssertionError(); }
}
