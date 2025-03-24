package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>LegacyGrabber</h1>
 * <p>Grabber that finds client versions from a provided channel using the legacy "version", "versionStudio", and
 * "versionQTStudio" endpoints.</p>
 * @implNote <p>Not reliable for scraping newer channels or finding newer clients as Roblox stopped updating this
 *              endpoint in August 2023.</p>
 */
public class LegacyGrabber extends AbstractGrabber
{
    public LegacyGrabber(JRGConfiguration config)
    {
        super(config, true);

        Main.getLogger().warn("The legacy endpoint is deprecated and has not been updated since August 2023. Future "
                + "channels don't even have it, so keep that in mind.");
    }

    @Override
    public List<RBXVersion> getVersions(String channel, List<String> known)
    {
        final List<RBXVersion> versions = new ArrayList<>();

        // Information regarding the types of clients were located at different endpoints
        // TODO: Clean this up later
        try
        {
            final String prefix = "https://" + getConfig().getDomain() + "/" +
                    (channel.equalsIgnoreCase("live") ? "" : "channel/" + channel + "/");

            // Player
            final HttpResponse<String> player = HttpUtil.getFull(prefix + (getConfig().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "") + "version");

            // Did we score a hit?
            if (player.statusCode() != 403)
            {
                String hash = player.body();
                long deployDate;
                try
                {
                    deployDate = HttpUtil.getDateFormat().parse(player.headers().map().get("Last-Modified").toString()).getTime();
                }
                catch (ParseException ex)
                {
                    // Invalid time format?
                    deployDate = 0;
                }

                versions.add(RBXVersion.fromClientSettings(
                        getConfig().isMac() ? RBXVersion.VersionType.MAC_PLAYER : RBXVersion.VersionType.WINDOWS_PLAYER,
                        hash, deployDate, "0, 0, 0, 0", channel, known, false));
            }

            // Studio
            final HttpResponse<String> studio = HttpUtil.getFull(prefix + (getConfig().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") + "versionStudio" : "versionQTStudio"));

            if (studio.statusCode() != 403)
            {
                String hash = studio.body();
                long deployDate;
                try
                {
                    deployDate = HttpUtil.getDateFormat().parse(studio.headers().map().get("Last-Modified").toString()).getTime();
                }
                catch (ParseException ex)
                {
                    // Invalid time format?
                    deployDate = 0;
                }

                versions.add(RBXVersion.fromClientSettings(
                        getConfig().isMac() ? RBXVersion.VersionType.MAC_STUDIO : RBXVersion.VersionType.WINDOWS_STUDIO_X86,
                        hash, deployDate, "0, 0, 0, 0", channel, known, false));
            }

            // versionStudio on Windows was last updated in December 2012 and points to the last MFCStudio version
            // It is weirdly inconsistent but whatever
            if (!getConfig().isMac())
            {
                final HttpResponse<String> mfcStudio = HttpUtil.getFull(prefix + "versionStudio");

                if (mfcStudio.statusCode() != 403)
                {
                    String hash = mfcStudio.body();
                    long deployDate;
                    try
                    {
                        deployDate = HttpUtil.getDateFormat().parse(mfcStudio.headers().map().get("Last-Modified").toString()).getTime();
                    }
                    catch (ParseException ex)
                    {
                        // Invalid time format?
                        deployDate = 0;
                    }

                    versions.add(RBXVersion.fromClientSettings(RBXVersion.VersionType.WINDOWS_MFC_STUDIO, hash, deployDate,
                            "0, 0, 0, 0", channel, known, false));
                }
            }
        }
        catch (IOException | InterruptedException ex)
        {
            Main.getLogger().error("An error occurred while attempting to grab versions from the legacy endpoints for channel {}", channel, ex);
        }

        return versions;
    }
}
