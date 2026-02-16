package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.util.Cache;

public class ScheduleSection {

    private static final int INVALID = -1;
    
    private transient final TrainData data;    
    private transient final int scheduleIndex;
    private transient final boolean isDefault;

    private final boolean includeLastStationOfLastSection;
    private final boolean usable;
    private final TrainCategory trainCategory;
    private final TrainLine trainLine;

    private final Cache<List<TrainPrediction>> predictions = new Cache<>(() -> getPredictions(INVALID, false));
    private final Cache<List<String>> stopoversCache = new Cache<>(() -> {
        List<TrainPrediction> predictions = this.predictions.get();
        List<String> result = new ArrayList<>(predictions.size() - 2);
        for (int i = 1; i < predictions.size() - 1; i++) {
            result.add(predictions.get(i).getStationTag().getTagName().get());
        }
        return result;
    });
    private final Cache<ScheduleSection> nextSection;
    private final Cache<ScheduleSection> previousSection;

    public ScheduleSection(TrainData data, int indexInSchedule, TrainCategory category, TrainLine line, boolean includePreviousStation, boolean usable) {
        this(false, data, indexInSchedule, category, line, includePreviousStation, usable);
    }
    
    private ScheduleSection(boolean isDefault, TrainData data, int indexInSchedule, TrainCategory category, TrainLine line, boolean includePreviousStation, boolean usable) {
        this.data = data;
        this.scheduleIndex = indexInSchedule;
        this.isDefault = isDefault;
        this.trainCategory = category;
        this.trainLine = line;
        this.includeLastStationOfLastSection = includePreviousStation;
        this.usable = usable;

        nextSection = new Cache<>(() -> {            
            if (data.isSingleSection()) {
                return this;
            }
    
            List<ScheduleSection> sections = data.getSections();
            if (sections.size() <= 1) {
                return this;
            }
            int selfIndex = sections.indexOf(this);
            if (selfIndex < 0 || selfIndex >= sections.size()) {
                return sections.get(0);
            }
            return sections.get((selfIndex + 1) % sections.size());
        });

        
        previousSection = new Cache<>(() -> {
            if (data.isSingleSection()) {
                return this;
            }
    
            List<ScheduleSection> sections = data.getSections();
            if (sections.size() <= 1) {
                return this;
            }
            int selfIndex = sections.indexOf(this);
            if (selfIndex < 0 || selfIndex >= sections.size()) {
                return sections.get(0);
            }
            int prevIndex = selfIndex - 1;
            return sections.get(prevIndex < 0 ? sections.size() - 1 : prevIndex);
        });
    }

    public static final ScheduleSection def(TrainData data) {
        return new ScheduleSection(true, data, 0, null, null, true, true);
    }

    public boolean isDefault() {
        return isDefault;
    }

    public TrainData getData() {
        return data;
    }

    public int getScheduleIndex() {
        return scheduleIndex;
    }

    public boolean shouldIncludeNextStationOfNextSection() {
        return includeLastStationOfLastSection;
    }
    
    public boolean isUsable() {
        return usable;
    }

    public Optional<TrainCategory> getTrainCategory() {
        return Optional.ofNullable(trainCategory);
    }

    public Optional<TrainLine> getTrainLine() {
        return Optional.ofNullable(trainLine);
    }

    public ScheduleSection nextSection() {
        return nextSection.get();
    }

    public ScheduleSection previousSection() {
        return previousSection.get();
    }

    /**
     * Creates a list of all stops assigned to this section.
     * @param startingAtIndex The schedule index from which found stops should be added, or {@code < 0} to get all elements of the section.
     * @param ignoreIncludeLastStationRule Whether the first station of the next section should be included when the option Include start of next station is enabled or not.
     * @return A list of all predictions that belong to this section.
     */

    
    public List<TrainPrediction> getPredictions(int startingAtIndex, boolean ignoreIncludeLastStationRule) {
        if (data.getTrain() == null || data.getTrain().runtime == null || data.getTrain().runtime.getSchedule() == null) {
            return List.of();
        }
        List<TrainPrediction> result = new ArrayList<>();
        ScheduleSection nextSection = nextSection();
        
        Map<Integer, TrainPrediction> predictionsSrc = data.getPredictionsMap();
        Map<Integer, TrainPrediction> predictions = new HashMap<>(predictionsSrc.size());
        for (Map.Entry<Integer, TrainPrediction> prediction : predictionsSrc.entrySet()) {
            if (GlobalSettings.getInstance().isStationBlacklisted(prediction.getValue().getTargetedStationName())) {
                continue;
            }
            predictions.put(prediction.getKey(), prediction.getValue());
        }

        final int startIndex = getScheduleIndex();
        final int stopIndex = nextSection.getScheduleIndex();
        final int count = data.getTrain().runtime.getSchedule().entries.size();

        boolean customStartFound = false;
        boolean endReached = false;
        for (int i = 0; i <= count * 2; i++) {
            final int j = (startIndex + i) % count;
            if (i != 0 && j == stopIndex) {
                if (!ignoreIncludeLastStationRule && shouldIncludeNextStationOfNextSection()) {
                    endReached = true;
                } else break;
            }
            customStartFound = customStartFound || startingAtIndex < 0 || j == startingAtIndex;
            if (!predictions.containsKey(j) || !customStartFound) continue;
            result.add(predictions.get(j));
            if (endReached) break;
        }
        return result;
    }


    public int getFirstIndexFor(StationTag tag) {
        List<TrainPrediction> predictions = getPredictions(INVALID, false);
        for (TrainPrediction p : predictions) {
            if (p.getStationTag().equals(tag)) {
                return p.getEntryIndex();
            }
        }
        return 0;
    }

    /**
     * Creates a list as a route with all stations in this section.
     * @param simulationTime How far ahead the predictions should be calculated.
     * @param currentIndex The schedule index of the current station as a reference point. The route is calculated so that the station with the specified index fits with it's timing into the generated route.
     * @return A list as a route with all stations in this section
     */
    public List<TrainStop> getAllStops(long simulationTime, int currentIndex) {
        List<TrainStop> result = new ArrayList<>();
        List<TrainPrediction> predictions = getPredictions(INVALID, false);

        TrainStop previousStop = null;
        for (TrainPrediction prediction : predictions) {
            TrainStop stop = new TrainStop(prediction);
            stop.simulateTicks(simulationTime);
            if (previousStop != null && previousStop.getScheduledArrivalTime() > stop.getScheduledArrivalTime()) {
                if (prediction.getEntryIndex() == currentIndex) {
                    result.forEach(x -> x.simulateCycles(-1));
                } else {
                    stop.simulateCycles(1);
                }
            }
            result.add(stop);
            previousStop = stop;
        }
        return result;
    }

    public List<String> getStopovers() {
        return stopoversCache.get();
    }

    public List<String> getStopoversFrom(int startIndex) {
        List<String> predictions = new ArrayList<>();
        List<TrainPrediction> predictionsSrc = this.predictions.get();
        boolean startFound = false;
        for (int i = 0; i < predictionsSrc.size() - 1; i++) {
            TrainPrediction prediction = predictionsSrc.get(i);
            boolean wasStartFound = startFound;
            if (prediction.getEntryIndex() == startIndex) startFound = true;
            if (!wasStartFound) continue;
            if (GlobalSettings.getInstance().isStationBlacklisted(prediction.getStationFilter())) continue;
            predictions.add(prediction.getStationTag().getTagName().get());
        }
        return predictions;
    }

    public Optional<TrainPrediction> getFinalStop() {
        List<TrainPrediction> predictions = this.predictions.get();
        return predictions.isEmpty() ? Optional.empty() : Optional.ofNullable(predictions.get(predictions.size() - 1));
    }

    public boolean isFinalStop(TrainPrediction prediction) {
        Optional<TrainPrediction> pred = getFinalStop();
        return pred.isPresent() && pred.get() == prediction;
    }

    public boolean isFirstStop(TrainPrediction prediction) {
        Optional<TrainPrediction> pred = getFirstStop();
        return pred.isPresent() && pred.get() == prediction;
    }

    public boolean isFinalStop(int scheduleIndex) {
        Optional<TrainPrediction> pred = getFinalStop();
        return pred.isPresent() && pred.get().getEntryIndex() == scheduleIndex;
    }

    public boolean isFirstStop(int scheduleIndex) {
        Optional<TrainPrediction> pred = getFirstStop();
        return pred.isPresent() && pred.get().getEntryIndex() == scheduleIndex;
    }

    public Optional<TrainPrediction> getFirstStop() {
        List<TrainPrediction> predictions = this.predictions.get();
        return predictions.isEmpty() ? Optional.empty() : Optional.ofNullable(predictions.get(0));
    }

    public Optional<TrainPrediction> getNextStop() {
        List<TrainPrediction> predictions = this.predictions.get();
        return predictions.isEmpty() ? Optional.empty() : Optional.ofNullable(predictions.get(0));
    }

    public String getDisplayText() {
        if (!isUsable()) {
            return CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.not_in_service").getString();
        }        
        return getFinalStop().map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x.getTargetedStationName()).getTagName().get()).orElse("?");
    }

    public String getDisplayTextStart() {
        return !isUsable() ? CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.not_in_service").getString() : getFirstStop().map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x.getTargetedStationName()).getTagName().get()).orElse("?");
    }

    public String getStartStationName() {
        return getFirstStop().map(x -> x.getTargetedStationName()).orElse("?");
    }

    public String getDestinationStationName() {
        return getFinalStop().map(x -> x.getTargetedStationName()).orElse("?");
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScheduleSection o) {
            return getScheduleIndex() == o.getScheduleIndex();
        }
        return false;
    }
}
