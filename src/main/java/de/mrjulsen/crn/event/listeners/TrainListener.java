package de.mrjulsen.crn.event.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.TimedWaitCondition;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.mixin.ScheduleDataAccessor;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.utils.ScheduledTask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class TrainListener {

    private static TrainListener instance;

    private boolean isRunning = true;
    private int totalTrainCount;
    private int listeingTrainCount;
    private final Map<UUID, List<Integer>> TRAIN_DURATIONS = new HashMap<>();
    private final Map<UUID, Integer> lastTicks = new HashMap<>();

    public int getDepartmentTime(Level level, Train train) {
		List<List<ScheduleWaitCondition>> conditions = train.runtime.getSchedule().entries.get(train.runtime.currentEntry).conditions;
		if (conditions.isEmpty() || ((ScheduleDataAccessor)train.runtime).crn$conditionProgress().isEmpty() || ((ScheduleDataAccessor)train.runtime).crn$conditionContext().isEmpty())
			return 0;

		List<ScheduleWaitCondition> list = conditions.get(0);
		int progress = ((ScheduleDataAccessor)train.runtime).crn$conditionProgress().get(0);
		if (progress >= list.size())
			return 0;

		CompoundTag tag = ((ScheduleDataAccessor)train.runtime).crn$conditionContext().get(0);
		ScheduleWaitCondition condition = list.get(progress);
		return ((TimedWaitCondition)condition).totalWaitTicks() - tag.getInt("Time");
	}

    private boolean performTask(TrainListener instance, Level level, int iteration) {

        if (!isRunning) {
            return false;
        }

        new Thread(() -> {
            Collection<Train> trains = TrainUtils.getAllTrains();
            listeingTrainCount = 0;
            trains.forEach(train -> {
                if (!TrainUtils.isTrainValid(train)) {
                    return;
                }

                OptionalInt maxTrainDuration = TrainUtils.getTrainDeparturePredictions(train.id).stream().mapToInt(x -> x.getTicks()).max();

                if (maxTrainDuration.isPresent()) {
                    if (!lastTicks.containsKey(train.id)) {
                        lastTicks.put(train.id, 0);
                    }

                    if (lastTicks.get(train.id) < maxTrainDuration.getAsInt()) {
                        
                    }
                    if (!TRAIN_DURATIONS.containsKey(train.id)) {
                        TRAIN_DURATIONS.put(train.id, new ArrayList<>());
                    }

                    TRAIN_DURATIONS.get(train.id).add(maxTrainDuration.getAsInt());
                    if (TRAIN_DURATIONS.get(train.id).size() > 30) {
                        TRAIN_DURATIONS.get(train.id).remove(0);
                    }
                    lastTicks.replace(train.id, maxTrainDuration.getAsInt());
                }
                listeingTrainCount++;
            });

            this.totalTrainCount = trains.size();
        }, "Train Listener Worker").run();

        return isRunning;
    }

    public static TrainListener getInstance() {
        return instance;
    }

    public static TrainListener start(Level level) {
        if (instance == null) 
            instance = new TrainListener();

        ScheduledTask.create(instance, level, ModCommonConfig.TRAIN_WATCHER_INTERVALL.get(), Integer.MAX_VALUE, instance::performTask);

        ModMain.LOGGER.info("TrainListener started.");
        return instance;
    }

    public static void stop() {
        if (instance == null)
            return;

        instance.stopInstance();
    }

    private void stopInstance() {
        isRunning = false;
        ModMain.LOGGER.info("TrainListener stopped.");
    }



    public int getApproximatedTrainDuration(Train train) {
        return getApproximatedTrainDuration(train.id);
    }

    public int getApproximatedTrainDuration(UUID trainId) {
        return TRAIN_DURATIONS.containsKey(trainId) ? TRAIN_DURATIONS.get(trainId).stream().mapToInt(v -> v).sum() / TRAIN_DURATIONS.get(trainId).size() : 0;
    }    

    public int getTotalTrainCount() {
        return this.totalTrainCount;
    }

    public int getListeningTrainCount() {
        return this.listeingTrainCount;
    }
}
