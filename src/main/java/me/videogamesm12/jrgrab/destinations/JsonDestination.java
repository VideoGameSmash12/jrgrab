package me.videogamesm12.jrgrab.destinations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class JsonDestination extends AbstractDestination
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonDestination(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        versions.parallelStream().forEach(version ->
        {
            Main.getLogger().info("Getting files for version {}", version.getVersionHash());
            version.verifyAvailability(getConfig());
            version.fetchFiles(getConfig());
        });

        try
        {
            FileWriter writer = new FileWriter("versions." + channel + (getConfig().isMac() ? ".mac" : "") + ".json");
            gson.toJson(versions.stream().filter(version -> getConfig().isIncludingUnavailable() || version.getAvailable()).toList(), writer);
            writer.close();
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to dump versions to JSON", ex);
        }
    }
}
