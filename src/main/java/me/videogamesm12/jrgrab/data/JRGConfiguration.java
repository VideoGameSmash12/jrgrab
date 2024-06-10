package me.videogamesm12.jrgrab.data;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Builder;
import lombok.Getter;
import me.videogamesm12.jrgrab.Main;
import org.eclipse.jgit.util.StringUtils;

import java.io.IOException;
import java.util.*;

@Builder
@Getter
public class JRGConfiguration
{
    @Builder.Default
    private String domain = "setup.rbxcdn.com";

    @Builder.Default
    private List<String> channels = List.of("live");

    @Builder.Default
    private boolean mac = false;

    private Destination destination;

    @Builder.Default
    private Source source = Source.DEPLOY_HISTORY;

    @Builder.Default
    private boolean includingUnavailable = false;

    @Builder.Default
    private Aria2Configuration aria2 = Aria2Configuration.builder().build();

    @Builder.Default
    private List<String> manuallySpecifiedClients = new ArrayList<>();

    public static JRGConfiguration fromArguments(final String[] args)
    {
        final OptionParser options = new OptionParser();
        options.accepts("help", "Prints this help message.").forHelp();
        options.accepts("domain", "The domain to use when grabbing clients. Unless you're attempting to scrape something like LuoBu, there isn't a need to change this.").withRequiredArg().describedAs("URL");
        options.accepts("channels", "The channels to grab clients from").withRequiredArg().describedAs("channel names separated by commas");
        options.accepts("destination", "Where the application will send all of its data to. Required.").requiredUnless("help").withRequiredArg().describedAs(StringUtils.join(Arrays.stream(Destination.values()).map(d -> d.name().toLowerCase()).toList(), ", ", ", or "));
        options.accepts("source", "Where the application will fetch clients from. Defaults to deploy_history.").withRequiredArg().describedAs(StringUtils.join(Arrays.stream(Source.values()).map(s -> s.name().toLowerCase()).toList(), ", ", ", or "));
        options.accepts("mac", "Grab Mac clients.");
        options.accepts("clients", "Manually grab these specified clients if you use the \"manual\" source.").withRequiredArg().describedAs("[channel@]version-hash");
        options.accepts("include-unavailable", "Include clients that aren't available when sending them to the chosen destination.");

        try
        {
            final OptionSet set = options.parse(args);

            if (set.has("help"))
            {
                try
                {
                    options.printHelpOn(System.console().writer());
                }
                catch (IOException ex)
                {
                    Main.getLogger().error("Failed to write help to console", ex);
                }

                return null;
            }

            JRGConfigurationBuilder configBuilder = builder();

            if (set.has("domain")) configBuilder = configBuilder.domain((String) set.valueOf("domain"));
            if (set.has("channels")) configBuilder = configBuilder.channels(Arrays.stream(((String) set.valueOf("channels")).split(",")).toList());
            configBuilder = configBuilder.destination(Destination.valueOf(((String) set.valueOf("destination")).toUpperCase()));
            if (set.has("source")) configBuilder = configBuilder.source(Source.valueOf(((String) set.valueOf("source")).toUpperCase()));
            if (set.has("mac")) configBuilder = configBuilder.mac(true);
            if (set.has("include-unavailable")) configBuilder = configBuilder.includingUnavailable(true);
            if (set.has("clients")) configBuilder = configBuilder.manuallySpecifiedClients(Arrays.stream(((String) set.valueOf("clients")).split(",")).filter(str -> !str.isBlank() && !str.isEmpty()).toList());

            return configBuilder.build();
        }
        catch (OptionException ex)
        {
            try
            {
                Main.getLogger().error("Error - {}", ex.getMessage());
                options.printHelpOn(System.console().writer());
            }
            catch (IOException io)
            {
                Main.getLogger().error("Failed to write help to console", io);
            }
            return null;
        }
    }

    @Builder
    @Getter
    public static class Aria2Configuration
    {
        @Builder.Default
        private String ipAddress = "127.0.0.1";
        @Builder.Default
        private int port = 6800;
    }

    public enum Source
    {
        CLIENT_SETTINGS,
        CLONETROOPER_GITHUB_TRACKER,
        DEPLOY_HISTORY,
        JSON,
        LEGACY,
        MANUAL,
        MATT_GITHUB_TRACKER;
    }

    public enum Destination
    {
        ARIA2C,
        DEPLOY_HISTORY,
        JSON
    }
}
