package me.videogamesm12.jrgrab.data;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
    private String repositoryUrl = null;

    @Builder.Default
    private String branch = null;

    @Builder.Default
    @Setter
    private List<String> channels = List.of("live");

    @Builder.Default
    private List<String> commonChannels = new ArrayList<>();

    @Builder.Default
    private boolean mac = false;

    @Builder.Default
    private boolean arm64 = false;

    @Builder.Default
    private boolean cjv = false;

    @Builder.Default
    private boolean fetchingManifestForFiles = true;

    private Destination destination;

    @Builder.Default
    private Source source = Source.DEPLOY_HISTORY;

    @Builder.Default
    private boolean includingUnavailable = false;

    @Builder.Default
    private boolean incremental = false;

    @Builder.Default
    private boolean assumeAvailableOnException = false;

    @Builder.Default
    private boolean detectCommonChannels = false;

    @Builder.Default
    private Aria2Configuration aria2 = Aria2Configuration.builder().build();

    @Builder.Default
    private List<String> manuallySpecifiedClients = new ArrayList<>();

    @Builder.Default
    private String file = null;

    public static JRGConfiguration fromArguments(final String[] args)
    {
        final OptionParser options = new OptionParser();
        options.accepts("help", "Prints this help message.").forHelp();
        options.accepts("domain", "The domain to use when grabbing clients. Unless you're attempting to scrape something like LuoBu, there isn't a need to change this.").withRequiredArg().describedAs("URL");
        options.accepts("branch", "The branch to use if using a GitHub-based scraper. You shouldn't need to change this unless the repository you're scraping has other branches you want to dig through.").withRequiredArg().describedAs("branch name");
        options.accepts("repository-url", "The repository to use if using a GitHub-based scraper. You shouldn't need to change this unless you're scraping a fork of a supported tracker.").withRequiredArg().describedAs("URL");
        options.accepts("common-channels", "The channels where \"common\" is used instead of the channel name to verify a client's availability and get its' files").withRequiredArg().describedAs("channel names separated by commas");
        options.accepts("channels", "The channels to grab clients from").withRequiredArg().describedAs("channel names separated by commas");
        options.accepts("destination", "Where the application will send all of its data to. Required.").requiredUnless("help").withRequiredArg().describedAs(StringUtils.join(Arrays.stream(Destination.values()).map(d -> d.name().toLowerCase()).toList(), ", ", ", or "));
        options.accepts("source", "Where the application will fetch clients from. Defaults to deploy_history.").withRequiredArg().describedAs(StringUtils.join(Arrays.stream(Source.values()).map(s -> s.name().toLowerCase()).toList(), ", ", ", or "));
        options.accepts("cjv", "Grab CJV clients.");
        options.accepts("mac", "Grab Mac clients.");
        options.accepts("mac-arm64", "If grabbing Mac clients, only grab arm64-based clients").availableIf("mac");
        options.accepts("bruteforce-files", "When fetching client files, use a hardcoded list of files instead of trying to find them from package manifests. Use in conjunction with --include-unavailable to bypass any other checks that may prevent them from being downloaded");
        options.accepts("clients", "Manually grab these specified clients if you use the \"manual\" source.").withRequiredArg().describedAs("[channel@]version-hash");
        options.accepts("include-unavailable", "Include clients that aren't available when sending them to the chosen destination.");
        options.accepts("assume-available-on-exception", "If an unexpected error occurs during availability checks, assume that the client is available and mark it as such.");
        options.accepts("detect-common-channels", "If the version availability check fails, check again with the common channel.");
        options.accepts("aria2-ip", "If using the \"aria2c\" destination, this sets the IP address for the daemon.").withRequiredArg();
        options.accepts("aria2-port", "If using the \"aria2c\" destination, this sets the port for the daemon.").withRequiredArg().ofType(int.class);
        options.accepts("aria2-token", "If using the \"aria2c\" destination, this sets the authentication token for the daemon (if one is present).").withRequiredArg();
        options.accepts("incremental", "Marks clients grabbed during this session as ones that shouldn't be grabbed in the future.");
        options.accepts("file", "Marks clients grabbed during this session as ones that shouldn't be grabbed in the future.").withRequiredArg();

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

            if (set.has("branch")) configBuilder = configBuilder.branch((String) set.valueOf("branch"));
            if (set.has("repository-url")) configBuilder = configBuilder.repositoryUrl((String) set.valueOf("repository-url"));
            if (set.has("domain")) configBuilder = configBuilder.domain((String) set.valueOf("domain"));
            if (set.has("common-channels")) configBuilder = configBuilder.commonChannels(Arrays.stream(((String) set.valueOf("common-channels")).split(",")).toList());
            if (set.has("channels")) configBuilder = configBuilder.channels(Arrays.stream(((String) set.valueOf("channels")).split(",")).toList());
            configBuilder = configBuilder.destination(Destination.valueOf(((String) set.valueOf("destination")).toUpperCase()));
            if (set.has("source"))
            {
                configBuilder = configBuilder.source(Source.valueOf(((String) set.valueOf("source")).toUpperCase()));
                if (configBuilder.source$value == Source.DEPLOY_HISTORY)
                {
                    configBuilder.file((String) set.valueOf("file"));
                }
            }
            if (set.has("cjv")) configBuilder = configBuilder.cjv(true);
            if (set.has("mac")) configBuilder = configBuilder.mac(true);
            if (set.has("mac-arm64")) configBuilder = configBuilder.arm64(true);
            if (set.has("bruteforce-files")) configBuilder = configBuilder.fetchingManifestForFiles(false);
            if (set.has("include-unavailable")) configBuilder = configBuilder.includingUnavailable(true);
            if (set.has("assume-available-on-exception")) configBuilder = configBuilder.assumeAvailableOnException(true);
            if (set.has("detect-common-channels")) configBuilder = configBuilder.detectCommonChannels(true);
            if (set.has("incremental")) configBuilder = configBuilder.incremental(true);
            if (set.has("clients")) configBuilder = configBuilder.manuallySpecifiedClients(Arrays.stream(((String) set.valueOf("clients")).split(",")).filter(str -> !str.isBlank() && !str.isEmpty()).toList());
            if (configBuilder.destination == Destination.ARIA2C)
            {
                Aria2Configuration.Aria2ConfigurationBuilder aria2ConfigBuilder = Aria2Configuration.builder();
                if (set.has("aria2-ip")) aria2ConfigBuilder = aria2ConfigBuilder.ipAddress((String) set.valueOf("aria2-ip"));
                if (set.has("aria2-port")) aria2ConfigBuilder = aria2ConfigBuilder.port(Math.min(Math.max((int) set.valueOf("aria2-port"), 0), 65535));
                if (set.has("aria2-token")) aria2ConfigBuilder = aria2ConfigBuilder.token((String) set.valueOf("aria2-token"));
                configBuilder.aria2(aria2ConfigBuilder.build());
            }

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
        @Builder.Default
        private String token = "";
    }

    public enum Source
    {
        CLIENT_SETTINGS,
        CLONETROOPER_GITHUB_TRACKER,
        DEPLOY_HISTORY,
        JSON,
        LEGACY,
        MANUAL,
        MATT_GITHUB_TRACKER,
        SNC_GITHUB_TRACKER;
    }

    public enum Destination
    {
        FILE_LIST,
        OPTIMIZED_ARIA2C,
        ARIA2C,
        DEPLOY_HISTORY,
        JSON,
        SPREADSHEET,
        URL_LIST,
    }
}
