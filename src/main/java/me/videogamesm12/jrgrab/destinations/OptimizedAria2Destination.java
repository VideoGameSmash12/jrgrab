package me.videogamesm12.jrgrab.destinations;

import com.google.gson.*;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>OptimizedAria2Destination</h1>
 * <p>A modified variant of {@link Aria2Destination} which avoids re-downloading the same files again and again, but at
 * the cost of being significantly more stringent with its requirements.</p>
 * <p>{@link Aria2Destination} works by sending all files we know of to the Aria2 daemon without regard for if the files
 * are different or not. This has the benefit of being a lot looser on what can and can't be done with it, but on the
 * other hand is extremely inefficient when you need to download lots of versions in bulk.</p>
 * <p>OptimizedAria2Destination takes a significantly different approach. It maintains a sort of "database" where it
 * keeps track of each version's files by using their MD5 hash as reference for if a file has changed between the two
 * versions, then only sends the "new" files to the database.</p>
 * <p>This is apparently how Roblox handles version updates as well, so it works out!</p>
 */
public class OptimizedAria2Destination extends Aria2Destination
{
    private final Map<String, JsonObject> fileMap = new HashMap<>();
    private final List<String> knownFileHashes = new ArrayList<>();

    public OptimizedAria2Destination(JRGConfiguration config)
    {
        super(config);

        if (config.isIncludingUnavailable())
        {
            throw new IllegalArgumentException("This destination type requires an exact knowledge of which clients are "
                    + "available. Unavailable clients cannot be included.");
        }
        else if (!config.isFetchingManifestForFiles())
        {
            throw new IllegalArgumentException("This destination type requires us to know the hashes of all files in a " +
                    "client, which requires us to fetch the manifest.");
        }
        else if (config.isMac())
        {
            throw new IllegalArgumentException("This destination type cannot be used for Mac clients due to differences"
                    + " in how those are packaged.");
        }

        if (config.isIncremental())
        {
            final File folder = new File(System.getProperty("user.dir"));
            if (!folder.isDirectory())
            {
                throw new IllegalStateException("Something isn't right, current directory isn't a directory??");
            }

            final Pattern filePattern = Pattern.compile("files\\.([A-z0-9_-]+)\\.json");

            Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file -> file.getName().startsWith("files.")
                    && file.getName().endsWith(".json")).forEach(file ->
            {
                try
                {
                    final JsonObject map = getGson().fromJson(new FileReader(file), JsonObject.class);
                    Main.getLogger().info(file.getName());
                    final Matcher channelMatcher = filePattern.matcher(file.getName());
                    if (channelMatcher.find())
                    {
                        final String channelName = channelMatcher.group(1);
                        map.entrySet().forEach(entry -> entry.getValue().getAsJsonObject().entrySet().stream()
                                .filter(fileEntry -> !knownFileHashes.contains(fileEntry.getValue().getAsString()))
                                .forEach(fileEntry -> knownFileHashes.add(fileEntry.getValue().getAsString())));
                        fileMap.put(channelName, map);
                    }
                }
                catch (IOException ex)
                {
                    Main.getLogger().error("Failed to read channel file list", ex);
                }
                catch (JsonParseException ex)
                {
                    Main.getLogger().error("Failed to parse channel file list", ex);
                }
            });
        }
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

        final JsonObject map;

        if (!fileMap.containsKey(channel))
        {
            map = new JsonObject();
            fileMap.put(channel, map);
        }
        else
        {
            map = fileMap.get(channel);
        }

        // Since we have a lot more free resources when we're grabbing just one channel's worth of clients, we can use
        //  parallel streams to get more done within a timeframe.
        (getConfig().getChannels().size() == 1 ? versions.parallelStream() : versions.stream())
                .filter(version -> getConfig().isIncludingUnavailable() || version.getAvailable()).toList().forEach(client ->
        {
            final JsonObject versionElement = new JsonObject();

            try
            {
                client.getFiles().forEach((file, hash) ->
                {
                    // If the file hash is blank or empty, ignore it
                    if (hash.isEmpty() || hash.isBlank())
                    {
                        return;
                    }

                    versionElement.addProperty(file, hash);

                    // Ignore file if it's already known, is blank, or is empty.
                    if (knownFileHashes.contains(hash))
                    {
                        return;
                    }

                    Main.getLogger().info("Queuing unique file {} with hash {} from version {}", file, hash, client.getVersionHash());

                    try
                    {
                        HttpUtil.post("http://" + getConfig().getAria2().getIpAddress() + ":" + getConfig().getAria2().getPort() + "/jsonrpc",
                                getGson().toJson(createAria2Request("https://" + getConfig().getDomain() + "/"
                                        + (client.getChannel().equalsIgnoreCase("live") ? "" : "channel/"
                                        + (getConfig().getCommonChannels().contains(channel) ? "common" : channel) + "/")
                                        + (client.getType().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
                                        + (client.isCjv() ? "cjv/" : "") + client.getVersionHash() + "-" + file, hash)));
                        knownFileHashes.add(hash);
                    }
                    catch (IOException | InterruptedException ex)
                    {
                        Main.getLogger().error("Failed to queue file in Aria2 instance", ex);
                    }
                });

                map.add(client.getVersionHash(), versionElement);
            }
            catch (Exception ex)
            {
                Main.getLogger().error("WTF", ex);
            }
        });

        try
        {
            Main.getLogger().info("Writing version file map to disk");
            FileWriter writer = new FileWriter("files." + channel + (getConfig().isCjv() ? ".cjv" : "") + ".json");
            getGson().toJson(map, writer);
            writer.close();
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to write version file map to disk", ex );
        }
    }
}
