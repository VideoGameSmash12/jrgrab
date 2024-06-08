package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DeployHistoryDestination extends AbstractDestination
{
    public DeployHistoryDestination(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        versions.parallelStream().forEach(version ->
        {
            Main.getLogger().info("Verifying availability for version {}", version.getVersionHash());
            version.verifyAvailability(getConfig());
        });

        File folder = new File(channel + (getConfig().isMac() ? "/mac/" : ""));
        folder.mkdirs();

        try
        {
            final FileWriter writer = new FileWriter(new File(folder, "DeployHistory.txt"));
            writer.write(String.join("\r\n", versions.stream().filter(version -> getConfig().isIncludingUnavailable() || version.getAvailable()).map(RBXVersion::toString).toList()));
            writer.close();
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to write DeployHistory-formatted file", ex);
        }
    }
}
