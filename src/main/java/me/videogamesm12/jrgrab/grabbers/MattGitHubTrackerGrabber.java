package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.Main;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
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

/**
 * <h1>MattGitHubTrackerGrabber</h1>
 * <p>Grabber that scrapes the commit history of bluepilledgreat's Roblox-DeployHistory-Tracker repository to find
 * versions of the client.</p>
 */
public class MattGitHubTrackerGrabber extends AbstractGrabber
{
    private final File folder = new File("temp");
    private final Map<String, List<RBXVersion>> channels = new HashMap<>();
    private Git repository = null;

    public MattGitHubTrackerGrabber(JRGConfiguration config)
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

        Main.getLogger().info("Cloning repository");
        try
        {
            repository = Git.cloneRepository()
                    .setURI(getConfig().getRepositoryUrl() != null ? getConfig().getRepositoryUrl() : "https://github.com/bluepilledgreat/Roblox-DeployHistory-Tracker.git")
                    .setDirectory(folder)
                    .call();

            if (getConfig().getBranch() != null)
            {
                repository.checkout()
                        .setName(getConfig().getBranch())
                        .setCreateBranch(true)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/" + getConfig().getBranch())
                        .call();
            }

            // Get ALL the channels
            if (getConfig().getChannels().contains("*"))
            {
                List<String> channels = new ArrayList<>();

                Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file -> file.getName().endsWith(".txt"))
                        .toList().forEach(channel ->
                {
                    final String name = channel.getName().toLowerCase().replace(".txt", "");
                    channels.add(name);
                });

                getConfig().setChannels(channels);
            }
        }
        catch (GitAPIException ex)
        {
            Main.getLogger().error("Failed to clone repository", ex);
        }
    }

    public void getVersions(List<String> known)
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

                // Matt's tracker is structured like this: every channel has their own dedicated txt file containing the
                //  latest version hashes for Mac and Windows clients of their respective types along with their version
                //  which is the same as what is reported by DeployHistory, but formatted differently.
                Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(file -> file.getName().endsWith(".txt")
                        && getConfig().getChannels().contains(file.getName().toLowerCase().replace(".txt", ""))).toList().forEach(channel ->
                {
                    final String name = channel.getName().toLowerCase().replace(".txt", "");

                    if (!channels.containsKey(name))
                    {
                        channels.put(name, new ArrayList<>());
                    }

                    final List<RBXVersion> storage = channels.get(name);
                    try (final Scanner scanner = new Scanner(channel))
                    {
                        while (scanner.hasNext())
                        {
                            final String line = scanner.nextLine();
                            final Matcher matcher = pattern.matcher(line);
                            if (!matcher.find())
                            {
                                continue;
                            }

                            final RBXVersion.VersionType type = RBXVersion.VersionType.find(matcher.group(1), getConfig().isMac(), getConfig().isCjv());
                            if (type == null)
                            {
                                continue;
                            }

                            final String hash = matcher.group(2);
                            final String version = matcher.group(3);
                            final long deployDate = ((long) commit.getCommitTime()) * 1000;

                            if (storage.stream().filter(Objects::nonNull).noneMatch(client -> client.getVersionHash().equalsIgnoreCase(hash)))
                            {
                                storage.add(RBXVersion.fromClientSettings(type, hash, deployDate, version, name, known, type.isCjv()));
                            }
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
    public List<RBXVersion> getVersions(String channel, List<String> known)
    {
        if (repository == null)
        {
            return List.of();
        }

        if (channels.isEmpty())
        {
            getVersions(known);
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
