package me.videogamesm12.jrgrab;

import lombok.Getter;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.destinations.AbstractDestination;
import me.videogamesm12.jrgrab.destinations.Aria2Destination;
import me.videogamesm12.jrgrab.destinations.DeployHistoryDestination;
import me.videogamesm12.jrgrab.destinations.JsonDestination;
import me.videogamesm12.jrgrab.grabbers.AbstractGrabber;
import me.videogamesm12.jrgrab.grabbers.ClientSettingsGrabber;
import me.videogamesm12.jrgrab.grabbers.DeployGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
    @Getter
    private static final Logger logger = LoggerFactory.getLogger("JRGrab");

    public static void main(String[] args)
    {
        JRGConfiguration configuration = JRGConfiguration.fromArguments(args);
        if (configuration == null)
        {
            return;
        }

        final AbstractGrabber grabber = switch(configuration.getSource())
        {
            case DEPLOY_HISTORY -> new DeployGrabber(configuration);
            case CLIENT_SETTINGS -> new ClientSettingsGrabber(configuration);
        };
        final AbstractDestination destination = switch(configuration.getDestination())
        {
            case ARIA2C -> new Aria2Destination(configuration);
            case DEPLOY_HISTORY -> new DeployHistoryDestination(configuration);
            case JSON -> new JsonDestination(configuration);
        };

        grabber.setup();
        destination.sendVersions(grabber.getVersions());
    }
}