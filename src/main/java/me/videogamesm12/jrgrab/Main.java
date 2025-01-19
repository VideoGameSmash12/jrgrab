package me.videogamesm12.jrgrab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.destinations.*;
import me.videogamesm12.jrgrab.grabbers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class Main
{
    @Getter
    private static final Logger logger = LoggerFactory.getLogger("jrgrab");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args)
    {
        System.out.println("""
                     _                   _   \s
                    (_)_ _ __ _ _ _ __ _| |__\s
                    | | '_/ _` | '_/ _` | '_ \\
                   _/ |_| \\__, |_| \\__,_|_.__/
                  |__/    |___/""");
        System.out.println("__--======================--__");

        JRGConfiguration configuration = JRGConfiguration.fromArguments(args);
        if (configuration == null)
        {
            return;
        }

        final Map<String, List<String>> known = new HashMap<>();
        final File knownFile = new File("known.json");
        if (configuration.isIncremental() && knownFile.exists())
        {
            getLogger().info("Loading known client database");
            try (BufferedReader reader = Files.newBufferedReader(knownFile.toPath()))
            {
                known.putAll(gson.fromJson(reader, new TypeToken<Map<String, ArrayList<String>>>(){}.getType()));
                getLogger().info("Loaded database");
            }
            catch (IOException ex)
            {
                getLogger().error("Failed to load client database", ex);
            }
        }

        getLogger().info("Setting up grabber");
        final AbstractGrabber grabber = switch(configuration.getSource())
        {
            case CLIENT_SETTINGS -> new ClientSettingsGrabber(configuration);
            case CLONETROOPER_GITHUB_TRACKER -> new ClonetrooperGitHubTrackerGrabber(configuration);
            case DEPLOY_HISTORY -> new DeployGrabber(configuration);
            case JSON -> new JsonGrabber(configuration);
            case LEGACY -> new LegacyGrabber(configuration);
            case MANUAL -> new ManualGrabber(configuration);
            case MATT_GITHUB_TRACKER -> new MattGitHubTrackerGrabber(configuration);
            case SNC_GITHUB_TRACKER -> new SNCGitHubTrackerGrabber(configuration);
        };
        getLogger().info("Setting up destination");
        final AbstractDestination destination = switch(configuration.getDestination())
        {
            case FILE_LIST -> new FileListDestination(configuration);
            case OPTIMIZED_ARIA2C -> new OptimizedAria2Destination(configuration);
            case ARIA2C -> new Aria2Destination(configuration);
            case DEPLOY_HISTORY -> new DeployHistoryDestination(configuration);
            case JSON -> new JsonDestination(configuration);
            case SPREADSHEET -> new SpreadsheetDestination(configuration);
            case URL_LIST -> new UrlListDestination(configuration);
        };

        grabber.setup();
        getLogger().info("Grabbing channels");
        configuration.getChannels().forEach(channel ->
        {
            if (!known.containsKey(channel))
            {
                known.put(channel, new ArrayList<>());
            }

            List<String> k = known.get(channel);
            List<RBXVersion> clients = grabber.getVersions(channel, k).stream().filter(Objects::nonNull).toList();

            if (configuration.isIncremental())
            {
                k.addAll(clients.stream().map(RBXVersion::getVersionHash).toList());
            }

            destination.sendVersions(clients,  channel);
        });

        if (configuration.isIncremental())
        {
            getLogger().info("Saving known client database to disk");

            try (BufferedWriter writer = Files.newBufferedWriter(new File("known.json").toPath()))
            {
                gson.toJson(known, writer);
            }
            catch (IOException ex)
            {
                Main.getLogger().error("Failed to write known client database to disk", ex);
            }
        }

        getLogger().info("Done");
    }
}