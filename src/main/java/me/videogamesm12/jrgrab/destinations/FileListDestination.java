package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileListDestination extends AbstractDestination
{
    public FileListDestination(JRGConfiguration config)
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

        Main.getLogger().info("Writing file list to disk");
        try (FileWriter writer = new FileWriter("files." + channel + (getConfig().isMac() ? ".mac"
                + (getConfig().isArm64() ? ".arm64" : "") : "") + (getConfig().isCjv() ? ".cjv" : "") + ".txt"))
        {
            writer.write(String.join("\r\n", versions.stream().map(version -> String.join("\r\n",
                    version.getFiles().keySet())).toList()));
        }
        catch (IOException e)
        {
            Main.getLogger().error("Failed to write file list to disk");
        }
    }
}
