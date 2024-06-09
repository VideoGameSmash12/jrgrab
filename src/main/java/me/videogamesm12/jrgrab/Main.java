package me.videogamesm12.jrgrab;

import lombok.Getter;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.destinations.AbstractDestination;
import me.videogamesm12.jrgrab.destinations.Aria2Destination;
import me.videogamesm12.jrgrab.destinations.DeployHistoryDestination;
import me.videogamesm12.jrgrab.destinations.JsonDestination;
import me.videogamesm12.jrgrab.grabbers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    @Getter
    private static final Logger logger = LoggerFactory.getLogger("jrgrab");

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

        getLogger().info("Setting up grabber");
        final AbstractGrabber grabber = switch(configuration.getSource())
        {
            case CLIENT_SETTINGS -> new ClientSettingsGrabber(configuration);
            case DEPLOY_HISTORY -> new DeployGrabber(configuration);
            case GITHUB_TRACKER -> new TrackerGitHubGrabber(configuration);
            case JSON -> new JsonGrabber(configuration);
        };
        getLogger().info("Setting up destination");
        final AbstractDestination destination = switch(configuration.getDestination())
        {
            case ARIA2C -> new Aria2Destination(configuration);
            case DEPLOY_HISTORY -> new DeployHistoryDestination(configuration);
            case JSON -> new JsonDestination(configuration);
        };

        grabber.setup();
        getLogger().info("Grabbing channels");
        configuration.getChannels().forEach(channel -> destination.sendVersions(grabber.getVersions(channel), channel));
        getLogger().info("Done");
    }
}