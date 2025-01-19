package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SpreadsheetDestination extends AbstractDestination
{
    public SpreadsheetDestination(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        try
        {
            Main.getLogger().info("Writing DeployHistory-formatted file");
            final FileWriter writer = new FileWriter(new File("version_spreadsheet." + channel +
                    (getConfig().isMac() ? ".mac" + (getConfig().isArm64() ? ".arm" : "") : "") + ".txt"));
            writer.write(String.join("\r\n", versions.stream().map(version -> version.getVersionHash() + "," + version.getFileVersion().replaceAll(",( )?", ".") + "," + version.getType().getFriendlyName() + "," + "=EPOCHTODATE(" + (version.getDeployDate() / 1000) + ")").toList()));
            writer.close();
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to write DeployHistory-formatted file", ex);
        }
    }
}
