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

        final StringBuilder fileList = new StringBuilder();
        versions.forEach(version -> version.getFiles().keySet().forEach(file ->
        {
            fileList.append(file);
            fileList.append("\n");
        }));


        try (FileWriter writer = new FileWriter("files." + channel + (getConfig().isMac() ? ".mac"
                + (getConfig().isArm64() ? ".arm64" : "") : "") + (getConfig().isCjv() ? ".cjv" : "") + ".txt"))
        {
            writer.write(fileList.toString());
        }
        catch (IOException e)
        {
        }
    }
}
