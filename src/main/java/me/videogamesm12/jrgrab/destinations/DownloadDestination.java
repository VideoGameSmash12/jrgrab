package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadDestination extends AbstractDestination
{
	public DownloadDestination(JRGConfiguration config)
	{
		super(config);

		// Parallel file downloading will cause problems so we don't support it currently
		if (config.getChannels().size() > 1)
		{
			throw new IllegalArgumentException("This destination currently only supports 1 channel per process.");
		}
	}

	@Override
	public void sendVersions(List<RBXVersion> versions, String channel)
	{
		Main.getLogger().info("Downloading files for channel {}", channel);

		final List<File> folders = new ArrayList<>();

		// Fetch files for each version
		versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

		for (RBXVersion version : versions.stream().filter(RBXVersion::getAvailable).toList())
		{
			Main.getLogger().info("Downloading files for version {}", version.getVersionHash());

			final File dir = new File(new File(System.getProperty("user.dir")),
					(channel.equalsIgnoreCase("live") ? "" : channel + "/")
					+ (getConfig().isMac() ? "mac/" : "")
					+ (version.isCjv() ? "cjv/" : "") + version.getVersionHash());
			dir.mkdirs();

			// Download all files ourselves
			version.getFiles().keySet().parallelStream().forEach(fileName ->
			{
				try
				{
					Main.getLogger().info("Downloading file {} from version {}", fileName, version.getVersionHash());

					// We will download the file ourselves
					final String url = version.getBaseUrl(getConfig()) + "-" + fileName;
					final File destination = new File(dir, version.getVersionHash() + "-" + fileName);

					long bytes = HttpUtil.downloadFile(url, destination.toPath());

					if (bytes == 0)
					{
						Main.getLogger().warn("Uh oh, client file not found or some other problem happened.");
					}
					else
					{
						Main.getLogger().info("Downloading file {} from version {} completed with {} bytes", fileName, version.getVersionHash(), bytes);
					}
				}
				catch (Throwable ex)
				{
					Main.getLogger().warn("Wtf?", ex);
				}
			});

			folders.add(dir);
		}

		postDownload(folders, channel);
	}

	public void postDownload(List<File> dirs, String channel)
	{
		// Do nothing, this is just a foundational thing to do.
	}
}
