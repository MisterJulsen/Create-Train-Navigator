package de.mrjulsen.crn.compat;

import de.mrjulsen.ctt.CreateThreadedTrains;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;

public class CreateThreadedTrainsCompat {

    public double getTpsFactor() {
        double serverMspt = GameInstance.getServer().getAverageTickTime();
        double targetNanoTime = Math.max(50.0, serverMspt) * 1_000_000.0;
        double myTickTime = (double) CreateThreadedTrains.getAvgTickTime();
        return Math.min(1.0, targetNanoTime / myTickTime);
    }
}
