package me.videogamesm12.jrgrab.grabbers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * <h1>JsonGrabber</h1>
 * <p>Grabber that imports previously generated versions.json files from previous runs of the application.</p>
 */
public class JsonGrabber extends AbstractGrabber
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonGrabber(JRGConfiguration config)
    {
        super(config, true);
    }

	@Override
    public List<RBXVersion> getVersions(String channel, List<String> known)
    {
        final File file = new File("versions." + channel + (getConfig().isMac() ? ".mac" + (getConfig().isArm64() ? ".arm64" : "") : "") + ".json");
        if (file.isFile())
        {
            try
            {
                return gson.fromJson(new FileReader(file), new TypeToken<List<RBXVersion>>(){}.getType());
            }
            catch (IOException ex)
            {
                Main.getLogger().error("An I/O error occurred while attempting to read the versions.json for channel {}", channel, ex);
            }
            catch (JsonParseException ex)
            {
                Main.getLogger().error("The versions.json for channel {} is corrupted. Check the error below.", channel, ex);
            }
        }
        else
        {
            Main.getLogger().warn("Not getting clients for channel {} as the versions.json doesn't exist", channel);
        }

        return List.of();
    }
}
