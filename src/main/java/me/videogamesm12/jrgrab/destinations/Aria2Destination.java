package me.videogamesm12.jrgrab.destinations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class Aria2Destination extends AbstractDestination
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Aria2Destination(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void sendVersions(List<RBXVersion> versions, String channel)
    {
        versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

        // Since we have a lot more free resources when we're grabbing just one channel's worth of clients, we can use
        //  parallel streams to get more done within a timeframe.
        (getConfig().getChannels().size() == 1 ? versions.parallelStream() : versions.stream())
                .filter(version -> getConfig().isIncludingUnavailable() || version.getAvailable()).toList().forEach(client ->
        {
            Main.getLogger().info("Queuing files for download for version {}", client.getVersionHash());

            try
            {
                client.getFiles().keySet().forEach(file ->
                {
                    // Create an aria2c request
                    final JsonObject object = new JsonObject();
                    object.addProperty("jsonrpc", "2.0");
                    object.addProperty("method", "aria2.addUri");

                    final JsonArray params = new JsonArray();
                    params.add("token:" + getConfig().getAria2().getToken());
                    final JsonArray links = new JsonArray();
                    links.add("https://" + getConfig().getDomain() + "/"
                            + (client.getChannel().equalsIgnoreCase("live") ? "" : "channel/" + client.getChannel() + "/")
                            + (client.getType().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
                            + (client.isCjv() ? "cjv/" : "") + client.getVersionHash() + "-" + file);
                    params.add(links);
                    final JsonObject output = new JsonObject();
                    output.addProperty("out", client.getChannel() + "/"
                            + (client.getType().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
                            + (client.isCjv() ? "cjv/" : "")
                            + client.getVersionHash() + "/"
                            + client.getVersionHash() + "-" + file);
                    params.add(output);
                    object.add("params", params);
                    object.addProperty("id", Instant.now().toEpochMilli());

                    try
                    {
                        HttpUtil.post("http://" + getConfig().getAria2().getIpAddress() + ":" + getConfig().getAria2().getPort() + "/jsonrpc", gson.toJson(object));
                    }
                    catch (IOException | InterruptedException ex)
                    {
                        Main.getLogger().error("Failed to queue file in Aria2 instance", ex);
                    }
                });
            }
            catch (Exception ex)
            {
                Main.getLogger().error("WTF", ex);
            }
        });
    }
}
