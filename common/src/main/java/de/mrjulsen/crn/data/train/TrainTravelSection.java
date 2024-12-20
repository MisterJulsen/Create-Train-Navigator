package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainGroup;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.mcdragonlib.data.Cache;

public class TrainTravelSection {

    private static final int INVALID = -1;
    
    private transient final TrainData data;    
    private transient final int scheduleIndex;
    private transient final boolean isDefault;

    private final boolean includeLastStationOfLastSection;
    private final boolean usable;
    private final TrainGroup trainGroup;
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
    private final Cache<TrainTravelSection> nextSection;
    private final Cache<TrainTravelSection> previousSection;

    public TrainTravelSection(TrainData data, int indexInSchedule, TrainGroup group, TrainLine line, boolean includePreviousStation, boolean usable) {
        this(false, data, indexInSchedule, group, line, includePreviousStation, usable);
    }
    
    private TrainTravelSection(boolean isDefault, TrainData data, int indexInSchedule, TrainGroup group, TrainLine line, boolean includePreviousStation, boolean usable) {
        this.data = data;
        this.scheduleIndex = indexInSchedule;
        this.isDefault = isDefault;
        this.trainGroup = group;
        this.trainLine = line;
        this.includeLastStationOfLastSection = includePreviousStation;
        this.usable = usable;

        nextSection = new Cache<>(() -> {            
            if (data.isSingleSection()) {
                return this;
            }
    
            List<TrainTravelSection> sections = data.getSections();
            if (sections.isEmpty()) {
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
    
            List<TrainTravelSection> sections = data.getSections();
            if (sections.isEmpty()) {
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

    public static final TrainTravelSection def(TrainData data) {
        return new TrainTravelSection(true, data, 0, null, null, true, true);
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

    public Optional<TrainGroup> getTrainGroup() {
        return Optional.ofNullable(trainGroup);
    }

    public Optional<TrainLine> getTrainLine() {
        return Optional.ofNullable(trainLine);
    }

    public TrainTravelSection nextSection() {
        return nextSection.get();
    }

    public TrainTravelSection previousSection() {
        return previousSection.get();
    }

    /**
     * Creates a list of all stops assigned to this section.
     * @param startingAtIndex The schedule index from which found stops should be added, or {@code < 0} to get all elements of the section.
     * @return A list of all stops assigned to this section.
     */
    public List<TrainPrediction> getPredictions(int startingAtIndex, boolean ignoreIncludeLastStationRule) {
        if (data.getTrain() == null || data.getTrain().runtime == null || data.getTrain().runtime.getSchedule() == null) {
            return List.of();
        }
        List<TrainPrediction> result = new ArrayList<>();
        TrainTravelSection nextSection = nextSection();
        
        Map<Integer, TrainPrediction> predictionsSrc = data.getPredictionsRaw();
        Map<Integer, TrainPrediction> predictions = new HashMap<>(predictionsSrc.size());
        for (Map.Entry<Integer, TrainPrediction> prediction : predictionsSrc.entrySet()) {
            if (GlobalSettings.getInstance().isStationBlacklisted(prediction.getValue().getStationName())) {
                continue;
            }
            predictions.put(prediction.getKey(), prediction.getValue());
        }

        final int startIndex = getScheduleIndex();
        final int stopIndex = nextSection.getScheduleIndex();
        final int count = data.getTrain().runtime.getSchedule().entries.size();

        boolean customStartFound = false;
        boolean endReached = false;
        TrainPrediction pred = null;
        for (int i = 0; i < count * 2; i++) {
            final int j = (startIndex + i) % count;
            if (i != 0 && j == stopIndex) {
                if (!ignoreIncludeLastStationRule && shouldIncludeNextStationOfNextSection()) {
                    endReached = true;
                } else return result;
            }
            customStartFound = customStartFound || startingAtIndex < 0 || j == startingAtIndex;
            if (!predictions.containsKey(j) || !customStartFound) continue;
            pred = predictions.get(j);
            result.add(pred);
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

        TrainStop lastStop = null;
        for (TrainPrediction prediction : predictions) {
            TrainStop stop = new TrainStop(prediction);
            stop.simulateTicks(simulationTime);
            if (lastStop != null && lastStop.getScheduledArrivalTime() > stop.getScheduledArrivalTime()) {
                if (prediction.getEntryIndex() == currentIndex) {
                    result.forEach(x -> x.simulateCycles(-1));
                } else {
                    stop.simulateCycles(1);
                }
            }
            result.add(stop);
            lastStop = stop;
        }
        return result;
    }

    public List<String> getStopovers() {
        return stopoversCache.get();
    }

    public List<String> getStopoversFrom(int startIndex) {
        List<String> predictions = new ArrayList<>();
        boolean startFound = false;
        for (int i = 0; i < this.predictions.get().size() - 1; i++) {
            TrainPrediction prediction = this.predictions.get().get(i);
            boolean wasStartFound = startFound;
            if (prediction.getEntryIndex() == startIndex) startFound = true;
            if (!wasStartFound) continue;
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
        return getFinalStop().map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x.getStationName()).getTagName().get()).orElse("?");
    }

    public String getDisplayTextStart() {
        return !isUsable() ? CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.not_in_service").getString() : getFirstStop().map(x -> GlobalSettings.getInstance().getOrCreateStationTagFor(x.getStationName()).getTagName().get()).orElse("?");
    }

    public String getStartStationName() {
        return getFirstStop().map(x -> x.getStationName()).orElse("?");
    }

    public String getDestinationStationName() {
        return getFinalStop().map(x -> x.getStationName()).orElse("?");
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
