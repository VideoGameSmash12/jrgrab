package me.videogamesm12.jrgrab.data;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Builder;
import lombok.Getter;
import me.videogamesm12.jrgrab.Main;

import java.io.IOException;

@Builder
@Getter
public class JRGConfiguration
{
    @Builder.Default
    private String domain = "setup.rbxcdn.com";

    @Builder.Default
    private String channel = "live";

    @Builder.Default
    private boolean mac = false;

    private Destination destination;

    @Builder.Default
    private Source source = Source.DEPLOY_HISTORY;

    @Builder.Default
    private boolean includingUnavailable = false;

    @Builder.Default
    private Aria2Configuration aria2 = Aria2Configuration.builder().build();

    public static JRGConfiguration fromArguments(final String[] args)
    {
        final OptionParser options = new OptionParser();
        options.accepts("help", "Prints this help message.").forHelp();
        options.accepts("domain", "The domain to use when grabbing clients. Unless you're attempting to scrape something like LuoBu, there isn't a need to change this.").withRequiredArg().describedAs("URL");
        options.accepts("channel", "The channel to grab clients from").withRequiredArg().describedAs("channel name");
        options.accepts("destination", "Where the application will send all of its data to. Required.").requiredUnless("help").withRequiredArg().describedAs("json or aria2c");
        options.accepts("source", "Where the application will fetch clients from.").withRequiredArg().describedAs("client_settings or deploy_history");
        options.accepts("mac", "Grab Mac clients.");
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
            if (set.has("channel")) configBuilder = configBuilder.channel((String) set.valueOf("channel"));
            configBuilder = configBuilder.destination(Destination.valueOf(((String) set.valueOf("destination")).toUpperCase()));
            if (set.has("source")) configBuilder = configBuilder.source(Source.valueOf(((String) set.valueOf("source")).toUpperCase()));
            if (set.has("mac")) configBuilder = configBuilder.mac(true);
            if (set.has("include-unavailable")) configBuilder = configBuilder.includingUnavailable(true);

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
        DEPLOY_HISTORY;
    }

    public enum Destination
    {
        ARIA2C,
        DEPLOY_HISTORY,
        JSON
    }
}
