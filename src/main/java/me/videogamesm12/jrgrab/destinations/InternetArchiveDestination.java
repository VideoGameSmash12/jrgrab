package me.videogamesm12.jrgrab.destinations;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;

public class InternetArchiveDestination extends AbstractDestination
{
	public InternetArchiveDestination(JRGConfiguration config)
	{
		super(config);

		// Parallel file downloading will cause problems so we don't support it currently
		if (config.getChannels().size() > 1)
		{
			throw new IllegalArgumentException("This destination currently only supports 1 channel per process.");
		}
		else if (config.getIaToken() == null)
		{
			throw new IllegalArgumentException("You need to set an account token to use for uploading files. For more"
					+ " information, see this page on the Internet Archive wiki -"
					+ " https://archive.org/developers/tutorial-get-ia-credentials.html");
		}
		// validate account login
	}

	@Override
	public void sendVersions(List<RBXVersion> versions, String channel)
	{
		// Fetch files
		versions.parallelStream().forEach(version -> version.fetchFiles(getConfig()));

		Main.getLogger().info("Downloading files for channel {}", channel);

		// For each version (in a non-parallel stream), get the files by either downloading them
		// 	or piping it into an inputstream for upload, not sure how this works yet
		for (RBXVersion version : versions.stream().filter(RBXVersion::getAvailable).toList())
		{
			Main.getLogger().info("Downloading files for version {}", version.getVersionHash());

			final String bucket = ("roblox_" + version.getType().getFriendlyName() + "_" + version.getVersionHash() + "_"
					+ (channel.equalsIgnoreCase("live") ? "" : channel + "_") + "jrgrab").toLowerCase();

			// Verify that an entry doesn't already exist (to avoid fucking up existing entries). If it does exist,
			// 	continue onto the next client with a message saying it was already archived
			Main.getLogger().info("Verifying version {} is not on the Internet Archive already", version.getVersionHash());
			if (bucketExists(bucket))
			{
				Main.getLogger().info("Skipping version {} in channel {} as it already exists on the Internet Archive",
						version, channel);
				continue;
			}

			Main.getLogger().info("Creating temporary directory for version {}", version.getVersionHash());
			final File tempDir = new File(bucket);
			tempDir.mkdirs();

			// Create boilerplate headers to use
			Map<String, String> headers = new HashMap<>();
			headers.put("x-amz-auto-make-bucket", "1");
			headers.put("x-archive-meta01-collection", "open_source_software");
			headers.put("x-archive-meta-mediatype", "software");
			headers.put("x-archive-meta-title", "Roblox - " + version.getVersionHash()
					+ " (" + version.getType().getFriendlyName() + (channel.equalsIgnoreCase("live") ? "" : ", " + channel) + ") [jrgrab]");
			headers.put("x-archive-meta-creator", "Roblox Corporation");
			headers.put("x-archive-meta-description", "Downloaded and uploaded using jrgrab.");
			headers.put("x-archive-meta01-subject", "Roblox");
			headers.put("x-archive-meta02-subject", version.getType().getFriendlyName());
			headers.put("x-archive-meta03-subject", version.getVersionHash());
			headers.put("x-archive-meta-scanner", "jrgrab");
			headers.put("authorization", "LOW " + getConfig().getIaToken());

			Main.getLogger().info("DEBUG! HEADERS");
			Main.getLogger().info(headers.toString());

			// Download all files ourselves
			version.getFiles().keySet().parallelStream().forEach(fileName ->
			{
				try
				{
					Main.getLogger().info("Downloading file {} from version {}", fileName, version.getVersionHash());

					if (fileName.equalsIgnoreCase("rbxPkgManifest.txt"))
					{
						// We will download the file ourselves
						final String url = "https://" + getConfig().getDomain() + "/"
								+ (version.getChannel().equalsIgnoreCase("live") ? "" : "channel/"
								+ (getConfig().getCommonChannels().contains(channel) ? "common" : channel) + "/")
								+ (version.getType().isMac() ? "mac/" + (getConfig().isArm64() ? "arm64/" : "") : "")
								+ (version.isCjv() ? "cjv/" : "") + version.getVersionHash() + "-" + fileName;
						final File destination = new File(tempDir, version.getVersionHash() + "-" + fileName);

						long bytes = HttpUtil.downloadFile(url, destination.toPath());

						if (bytes == 0)
						{
							Main.getLogger().warn("Uh oh, client file not found or some other tomfuckery happened! Video, please fix this!");
						}
						else
						{
							Main.getLogger().info("Downloading file {} from version {} completed with {} bytes, now uploading", fileName, version.getVersionHash(), bytes);
							HttpUtil.uploadFile("http://s3.us.archive.org/" + bucket + "/" + version.getVersionHash() + "-" + fileName, destination.toPath(), headers);
						}
					}
				}
				catch (Throwable ex)
				{
					Main.getLogger().warn("Wtf?", ex);
				}
			});

			Arrays.stream(Objects.requireNonNull(tempDir.listFiles())).parallel().forEach(File::delete);
			tempDir.delete();

			// Create a temporary folder containing the client files. We will delete this after

			// Create a bucket for the relevant entry (format should be roblox_<type>_<versionhash>_jrgrab for live,
			// 	roblox_<type>_<versionhash>_<channel>_jrgrab for non-live channels)

			// Each client file should be downloaded and then uploaded using the bucket
		}
	}

	private boolean bucketExists(String bucket)
	{
		try
		{
			// Check the bucket with a simple command
			final HttpResponse<String> response = HttpUtil.getFull("http://s3.us.archive.org/" + bucket + "/");

			// Bucket should return 200 or 307 if bucket exists
			return response.statusCode() == 200 || response.statusCode() == 307;
		}
		catch (IOException | InterruptedException failure)
		{
			return false;
		}
	}


	// To upload files, follow this general concept but in Java. It uses the PUT HTTP function
	// curl --location \
	//     --header "authorization: LOW $accesskey:$secret" \
	//     --upload-file /home/samuel/public_html/intro-to-k.pdf \
	//     https://s3.us.archive.org/sam-s3-test-08/demo-intro-to-k.pdf
}
