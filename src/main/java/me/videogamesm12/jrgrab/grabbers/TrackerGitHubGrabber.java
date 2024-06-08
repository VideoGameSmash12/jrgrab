package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class TrackerGitHubGrabber extends AbstractGrabber
{
    private final File folder = new File("temp");
    private final Map<String, List<RBXVersion>> channels = new HashMap<>();
    private Git repository = null;

    public TrackerGitHubGrabber(JRGConfiguration config)
    {
        super(config, false);
    }

    @Override
    public void setup()
    {
        if (folder.isDirectory())
        {
            try
            {
                FileUtils.deleteDirectory(folder);
            }
            catch (IOException ex)
            {
                Main.getLogger().error("Failed to delete existing temporary directory", ex);
            }
        }

        try
        {
            repository = Git.cloneRepository()
                    .setURI("https://github.com/bluepilledgreat/Roblox-DeployHistory-Tracker.git")
                    .setDirectory(folder)
                    .call();
        }
        catch (GitAPIException ex)
        {
            Main.getLogger().error("Failed to clone repository", ex);
        }
    }

    public void getVersions()
    {
        try
        {
            final List<RevCommit> commits = StreamSupport.stream(repository.log().call().spliterator(), false)
                    .sorted(Comparator.comparingInt(RevCommit::getCommitTime)).toList();
            final Pattern pattern = Pattern.compile("([A-z0-9]+): (version-[A-z0-9]+) \\[([0-9.]+)]");

            commits.forEach(commit ->
            {
                try
                {
                    repository.reset().setMode(ResetCommand.ResetType.HARD).setRef(commit.getName()).call();
                }
                catch (GitAPIException ex)
                {
                    Main.getLogger().error("Failed to revert repository to commit {}", commit.getName(), ex);
                    return;
                }

                Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file -> file.getName().endsWith(".txt")
                        && getConfig().getChannels().contains(file.getName().toLowerCase().replace(".txt", ""))
                        || getConfig().getChannels().contains("*")).toList().forEach(channel ->
                {
                    final String name = channel.getName().toLowerCase().replace(".txt", "");

                    if (!channels.containsKey(name))
                    {
                        channels.put(name, new ArrayList<>());
                    }

                    final List<RBXVersion> storage = channels.get(name);
                    try
                    {
                        final Scanner scanner = new Scanner(channel);

                        while (scanner.hasNext())
                        {
                            final String line = scanner.nextLine();
                            final Matcher matcher = pattern.matcher(line);
                            if (!matcher.find())
                            {
                                continue;
                            }

                            final RBXVersion.VersionType type = RBXVersion.VersionType.find(matcher.group(1), getConfig().isMac(), false);
                            if (type == null)
                            {
                                continue;
                            }

                            final String hash = matcher.group(2);
                            final String version = matcher.group(3);
                            final long deployDate = ((long) commit.getCommitTime()) * 1000;

                            storage.add(RBXVersion.fromClientSettings(type, hash, deployDate, version, name, new ArrayList<>(), type.isCjv()));
                        }
                    }
                    catch (IOException ex)
                    {
                        Main.getLogger().error("Failed to read channel {} in commit {}", name, commit.getName(), ex);
                    }
                });
            });
        }
        catch (GitAPIException ex)
        {
            Main.getLogger().error("Failed to get versions", ex);
        }
    }

    @Override
    public List<RBXVersion> getVersions(String channel)
    {
        if (repository == null)
        {
            return List.of();
        }

        if (channels.isEmpty())
        {
            getVersions();
            cleanUp();
        }

        return channels.getOrDefault(channel, List.of()).stream().filter(Objects::nonNull).toList();
    }

    public void cleanUp()
    {
        try
        {
            FileUtils.deleteDirectory(folder);
        }
        catch (IOException ex)
        {
            Main.getLogger().error("Failed to delete temporary directory", ex);
        }
    }
}
