package me.videogamesm12.jrgrab.destinations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptimizedDownloadDestination extends DownloadDestination
{
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Map<String, JsonObject> fileMap = new HashMap<>();
	private final List<String> knownFileHashes = new ArrayList<>();

	public OptimizedDownloadDestination(JRGConfiguration config)
	{
		super(config);

		if (config.isIncludingUnavailable())
		{
			throw new IllegalArgumentException("This destination type requires an exact knowledge of which clients are "
					+ "available. Unavailable clients cannot be included.");
		}
		else if (!config.isFetchingManifestForFiles())
		{
			throw new IllegalArgumentException("This destination type requires us to know the hashes of all files in a " +
					"client, which requires us to fetch the manifest.");
		}
		else if (config.isMac())
		{
			throw new IllegalArgumentException("This destination type cannot be used for Mac clients due to differences"
					+ " in how those are packaged.");
		}

		if (config.isIncremental())
		{
			final File folder = new File(System.getProperty("user.dir"));
			if (!folder.isDirectory())
			{
				throw new IllegalStateException("Something isn't right, current directory isn't a directory??");
			}

			final Pattern filePattern = Pattern.compile("files\\.([A-z0-9_-]+)\\.json");

			Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file -> file.getName().startsWith("files.")
					&& file.getName().endsWith(".json")).forEach(file ->
			{
				try
				{
					final JsonObject map = gson.fromJson(new FileReader(file), JsonObject.class);
					Main.getLogger().info(file.getName());
					final Matcher channelMatcher = filePattern.matcher(file.getName());
					if (channelMatcher.find())
					{
						final String channelName = channelMatcher.group(1);
						map.entrySet().forEach(entry -> entry.getValue().getAsJsonObject().entrySet().stream()
								.filter(fileEntry -> !knownFileHashes.contains(fileEntry.getValue().getAsString()))
								.forEach(fileEntry -> knownFileHashes.add(fileEntry.getValue().getAsString())));
						fileMap.put(channelName, map);
					}
				}
				catch (IOException ex)
				{
					Main.getLogger().error("Failed to read channel file list", ex);
				}
				catch (JsonParseException ex)
				{
					Main.getLogger().error("Failed to parse channel file list", ex);
				}
			});
		}
	}

	@Override
	public void sendVersions(List<RBXVersion> versions, String channel)
	{
		final JsonObject map;

		if (!fileMap.containsKey(channel))
		{
			map = new JsonObject();
			fileMap.put(channel, map);
		}
		else
		{
			map = fileMap.get(channel);
		}

		versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

		// Since we have a lot more free resources when we're grabbing just one channel's worth of clients, we can use
		//  parallel streams to get more done within a timeframe.
		versions.parallelStream().filter(RBXVersion::getAvailable).forEach(client ->
		{
			final JsonObject versionElement = new JsonObject();

			try
			{
				client.getFiles().forEach((file, hash) ->
				{
					// If the file hash is blank or empty, ignore it
					if (hash.isEmpty() || hash.isBlank())
					{
						return;
					}

					versionElement.addProperty(file, hash);

					// Ignore file if it's already known, is blank, or is empty.
					if (knownFileHashes.contains(hash))
					{
						return;
					}

					knownFileHashes.add(hash);

					Main.getLogger().info("Downloading file {} from version {}", file, client.getVersionHash());

					// We will download the file ourselves
					final String url = client.getBaseUrl(getConfig()) + "-" + file;
					final File destination = new File(hash);

					long bytes = HttpUtil.downloadFile(url, destination.toPath());

					if (bytes == 0)
					{
						Main.getLogger().warn("Uh oh, client file not found or some other tomfuckery happened! Video, please fix this!");
					}
					else
					{
						Main.getLogger().info("Download for file {} from version {} completed with {} bytes", file, hash, bytes);
					}
				});

				map.add(client.getVersionHash(), versionElement);
			}
			catch (Exception ex)
			{
				Main.getLogger().error("WTF", ex);
			}
		});

		try
		{
			Main.getLogger().info("Writing version file map to disk");
			FileWriter writer = new FileWriter("files." + channel + (getConfig().isCjv() ? ".cjv" : "") + ".json");
			gson.toJson(map, writer);
			writer.close();
		}
		catch (IOException ex)
		{
			Main.getLogger().error("Failed to write version file map to disk", ex );
		}
	}
}
