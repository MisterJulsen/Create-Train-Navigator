package de.mrjulsen.crn.core.schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.ChangeTitleInstruction;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;

import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.schedule.instruction.ResetTimingsInstruction;
import de.mrjulsen.crn.data.schedule.instruction.TravelSectionInstruction;

public final class JourneyParser {

    private record TitleMarker(int entryIndex, String title) {}

    private JourneyParser() {}

    public static TrainJourney parse(Train train) {
        Schedule schedule = train != null && train.runtime != null ? train.runtime.getSchedule() : null;
        if (schedule == null || schedule.entries == null || schedule.entries.isEmpty()) {
            return TrainJourney.empty(train == null ? null : train.id);
        }

        final int entryCount = schedule.entries.size();

        List<JourneyStop> stops = new ArrayList<>();
        List<JourneySection> sectionMarkers = new ArrayList<>();
        List<TitleMarker> titleMarkers = new ArrayList<>();
        Set<Integer> resetTimingEntries = new HashSet<>();
        boolean flexibleDwellTimes = false;

        for (int i = 0; i < entryCount; i++) {
            ScheduleEntry entry = schedule.entries.get(i);
            if (entry.instruction instanceof DestinationInstruction destination) {
                stops.add(new JourneyStop(i, stops.size(), destination.getFilter(), entry));
            } else if (entry.instruction instanceof ChangeTitleInstruction title) {
                titleMarkers.add(new TitleMarker(i, title.getScheduleTitle()));
            } else if (entry.instruction instanceof TravelSectionInstruction sectionInstruction) {
                sectionMarkers.add(new JourneySection(
                        sectionMarkers.size(),
                        i,
                        sectionInstruction.getTrainCategoryId(),
                        sectionInstruction.getTrainLineId(),
                        sectionInstruction.shouldIncludePreviousStationStop(),
                        sectionInstruction.isSectionUsable()
                ));
            } else if (entry.instruction instanceof ResetTimingsInstruction) {
                resetTimingEntries.add(i);
            }

            if (!flexibleDwellTimes && entry.conditions != null) {
                for (List<ScheduleWaitCondition> group : entry.conditions) {
                    for (ScheduleWaitCondition condition : group) {
                        if (condition instanceof DynamicDelayCondition c && c.minWaitTicks() < c.totalWaitTicks()) {
                            flexibleDwellTimes = true;
                        }
                    }
                }
            }
        }

        if (sectionMarkers.isEmpty()) {
            sectionMarkers.add(JourneySection.def());
        }

        assignStops(stops, sectionMarkers, titleMarkers);

        return new TrainJourney(train.id, schedule, stops, sectionMarkers, resetTimingEntries, schedule.cyclic, flexibleDwellTimes);
    }

    private static void assignStops(List<JourneyStop> stops, List<JourneySection> sectionMarkers, List<TitleMarker> titleMarkers) {
        for (JourneyStop stop : stops) {
            JourneySection selectedSection = sectionMarkers.get(sectionMarkers.size() - 1);
            for (JourneySection section : sectionMarkers) {
                if (section.entryIndex() > stop.entryIndex()) {
                    break;
                }
                selectedSection = section;
            }

            String selectedTitle = "";
            for (TitleMarker title : titleMarkers) {
                if (title.entryIndex() > stop.entryIndex()) {
                    break;
                }
                selectedTitle = title.title();
            }

            selectedSection.addStop(stop);
            stop.setSection(selectedSection);
            stop.setTitle(selectedTitle);
        }
    }

    public static boolean isOutdated(TrainJourney journey, Train train) {
        Schedule schedule = train != null && train.runtime != null ? train.runtime.getSchedule() : null;
        if (journey == null) {
            return true;
        }
        if (schedule == null) {
            return journey.getSchedule() != null;
        }
        return journey.getSchedule() != schedule;
    }
}
