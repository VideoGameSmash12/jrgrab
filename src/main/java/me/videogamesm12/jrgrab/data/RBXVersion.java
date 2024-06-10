package me.videogamesm12.jrgrab.data;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.util.HttpUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Getter
public class RBXVersion
{
    private static final Pattern versionPattern = Pattern.compile("^New (WindowsPlayer|Studio|Studio64|Client) (version-[A-Fa-f0-9]{16}) at ([0-9]{1,2}/[0-9]{1,2}/[0-9]{4} [0-9]{1,2}:[0-9]{2}:[0-9]{2} (AM|PM))(, file vers?ion: ([0-9]+, ?[0-9]+, ?[0-9]+, ?[0-9]+))?(, git hash: ([A-z0-9]+ ))?...(Done!)?");
    private static final DateFormat dateFormat = new SimpleDateFormat("M'/'d'/'yyyy h':'mm':'ss a");
    //--
    private transient final String fullVersionString;
    private final VersionType type;
    private final String versionHash;
    private final long deployDate;
    private final String fileVersion;
    private final String channel;
    private final boolean cjv;
    @Builder.Default
    private Map<String, String> files = new HashMap<>();
    @Builder.Default
    private Boolean available = null;

    public void verifyAvailability(JRGConfiguration configuration)
    {
        // We already know it's available, no need to check again
        if (available != null && available)
        {
            return;
        }

        try
        {
            Main.getLogger().info("Verifying availability for version {}", getVersionHash());
            available = HttpUtil.getFull("https://" + configuration.getDomain() + "/"
                    + (!channel.equalsIgnoreCase("live") ? "channel/" + channel + "/" : "")
                    + (type.isMac() ? "mac/" : "") + (isCjv() ? "cjv/" : "")
                    + getVersionHash() + "-" + (type.isMac() ?
                    (type == RBXVersion.VersionType.MAC_STUDIO ? "RobloxStudioVersion.txt" : "RobloxVersion.txt") : "rbxPkgManifest.txt")).statusCode() != 403;
        }
        catch (IOException | InterruptedException ex)
        {
            Main.getLogger().warn("Failed to verify availability for version {} in channel {}", getVersionHash(), channel, ex);
            available = false;
        }
    }

    public void fetchFiles(List<String> manifest)
    {
        if (files.isEmpty())
        {
            return;
        }

        final Pattern filePattern = Pattern.compile("^([A-z0-9-]+\\.[A-z0-9]+)");
        final Pattern hashPattern = Pattern.compile("^[a-z0-9]{32}$");

        String name = null;
        String hash = null;

        for (String line : manifest)
        {
            if (filePattern.matcher(line).find())
            {
                name = line;
            }
            else if (hashPattern.matcher(line).find())
            {
                hash = line;
                files.put(name, hash);
            }
        }
    }

    public void fetchFiles(JRGConfiguration configuration)
    {
        // We already got the files we needed
        if (!files.isEmpty())
        {
            return;
        }

        Main.getLogger().info("Getting files for version {}", getVersionHash());

        /* Mac clients are stored differently compared to Windows clients. Instead of things like content being split up
           into their own ZIP files, everything is stored in a single ZIP file which makes archiving them easier. The
           downside is that there isn't a quick and efficient way to get files and their hashes, so we end up having to
           download massive ZIP files every single time. */
        if (type.isMac())
        {
            if (type == VersionType.MAC_PLAYER)
            {
                files.put("Roblox.dmg", "");
                files.put("RobloxPlayer.zip", "");
            }
            else
            {
                files.put("RobloxStudioApp.zip", "");
                files.put("RobloxStudio.zip", "");
                files.put("RobloxStudio.dmg", "");
            }

            verifyAvailability(configuration);
        }
        else
        {
            if (!configuration.isFetchingManifestForFiles())
            {
                if (type.name().contains("STUDIO"))
                {
                    files.put("BuiltInPlugins.zip", "");
                    files.put("BuiltInStandalone.zip", "");
                    files.put("content-luapackages.zip", "");
                    files.put("content-qt_translations.zip", "");
                    files.put("content-translations.zip", "");
                    files.put("content-scripts.zip", "");
                    files.put("content-models.zip", "");
                    files.put("LibrariesQt5.zip", "");
                    files.put("Qml.zip", "");
                    files.put("RobloxStudioLauncherBeta.exe", "");
                    files.put("RobloxStudio.zip", "");
                    files.put("ssl.zip", "");
                    files.put("Plugins.zip", "");
                }

                files.put("content-avatar.zip", "");
                files.put("content-fonts.zip", "");
                files.put("content-music.zip", "");
                files.put("content-platform-fonts.zip", "");
                files.put("content-terrain.zip", "");
                files.put("content-particles.zip", "");
                files.put("content-sky.zip", "");
                files.put("content-sounds.zip", "");
                files.put("content-textures.zip", "");
                files.put("content-textures2.zip", "");
                files.put("content-textures3.zip", "");
                files.put("Libraries.zip", "");
                files.put("NPRobloxProxy.zip", "");
                files.put("redist.zip", "");
                files.put("Roblox.exe", "");
                files.put("RobloxApp.zip", "");
                files.put("RobloxProxy.zip", "");
                files.put("shaders.zip", "");

                available = true;
                return;
            }

            try
            {
                final Pattern filePattern = Pattern.compile("^([A-z0-9-]+\\.[A-z0-9]+)");
                final Pattern hashPattern = Pattern.compile("^[a-z0-9]{32}$");

                String name = null;
                String hash = null;

                final String request = HttpUtil.get("https://" + configuration.getDomain() + "/"
                                + (channel.equalsIgnoreCase("live") ? "" : "channel/" + channel + "/")
                                + (isCjv() ? "cjv/" : "") + getVersionHash() + "-rbxPkgManifest.txt");

                if (request.contains("AccessDenied"))
                {
                    available = false;
                    return;
                }

                for (String line : request.split("\r\n"))
                {
                    if (filePattern.matcher(line).find())
                    {
                        name = line;
                    }
                    else if (hashPattern.matcher(line).find())
                    {
                        hash = line;
                        files.put(name, hash);
                    }
                }

                files.put("rbxPkgManifest.txt", "");
                files.put("rbxManifest.txt", "");

                available = true;
            }
            catch (IOException | InterruptedException ex)
            {
                Main.getLogger().warn("Failed to get manifest for version {} in channel {}", getVersionHash(), channel, ex);
                available = false;
            }
        }
    }

    @Override
    public String toString()
    {
        if (type == null)
        {
            Main.getLogger().info(getFullVersionString());
        }

        return String.format("New %s %s at %s, file version: %s....Done!", type.getFriendlyName(), getVersionHash(), dateFormat.format(new Date(getDeployDate())), getFileVersion());
    }

    public static RBXVersion fromString(String str, String channel, List<String> blacklist, boolean mac, boolean cjv) throws ParseException
    {
        final Matcher matcher = versionPattern.matcher(str);
        if (!matcher.find() || blacklist.contains(matcher.group(2)))
        {
            return null;
        }

        long time;

        try
        {
            time = dateFormat.parse(matcher.group(3)).getTime();
        }
        catch (NumberFormatException ex)
        {
            time = Instant.now().toEpochMilli();
        }

        // De-duplication
        blacklist.add(matcher.group(2));

        return builder()
                .fullVersionString(str)
                .type(VersionType.find(matcher.group(1), mac))
                .versionHash(matcher.group(2))
                .deployDate(time)
                .fileVersion(Optional.ofNullable(matcher.group(6)).orElse("0, 0, 0, 0"))
                .channel(channel)
                .cjv(cjv)
                .build();
    }

    public static RBXVersion fromClientSettings(VersionType type, String hash, long deployDate, String version, String channel, List<String> blacklist, boolean cjv)
    {
        if (hash == null || blacklist.contains(hash))
        {
            return null;
        }

        return builder()
                .fullVersionString("Obtained from elsewhere")
                .type(type)
                .versionHash(hash)
                .deployDate(deployDate)
                .fileVersion(version.replaceAll("\\.", ", "))
                .channel(channel)
                .cjv(cjv)
                .build();
    }

    @RequiredArgsConstructor
    @Getter
    public enum VersionType
    {
        MAC_PLAYER("Client", "MacPlayer", new String[]{"Client", "MacPlayer"}, true, false),
        MAC_STUDIO("Studio", "MacStudio", new String[]{"Studio", "MacStudio"}, true, false),
        MAC_STUDIO_CJV("Studio", "MacStudio", new String[]{"MacStudioCJV"}, true, false),
        WINDOWS_PLAYER("WindowsPlayer", "WindowsPlayer", new String[]{"Client", "WindowsPlayer", "SetupVersion"}, false,false),
        WINDOWS_STUDIO_X86("Studio", "WindowsStudio", new String[]{"WindowsStudio", "Studio", "Studio-SetupVersion"}, false, false),
        WINDOWS_STUDIO_CJV_X86("Studio", "WindowsStudio", new String[]{"Studio", "WindowsStudioCJV"}, false, true),
        WINDOWS_STUDIO_X64("Studio64", "WindowsStudio64", new String[]{"WindowsStudio64", "Studio64"}, false, false),
        WINDOWS_STUDIO_CJV_X64("Studio64", "WindowsStudio64", new String[]{"Studio64", "WindowsStudio64CJV"}, false, true);

        private final String friendlyName;
        private final String clientSettingsName;
        private final String[] acceptableNames;
        private final boolean mac;
        private final boolean cjv;

        public static VersionType find(String friendlyName, boolean mac, boolean cjv)
        {
            return Arrays.stream(values()).filter(type -> (type.friendlyName.equalsIgnoreCase(friendlyName)
                    || Arrays.stream(type.acceptableNames).anyMatch(name -> name.equalsIgnoreCase(friendlyName)))
                    && type.mac == mac && type.cjv == cjv).findAny().orElse(null);
        }

        public static VersionType find(String friendlyName, boolean mac)
        {
            return find(friendlyName, mac, false);
        }
    }
}
