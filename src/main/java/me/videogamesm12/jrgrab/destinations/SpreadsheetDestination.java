package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.BufferedWriter;
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
        Main.getLogger().info("Writing spreadsheet");

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter("version_spreadsheet." + channel +
                (getConfig().isMac() ? ".mac" + (getConfig().isArm64() ? ".arm" : "") : "") + ".csv")))
        {
            writer.write(String.join(System.lineSeparator(), versions.stream().map(version ->
                    version.getVersionHash() + "," + version.getFileVersion().replaceAll(",( )?", ".") + ","
                            + version.getType().getFriendlyName() + "," + "=EPOCHTODATE("
                            + (version.getDeployDate() / 1000) + ")").toList()));
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to write spreadsheet", ex);
        }
    }
}
