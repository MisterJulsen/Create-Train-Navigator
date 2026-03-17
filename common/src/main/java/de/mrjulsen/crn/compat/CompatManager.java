package de.mrjulsen.crn.compat;

import dev.architectury.platform.Platform;

import java.util.Optional;

public final class CompatManager {
    private CompatManager() {}


    public static Optional<CreateThreadedTrainsCompat> getCTTCompat() {
        if (Platform.isModLoaded("createthreadedtrains")) {
            return Optional.of(new CreateThreadedTrainsCompat());
        }
        return Optional.empty();
    }

}
