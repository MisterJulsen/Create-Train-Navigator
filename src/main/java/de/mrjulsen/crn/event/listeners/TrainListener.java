package de.mrjulsen.crn.event.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.util.TrainUtils;

public class TrainListener {

    private static TrainListener instance;

    private boolean isRunning;
    private final Map<UUID, List<Integer>> TRAIN_DURATIONS = new HashMap<>();
    private final Map<UUID, Integer> lastTicks = new HashMap<>();

    private TrainListener() {
        isRunning = true;

        Thread t = new Thread(() -> {
            while (isRunning) {

                TrainUtils.getAllTrains().forEach(train -> {
                    if (!TrainUtils.isTrainValid(train)) {
                        return;
                    }

                    OptionalInt maxTrainDuration = train.runtime.submitPredictions().stream().mapToInt(x -> x.ticks).max();

                    if (maxTrainDuration.isPresent()) {
                        if (!lastTicks.containsKey(train.id)) {
                            lastTicks.put(train.id, 0);
                        }

                        if (lastTicks.get(train.id) < maxTrainDuration.getAsInt()) {
                            if (!TRAIN_DURATIONS.containsKey(train.id)) {
                                TRAIN_DURATIONS.put(train.id, new ArrayList<>());
                            }

                            TRAIN_DURATIONS.get(train.id).add(maxTrainDuration.getAsInt());
                            if (TRAIN_DURATIONS.get(train.id).size() > 10) {
                                TRAIN_DURATIONS.get(train.id).remove(0);
                            }
                        }
                        lastTicks.replace(train.id, maxTrainDuration.getAsInt());
                    }                    
                });

                try {
                    Thread.sleep(ModCommonConfig.TRAIN_LISTENER_INTERVALL.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.setName("Train Listener");
        t.start();
    }

    public static TrainListener getInstance() {
        return instance;
    }

    public int getApproximatedTrainDuration(Train train) {
        return TRAIN_DURATIONS.containsKey(train.id) ? TRAIN_DURATIONS.get(train.id).stream().mapToInt(v -> v).sum() / TRAIN_DURATIONS.get(train.id).size() : 0;
    }

    public static TrainListener start() {
        if (instance == null) 
            instance = new TrainListener();

        return instance;
    }

    public static void stop() {
        if (instance == null)
            return;

        instance.stopInstance();
    }

    private void stopInstance() {
        isRunning = false;
    }
}
