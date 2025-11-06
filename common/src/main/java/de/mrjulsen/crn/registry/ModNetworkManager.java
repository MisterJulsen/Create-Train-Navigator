package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.network.packets.cts.AdvancedDisplayUpdatePacketData;
import de.mrjulsen.crn.network.packets.pain.AddStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.AddStationToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.AddTrainToBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.AllTrainsInitializedPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.CreateTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.DeleteTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.EmptyNetworkPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllBlacklistedStationsPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllBlacklistedTrainsPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllStationNamesPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllStationTagsPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllStationsAsTagsPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllTrainCategoriesPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllTrainLinesPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllTrainNamesPacketData;
import de.mrjulsen.crn.network.packets.pain.GetAllTrainsDebugPacketData;
import de.mrjulsen.crn.network.packets.pain.GetDepartureAndArrivalRoutesAtPacketData;
import de.mrjulsen.crn.network.packets.pain.GetDeparturesAtPacketData;
import de.mrjulsen.crn.network.packets.pain.GetNearestStationPacketData;
import de.mrjulsen.crn.network.packets.pain.GetNextConnectionsDisplayDataPacketData;
import de.mrjulsen.crn.network.packets.pain.GetOnlinePlayersPacketData;
import de.mrjulsen.crn.network.packets.pain.GetStationDepartureHistoryPacketData;
import de.mrjulsen.crn.network.packets.pain.GetTrainCategoryPacketData;
import de.mrjulsen.crn.network.packets.pain.GetTrainDisplayDataPacketData;
import de.mrjulsen.crn.network.packets.pain.GetTrainLinePacketData;
import de.mrjulsen.crn.network.packets.pain.GetUserSettingsPacketData;
import de.mrjulsen.crn.network.packets.pain.NavigatePacketData;
import de.mrjulsen.crn.network.packets.pain.RegisterStationTagPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.RemoveTrainFromBlacklistPacketData;
import de.mrjulsen.crn.network.packets.pain.SaveUserSettingsPacketData;
import de.mrjulsen.crn.network.packets.pain.ShowTrainDebugScreenPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagRequestByTagPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagRequestPacketData;
import de.mrjulsen.crn.network.packets.pain.StationTagUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainCategoryUpdatePermissionsPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainHardResetPacketData;
import de.mrjulsen.crn.network.packets.pain.TrainSoftResetPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateRealtimePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagEntryPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateStationTagNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainCategoryNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineColorPacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLineNamePacketData;
import de.mrjulsen.crn.network.packets.pain.UpdateTrainLinePermissionsPacketData;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacketData;
import de.mrjulsen.mcdragonlib.network.DLNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketType;
import de.mrjulsen.mcdragonlib.util.DLUtils;

public final class ModNetworkManager {
    private ModNetworkManager() {}

    public static final void init() {}

    public static final DLNetworkManager NETWORK = new DLNetworkManager(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "network"), "1");

    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, StationTagRequestPacketData.Request, StationTagRequestPacketData.Response> GET_STATION_TAG = NETWORK.registerSendAndReceivePacket("get_station_tag", NetworkDirection.C2S, StationTagRequestPacketData::handle, StationTagRequestPacketData.Request::new, StationTagRequestPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, StationTagRequestByTagPacketData.Request, StationTagRequestByTagPacketData.Response> GET_STATION_TAG_BY_TAG = NETWORK.registerSendAndReceivePacket("get_station_tag_by_tag", NetworkDirection.C2S, StationTagRequestByTagPacketData::handle, StationTagRequestByTagPacketData.Request::new, StationTagRequestByTagPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, CreateStationTagPacketData.Request, CreateStationTagPacketData.Response> CREATE_STATION_TAG = NETWORK.registerSendAndReceivePacket("create_station_tag", NetworkDirection.C2S, CreateStationTagPacketData::handle, CreateStationTagPacketData.Request::new, CreateStationTagPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, StationTagUpdatePermissionsPacketData.Request, StationTagUpdatePermissionsPacketData.Response> UPDATE_STATION_TAG_PERMISSIONS = NETWORK.registerSendAndReceivePacket("update_station_tag_permissions", NetworkDirection.C2S, StationTagUpdatePermissionsPacketData::handle, StationTagUpdatePermissionsPacketData.Request::new, StationTagUpdatePermissionsPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, AddStationTagEntryPacketData.Request, AddStationTagEntryPacketData.Response> ADD_STATION_TAG_ENTRY = NETWORK.registerSendAndReceivePacket("add_station_tag_entry", NetworkDirection.C2S, AddStationTagEntryPacketData::handle, AddStationTagEntryPacketData.Request::new, AddStationTagEntryPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateStationTagEntryPacketData.Request, UpdateStationTagEntryPacketData.Response> UPDATE_STATION_TAG_ENTRY = NETWORK.registerSendAndReceivePacket("update_station_tag_entry", NetworkDirection.C2S, UpdateStationTagEntryPacketData::handle, UpdateStationTagEntryPacketData.Request::new, UpdateStationTagEntryPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, RemoveStationTagEntryPacketData.Request, RemoveStationTagEntryPacketData.Response> REMOVE_STATION_TAG_ENTRY = NETWORK.registerSendAndReceivePacket("remove_station_tag_entry", NetworkDirection.C2S, RemoveStationTagEntryPacketData::handle, RemoveStationTagEntryPacketData.Request::new, RemoveStationTagEntryPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetAllStationsAsTagsPacketData.Request, GetAllStationsAsTagsPacketData.Response> GET_ALL_STATIONS_AS_STATION_TAGS = NETWORK.registerSendAndReceivePacket("get_all_stations_as_station_tags", NetworkDirection.C2S, GetAllStationsAsTagsPacketData::handle, GetAllStationsAsTagsPacketData.Request::new, GetAllStationsAsTagsPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetTrainCategoryPacketData.Request, GetTrainCategoryPacketData.Response> GET_TRAIN_CATEGORY = NETWORK.registerSendAndReceivePacket("get_train_category", NetworkDirection.C2S, GetTrainCategoryPacketData::handle, GetTrainCategoryPacketData.Request::new, GetTrainCategoryPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateTrainCategoryNamePacketData.Request, UpdateTrainCategoryNamePacketData.Response> UPDATE_TRAIN_CATEGORY_NAME = NETWORK.registerSendAndReceivePacket("update_train_category_name", NetworkDirection.C2S, UpdateTrainCategoryNamePacketData::handle, UpdateTrainCategoryNamePacketData.Request::new, UpdateTrainCategoryNamePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, CreateTrainCategoryPacketData.Request, CreateTrainCategoryPacketData.Response> CREATE_TRAIN_CATEGORY = NETWORK.registerSendAndReceivePacket("create_train_category", NetworkDirection.C2S, CreateTrainCategoryPacketData::handle, CreateTrainCategoryPacketData.Request::new, CreateTrainCategoryPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, TrainCategoryUpdatePermissionsPacketData.Request, TrainCategoryUpdatePermissionsPacketData.Response> UPDATE_TRAIN_CATEGORY_PERMISSIONS = NETWORK.registerSendAndReceivePacket("update_train_category_permissions", NetworkDirection.C2S, TrainCategoryUpdatePermissionsPacketData::handle, TrainCategoryUpdatePermissionsPacketData.Request::new, TrainCategoryUpdatePermissionsPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, AddStationToBlacklistPacketData.Request, AddStationToBlacklistPacketData.Response> ADD_STATION_TO_BLACKLIST = NETWORK.registerSendAndReceivePacket("add_station_to_blacklist", NetworkDirection.C2S, AddStationToBlacklistPacketData::handle, AddStationToBlacklistPacketData.Request::new, AddStationToBlacklistPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, RemoveStationFromBlacklistPacketData.Request, RemoveStationFromBlacklistPacketData.Response> REMOVE_STATION_FROM_BLACKLIST = NETWORK.registerSendAndReceivePacket("remove_station_from_blacklist", NetworkDirection.C2S, RemoveStationFromBlacklistPacketData::handle, RemoveStationFromBlacklistPacketData.Request::new, RemoveStationFromBlacklistPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, AddTrainToBlacklistPacketData.Request, AddTrainToBlacklistPacketData.Response> ADD_TRAIN_TO_BLACKLIST = NETWORK.registerSendAndReceivePacket("add_train_to_blacklist", NetworkDirection.C2S, AddTrainToBlacklistPacketData::handle, AddTrainToBlacklistPacketData.Request::new, AddTrainToBlacklistPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, RemoveTrainFromBlacklistPacketData.Request, RemoveTrainFromBlacklistPacketData.Response> REMOVE_TRAIN_FROM_BLACKLIST = NETWORK.registerSendAndReceivePacket("remove_train_from_blacklist", NetworkDirection.C2S, RemoveTrainFromBlacklistPacketData::handle, RemoveTrainFromBlacklistPacketData.Request::new, RemoveTrainFromBlacklistPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateRealtimePacketData.Request, UpdateRealtimePacketData.Response> UPDATE_REALTIME = NETWORK.registerSendAndReceivePacket("update_realtime", NetworkDirection.C2S, UpdateRealtimePacketData::handle, UpdateRealtimePacketData.Request::new, UpdateRealtimePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetUserSettingsPacketData.Request, GetUserSettingsPacketData.Response> GET_USER_SETTINGS = NETWORK.registerSendAndReceivePacket("get_user_settings", NetworkDirection.C2S, GetUserSettingsPacketData::handle, GetUserSettingsPacketData.Request::new, GetUserSettingsPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetNearestStationPacketData.Request, GetNearestStationPacketData.Response> GET_NEAREST_STATION = NETWORK.registerSendAndReceivePacket("get_nearest_station", NetworkDirection.C2S, GetNearestStationPacketData::handle, GetNearestStationPacketData.Request::new, GetNearestStationPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetTrainDisplayDataPacketData.Request, GetTrainDisplayDataPacketData.Response> GET_TRAIN_DISPLAY_DATA = NETWORK.registerSendAndReceivePacket("get_train_display_data", NetworkDirection.C2S, GetTrainDisplayDataPacketData::handle, GetTrainDisplayDataPacketData.Request::new, GetTrainDisplayDataPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetNextConnectionsDisplayDataPacketData.Request, GetNextConnectionsDisplayDataPacketData.Response> GET_NEXT_CONNECTIONS_DISPLAY_DATA = NETWORK.registerSendAndReceivePacket("get_next_connections_display_data", NetworkDirection.C2S, GetNextConnectionsDisplayDataPacketData::handle, GetNextConnectionsDisplayDataPacketData.Request::new, GetNextConnectionsDisplayDataPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetTrainLinePacketData.Request, GetTrainLinePacketData.Response> GET_TRAIN_LINE = NETWORK.registerSendAndReceivePacket("get_train_line", NetworkDirection.C2S, GetTrainLinePacketData::handle, GetTrainLinePacketData.Request::new, GetTrainLinePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateTrainLineNamePacketData.Request, UpdateTrainLineNamePacketData.Response> UPDATE_TRAIN_LINE_NAME = NETWORK.registerSendAndReceivePacket("update_train_line_name", NetworkDirection.C2S, UpdateTrainLineNamePacketData::handle, UpdateTrainLineNamePacketData.Request::new, UpdateTrainLineNamePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, CreateTrainLinePacketData.Request, CreateTrainLinePacketData.Response> CREATE_TRAIN_LINE = NETWORK.registerSendAndReceivePacket("create_train_line", NetworkDirection.C2S, CreateTrainLinePacketData::handle, CreateTrainLinePacketData.Request::new, CreateTrainLinePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateTrainLinePermissionsPacketData.Request, UpdateTrainLinePermissionsPacketData.Response> UPDATE_TRAIN_LINE_PERMISSIONS = NETWORK.registerSendAndReceivePacket("update_train_line_permissions", NetworkDirection.C2S, UpdateTrainLinePermissionsPacketData::handle, UpdateTrainLinePermissionsPacketData.Request::new, UpdateTrainLinePermissionsPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, NavigatePacketData.Request, NavigatePacketData.Response> NAVIGATE = NETWORK.registerSendAndReceivePacket("navigate", NetworkDirection.C2S, NavigatePacketData::handle, NavigatePacketData.Request::new, NavigatePacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetDepartureAndArrivalRoutesAtPacketData.Request, GetDepartureAndArrivalRoutesAtPacketData.Response> GET_DEPARTURE_AND_ARRIVAL_ROUTES_AT = NETWORK.registerSendAndReceivePacket("get_departure_and_arrival_routes_at", NetworkDirection.C2S, GetDepartureAndArrivalRoutesAtPacketData::handle, GetDepartureAndArrivalRoutesAtPacketData.Request::new, GetDepartureAndArrivalRoutesAtPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetDeparturesAtPacketData.Request, GetDeparturesAtPacketData.Response> GET_DEPARTURES_AT = NETWORK.registerSendAndReceivePacket("get_departures_at", NetworkDirection.C2S, GetDeparturesAtPacketData::handle, GetDeparturesAtPacketData.Request::new, GetDeparturesAtPacketData.Response::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, GetStationDepartureHistoryPacketData.Request, GetStationDepartureHistoryPacketData.Response> GET_STATIONDEPARTURE_HISTORY = NETWORK.registerSendAndReceivePacket("get_station_departure_history", NetworkDirection.C2S, GetStationDepartureHistoryPacketData::handle, GetStationDepartureHistoryPacketData.Request::new, GetStationDepartureHistoryPacketData.Response::new);
    
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, RegisterStationTagPacketData, EmptyNetworkPacketData> REGISTER_STATION_TAG = NETWORK.registerSendAndReceivePacket("register_station_tag", NetworkDirection.C2S, RegisterStationTagPacketData::handle, RegisterStationTagPacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, DeleteStationTagPacketData, EmptyNetworkPacketData> DELETE_STATION_TAG = NETWORK.registerSendAndReceivePacket("delete_station_tag", NetworkDirection.C2S, DeleteStationTagPacketData::handle, DeleteStationTagPacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateStationTagNamePacketData, EmptyNetworkPacketData> UPDATE_STATION_TAG_NAME = NETWORK.registerSendAndReceivePacket("update_station_tag_name", NetworkDirection.C2S, UpdateStationTagNamePacketData::handle, UpdateStationTagNamePacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, DeleteTrainCategoryPacketData, EmptyNetworkPacketData> DELETE_TRAIN_CATEGORY = NETWORK.registerSendAndReceivePacket("delete_train_category", NetworkDirection.C2S, DeleteTrainCategoryPacketData::handle, DeleteTrainCategoryPacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateTrainCategoryColorPacketData, EmptyNetworkPacketData> UPDATE_TRAIN_CATEGORY_COLOR = NETWORK.registerSendAndReceivePacket("update_train_category_color", NetworkDirection.C2S, UpdateTrainCategoryColorPacketData::handle, UpdateTrainCategoryColorPacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, DeleteTrainLinePacketData, EmptyNetworkPacketData> DELETE_TRAIN_LINE = NETWORK.registerSendAndReceivePacket("delete_train_line", NetworkDirection.C2S, DeleteTrainLinePacketData::handle, DeleteTrainLinePacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, UpdateTrainLineColorPacketData, EmptyNetworkPacketData> UPDATE_TRAIN_LINE_COLOR = NETWORK.registerSendAndReceivePacket("update_train_line_color", NetworkDirection.C2S, UpdateTrainLineColorPacketData::handle, UpdateTrainLineColorPacketData::new, EmptyNetworkPacketData::new);
    public static final NetworkPacketType.SendAndReceive<NetworkDirection.C2S, SaveUserSettingsPacketData, EmptyNetworkPacketData> SAVE_USER_SETTINGS = NETWORK.registerSendAndReceivePacket("save_user_settings", NetworkDirection.C2S, SaveUserSettingsPacketData::handle, SaveUserSettingsPacketData::new, EmptyNetworkPacketData::new);

    public static final NetworkPacketType.Send<NetworkDirection.C2S, TrainSoftResetPacketData> TRAIN_SOFT_RESET = NETWORK.registerSendOnlyPacket("train_soft_reset", NetworkDirection.C2S, TrainSoftResetPacketData::handle, TrainSoftResetPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.C2S, TrainHardResetPacketData> TRAIN_HARD_RESET = NETWORK.registerSendOnlyPacket("train_hard_reset", NetworkDirection.C2S, TrainHardResetPacketData::handle, TrainHardResetPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.C2S, AdvancedDisplayUpdatePacketData> ADVANCED_DISPLAY_UPDATE_PACKET = NETWORK.registerSendOnlyPacket("advanced_display_update_packet", NetworkDirection.C2S, AdvancedDisplayUpdatePacketData::handle, AdvancedDisplayUpdatePacketData::new);

    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllStationTagsPacketData> GET_ALL_STATION_TAGS = NETWORK.registerReceiveOnlyPacket("get_all_station_tags", NetworkDirection.C2S, GetAllStationTagsPacketData::handle, GetAllStationTagsPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllTrainCategoriesPacketData> GET_ALL_TRAIN_CATEGORIES = NETWORK.registerReceiveOnlyPacket("get_all_train_categories", NetworkDirection.C2S, GetAllTrainCategoriesPacketData::handle, GetAllTrainCategoriesPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllTrainLinesPacketData> GET_ALL_TRAIN_LINES = NETWORK.registerReceiveOnlyPacket("get_all_train_lines", NetworkDirection.C2S, GetAllTrainLinesPacketData::handle, GetAllTrainLinesPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllBlacklistedStationsPacketData> GET_ALL_BLACKLISTED_STATIONS = NETWORK.registerReceiveOnlyPacket("get_all_blacklisted_stations", NetworkDirection.C2S, GetAllBlacklistedStationsPacketData::handle, GetAllBlacklistedStationsPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllBlacklistedTrainsPacketData> GET_ALL_BLACKLISTED_TRAINS = NETWORK.registerReceiveOnlyPacket("get_all_blacklisted_trains", NetworkDirection.C2S, GetAllBlacklistedTrainsPacketData::handle, GetAllBlacklistedTrainsPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllTrainNamesPacketData> GET_ALL_TRAIN_NAMES = NETWORK.registerReceiveOnlyPacket("get_all_train_names", NetworkDirection.C2S, GetAllTrainNamesPacketData::handle, GetAllTrainNamesPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllStationNamesPacketData> GET_ALL_STATION_NAMES = NETWORK.registerReceiveOnlyPacket("get_all_station_names", NetworkDirection.C2S, GetAllStationNamesPacketData::handle, GetAllStationNamesPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, AllTrainsInitializedPacketData> ALL_TRAINS_INITIALIZED = NETWORK.registerReceiveOnlyPacket("all_trains_initialized", NetworkDirection.C2S, AllTrainsInitializedPacketData::handle, AllTrainsInitializedPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetAllTrainsDebugPacketData> GET_ALL_TRAINS_DEBUG_DATA = NETWORK.registerReceiveOnlyPacket("get_all_trains_debug_data", NetworkDirection.C2S, GetAllTrainsDebugPacketData::handle, GetAllTrainsDebugPacketData::new);
    public static final NetworkPacketType.Receive<NetworkDirection.C2S, GetOnlinePlayersPacketData> GET_ONLINE_PLAYERS = NETWORK.registerReceiveOnlyPacket("get_online_players", NetworkDirection.C2S, GetOnlinePlayersPacketData::handle, GetOnlinePlayersPacketData::new);


    public static final NetworkPacketType.Send<NetworkDirection.S2C, ShowTrainDebugScreenPacketData> SHOW_TRAIN_DEBUG_SCREEN = NETWORK.registerSendOnlyPacket("show_train_debug_screen", NetworkDirection.S2C, ShowTrainDebugScreenPacketData::handle, ShowTrainDebugScreenPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.S2C, ServerErrorPacketData> SERVER_ERROR = NETWORK.registerSendOnlyPacket("server_error", NetworkDirection.S2C, ServerErrorPacketData::handle, ServerErrorPacketData::new);


}
