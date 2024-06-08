package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DeployGrabber extends AbstractGrabber
{
    public DeployGrabber(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void setup()
    {
    }

    @Override
    public List<RBXVersion> getVersions()
    {
        try
        {
            String[] lines = HttpUtil.get("https://" + getConfig().getDomain() + "/"
                    + (getConfig().getChannel().equalsIgnoreCase("live") ? "" : "channel/" + getConfig().getChannel().toLowerCase() + "/")
                    + (getConfig().isMac() ? "mac/" : "")
                    + "DeployHistory.txt").split("\r\n");

            return Arrays.stream(lines).map(line ->
            {
                try
                {
                    return RBXVersion.fromString(line, getConfig().getChannel(), new ArrayList<>(), getConfig().isMac(), false);
                }
                catch (ParseException e)
                {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
        catch (IOException | InterruptedException ex)
        {
            Main.getLogger().error("Failed to get version list from DeployHistory", ex);
            return List.of();
        }
    }
}
