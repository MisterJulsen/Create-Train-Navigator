package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.api.*;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;

import java.util.*;

public class AboutEndpoint implements IEndpointHandler {

    private record About(
            String minecraftVersion,
            String modId,
            String modName,
            String modVersion,
            String modHomepage,
            String modSources,
            String modIssueTracker,
            Collection<String> modLicense,
            String platform,
            int apiVersion,
            boolean devEnv,
            String environment,
            Collection<SimpleModEntry> modlist
    ) {}

    private record SimpleModEntry(
            String modId,
            String modName,
            String version
    ) {}

    @Override
    public Response handle(Request request) {
        Mod mod = Platform.getMod(CreateRailwaysNavigator.MOD_ID);
        return Response.json(new About(
                Platform.getMinecraftVersion(),
                CreateRailwaysNavigator.MOD_ID,
                mod.getName(),
                mod.getVersion(),
                mod.getHomepage().orElse(""),
                mod.getSources().orElse(""),
                mod.getIssueTracker().orElse(""),
                mod.getLicense(),
                (Platform.isForge() ? "Forge" : (Platform.isFabric() ? "Fabric" : "")),
                ApiVersion.latest().version(),
                Platform.isDevelopmentEnvironment(),
                Platform.getEnv().name().toLowerCase(Locale.ROOT),
                Platform.getMods().stream().map(x -> new SimpleModEntry(x.getModId(), x.getName(), x.getVersion())).toList()
        ));
    }
}