package de.mrjulsen.crn.api.core.notice;

import java.util.List;

/**
 * Supplies notices about stations, shown on the displays of a station in addition to the trains, or in their place
 * where no train calls (for example while the station is closed).
 * <p>
 * Implement this if your mod knows something about a station that passengers should read there, then register it with
 * {@link StationNoticeProviderRegistry}. Notices about a single train belong to a delay cause instead.
 * <p>
 * Implementations are called whenever a display refreshes its data, off the server thread, so they must be quick, must
 * not block and must tolerate concurrent calls. An implementation that throws is skipped for that refresh.
 */
@FunctionalInterface
public interface IStationNoticeProvider {

    /**
     * The notices for the station a display shows, in the order they should appear; may be empty. Returning
     * {@code null} is treated as no notices.
     *
     * @param stationName The station filter of the display, as entered on its display link; may be a glob pattern.
     */
    List<StationNotice> getNotices(String stationName);
}
