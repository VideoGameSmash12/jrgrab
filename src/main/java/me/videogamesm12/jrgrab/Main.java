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

import java.util.stream.Stream;

public class Main
{
    @Getter
    private static final Logger logger = LoggerFactory.getLogger("jrgrab");

    public static void main(String[] args)
    {
        JRGConfiguration configuration = JRGConfiguration.fromArguments(args);
        if (configuration == null)
        {
            return;
        }

        final AbstractGrabber grabber = switch(configuration.getSource())
        {
            case CLIENT_SETTINGS -> new ClientSettingsGrabber(configuration);
            case DEPLOY_HISTORY -> new DeployGrabber(configuration);
            case GITHUB_TRACKER -> new TrackerGitHubGrabber(configuration);
            case JSON -> new JsonGrabber(configuration);
        };
        final AbstractDestination destination = switch(configuration.getDestination())
        {
            case ARIA2C -> new Aria2Destination(configuration);
            case DEPLOY_HISTORY -> new DeployHistoryDestination(configuration);
            case JSON -> new JsonDestination(configuration);
        };

        grabber.setup();
        (grabber.isParallelSupported() ? configuration.getChannels().parallelStream() : configuration.getChannels().stream())
                .forEach(channel -> destination.sendVersions(grabber.getVersions(channel), channel));
    }
}