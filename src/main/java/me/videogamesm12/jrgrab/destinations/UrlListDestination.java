package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UrlListDestination extends AbstractDestination
{
    public UrlListDestination(JRGConfiguration config)
    {
        super(config);

        if (!config.isFetchingManifestForFiles())
        {
            Main.getLogger().warn("Fetching manifests for files is disabled. This may reduce the accuracy as for what files will be listed.");
        }
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

        Main.getLogger().info("Writing URL list to disk");
        try (FileWriter writer = new FileWriter("urls." + channel + (getConfig().isMac() ? ".mac"
                + (getConfig().isArm64() ? ".arm64" : "") : "") + (getConfig().isCjv() ? ".cjv" : "") + ".txt"))
        {
            List<String> files = new ArrayList<>();
            versions.forEach(version -> version.getFiles().keySet().forEach(file -> files.add("https://" + getConfig().getDomain() + "/"
                    + (version.getChannel().equalsIgnoreCase("live") ? "" : "channel/"
                    + (getConfig().getCommonChannels().contains(channel) ? "common" : channel) + "/")
                    + (version.getType().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
                    + (version.isCjv() ? "cjv/" : "") + version.getVersionHash() + "-" + file)));
            writer.write(String.join("\r\n", files));
        }
        catch (IOException e)
        {
            Main.getLogger().error("Failed to write file list to disk");
        }
    }
}
