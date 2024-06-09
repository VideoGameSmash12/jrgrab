package me.videogamesm12.jrgrab.grabbers;

import joptsimple.internal.Strings;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.*;

public class ManualGrabber extends AbstractGrabber
{
    private final Map<String, List<String>> clients = new HashMap<>();

    public ManualGrabber(JRGConfiguration config)
    {
        super(config, true);
    }

    @Override
    public void setup()
    {
        // Parse what we have
        getConfig().getManuallySpecifiedClients().stream().map(client -> client.split("@")).forEach(set ->
        {
            final String channel = set.length > 1 ? set[0].trim() : "live";
            final String hash = (set.length > 1 ? set[1] : set[0]).trim();

            // Reject blank/empty channels or hashes
            if (channel.isBlank() || channel.isEmpty() || hash.isBlank() || hash.isEmpty())
            {
                return;
            }

            if (!clients.containsKey(channel))
            {
                clients.put(channel, new ArrayList<>());
            }

            if (clients.get(channel).stream().noneMatch(client -> client.equalsIgnoreCase(hash)))
            {
                clients.get(channel).add(hash);
            }
        });
    }

    @Override
    public List<RBXVersion> getVersions(String channel)
    {
        return clients.getOrDefault(channel, new ArrayList<>()).stream().map(hash ->
        {
            // We have no idea what version type we are, so we need to make some calls to figure it out.
            // We don't use DeployHistory since Roblox tends to hide non-live clients from those, making them unreliable
            //  as a source of truth. They are also comparatively inefficient
            final RBXVersion.VersionType type;
            final long deployDate;
            try
            {
                final String prefix = "https://" + getConfig().getDomain() + "/" +
                        (channel.equalsIgnoreCase("live") ? "" : "channel/" + channel + "/");

                // This is the one thing we do know - whether our client is a Mac client.
                if (getConfig().isMac())
                {
                    // If RobloxVersion.txt doesn't return a 403, then it's a Client. If RobloxStudioVersion.txt doesn't
                    //  return a 403, then it's a Studio. If both of them return a 403, then it's not a valid client
                    final HttpResponse<String> checkUno = HttpUtil.getFull(prefix + "mac/" + hash + "-RobloxVersion.txt");
                    final HttpResponse<String> checkDos = HttpUtil.getFull(prefix + "mac/" + hash + "-RobloxStudioVersion.txt");

                    if (checkUno.statusCode() != 403)
                    {
                        type = RBXVersion.VersionType.MAC_PLAYER;
                        deployDate = HttpUtil.getDateFormat().parse(checkUno.headers().map().get("Last-Modified").toString()).getTime();
                    }
                    else if (checkDos.statusCode() != 403)
                    {
                        type = RBXVersion.VersionType.MAC_STUDIO;
                        deployDate = HttpUtil.getDateFormat().parse(checkDos.headers().map().get("Last-Modified").toString()).getTime();
                    }
                    else
                    {
                        // Not a valid client, returning null
                        return null;
                    }
                }
                // Just get the type from the rbxPkgManifest if that returns any useful information
                else
                {
                    final HttpResponse<String> manifest = HttpUtil.getFull(prefix + hash + "-rbxPkgManifest.txt");

                    if (manifest.statusCode() == 403)
                    {
                        // Not a valid client, no point in trying to get it
                        return null;
                    }
                    else if (manifest.body().contains("RobloxStudio"))
                    {
                        // Here's where things get tricky. There isn't a way to reliably distinguish between Studio64
                        //  clients and Studio clients through the rbxPkgManifest, but there is an endpoint we can hit
                        //  where we can more reliably distinguish the two.
                        //
                        // Both Studio and Studio64 clients have a file called WebView2Loader.dll. No idea what it does,
                        //  but the hash of this file has always been 577f05cd683ed0577f6c970ea57129e0 in Studio64. This
                        //  is not the case for regular Studio. This is how we can tell the two apart.
                        //
                        // Although Studio64's rbxPkgManifest has had the same hash for redist.zip since regular Studio
                        //  was discontinued, we can't use this information since the hashes are different in older,
                        //  still downloadable Studio64 versions like version-da6d7bb2a82d4aae (from December 2019).
                        type = HttpUtil.get(prefix + hash + "-rbxManifest.txt").contains("577f05cd683ed0577f6c970ea57129e0") ? RBXVersion.VersionType.WINDOWS_STUDIO_X64 : RBXVersion.VersionType.WINDOWS_STUDIO_X86;
                    }
                    else
                    {
                        type = RBXVersion.VersionType.WINDOWS_PLAYER;
                    }

                    deployDate = HttpUtil.getDateFormat().parse(manifest.headers().map().get("Last-Modified").toString()).getTime();
                }
            }
            catch (ParseException ex)
            {
                Main.getLogger().error("An error occurred while attempting to parse the deployment date for version {} in channel {}", hash, channel, ex);
                return null;
            }
            catch (IOException | InterruptedException ex)
            {
                Main.getLogger().error("An error occurred while attempting to figure out version {} in channel {}", hash, channel, ex);
                return null;
            }

            return RBXVersion.fromClientSettings(type, hash, deployDate, "0, 0, 0, 0", channel, new ArrayList<>(), false);
        }).filter(Objects::nonNull).toList();
    }
}
