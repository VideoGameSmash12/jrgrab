package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * <h1>DeployGrabber</h1>
 * <p>Grabber that scrapes the DeployHistory.txt file present in most channels to find clients.</p>
 * @implNote <p>Not always reliable for scraping non-live channels as Roblox started redacting the version hashes of
 *              clients logged in the DeployHistory starting in 2022/2023.</p>
 */
public class DeployGrabber extends AbstractGrabber
{
    public DeployGrabber(JRGConfiguration config)
    {
        super(config, true);
    }

	@Override
    public List<RBXVersion> getVersions(String channel, List<String> known)
    {
        try
        {
            String[] lines;

            if (getConfig().getFile() != null)
            {
                try (BufferedReader reader = new BufferedReader(new FileReader(getConfig().getFile())))
                {
                    lines = reader.lines().toArray(i -> new String[0]);
                }
            }
            else
            {
                lines = HttpUtil.get("https://" + getConfig().getDomain() + "/"
                        + (channel.equalsIgnoreCase("live") ? "" : "channel/" + channel.toLowerCase() + "/")
                        + (getConfig().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
                        + "DeployHistory.txt").split("\r\n");
            }

            return Arrays.stream(lines).map(line ->
            {
                try
                {
                    return RBXVersion.fromString(line, channel, known, getConfig().isMac(), getConfig().isCjv());
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
