package me.videogamesm12.jrgrab.grabbers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientSettingsGrabber extends AbstractGrabber
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ClientSettingsGrabber(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void setup()
    {
    }

    @Override
    public List<RBXVersion> getVersions(String channel)
    {
        return Arrays.stream(RBXVersion.VersionType.values()).filter(type -> getConfig().isMac() == type.isMac()).map(type ->
        {
            try
            {
                final JsonObject object = gson.fromJson(HttpUtil.get("https://clientsettings.roblox.com/v2/client-version/" + type.getClientSettingsName()
                        + (channel.equalsIgnoreCase("live") ? "" : "channel/" + channel)), JsonObject.class);

                if (object.has("errors") || !object.has("version"))
                {
                    Main.getLogger().error("Failed to get client of type {} due to an error on Roblox's behalf", type.name());
                    return null;
                }

                final String hash = object.getAsJsonPrimitive("clientVersionUpload").getAsString();
                final String version = object.getAsJsonPrimitive("version").getAsString();

                // ClientSettings isn't enough - We need to get the deployment date, so we need to just get the smallest
                //  file we can find and extract the last modified header from there to approximate it. If it fails, we
                //  will just have to wing it since we know the client does exist
                final HttpResponse<String> meta = HttpUtil.getFull("https://" + getConfig().getDomain() + "/" +
                        (channel.equalsIgnoreCase("live") ? "channel/" + channel : "")
                        + (getConfig().isMac() ? "mac/" : "") + hash + "-" + (getConfig().isMac() ?
                        (type == RBXVersion.VersionType.MAC_STUDIO ? "RobloxStudio.zip" : "Roblox.dmg") : "rbxPkgManifest.txt"));

                final long deployDate = meta.statusCode() != 403 ? HttpUtil.getDateFormat().parse(meta.headers().map().get("Last-Modified").toString()).getTime() : Instant.now().toEpochMilli();

                return RBXVersion.fromClientSettings(type, hash, deployDate, version, channel, new ArrayList<>(), false);
            }
            catch (IOException | InterruptedException | ParseException ex)
            {
                Main.getLogger().error("Failed to get client of type {}", type.name(), ex);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }
}
