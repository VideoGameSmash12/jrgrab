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

public class SNCGitHubTrackerGrabber extends AbstractGrabber
{
    private final File folder = new File("temp");
    private final List<RBXVersion> found = new ArrayList<>();
    private Git repository = null;

    public SNCGitHubTrackerGrabber(JRGConfiguration config)
    {
        super(config, false);
    }

    @Override
    public void setup()
    {
        // Sanity checks
        if (getConfig().isMac())
        {
            throw new IllegalArgumentException("Mac clients cannot be scraped using this grabber");
        }

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

        Main.getLogger().info("Cloning repository (this will take some time)");
        try
        {
            repository = Git.cloneRepository()
                    .setURI(getConfig().getRepositoryUrl() != null ? getConfig().getRepositoryUrl() : "https://github.com/SNCPlay42/roblox-api-dumps.git")
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
            final Pattern pattern = Pattern.compile("^([A-z0-9-]+): (version-[A-z0-9]+)$");

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

                final File versionFile = new File(folder, "info.txt");

                if (versionFile.exists())
                {
                    try (Scanner scanner = new Scanner(versionFile))
                    {
                        while (scanner.hasNext())
                        {
                            final String line = scanner.nextLine();
                            final Matcher matcher = pattern.matcher(line);
                            if (!matcher.find())
                            {
                                continue;
                            }

                            final RBXVersion.VersionType type = RBXVersion.VersionType.find(matcher.group(1), false, false);
                            if (type == null)
                            {
                                continue;
                            }

                            final String hash = matcher.group(2);
                            final String version = "0, 0, 0, 0";
                            final long deployDate = ((long) commit.getCommitTime()) * 1000;

                            if (found.stream().noneMatch(client -> client.getVersionHash().equalsIgnoreCase(hash)))
                            {
                                found.add(RBXVersion.fromClientSettings(type, hash, deployDate, version, "live", new ArrayList<>(), false));
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        Main.getLogger().error("Failed to get clients in commit {}", commit.getName(), ex);
                    }
                }
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
        if (repository == null || !channel.equalsIgnoreCase("live"))
        {
            return List.of();
        }

        if (found.isEmpty())
        {
            getVersions();
            cleanUp();
        }

        return found.stream().filter(Objects::nonNull).sorted(Comparator.comparingLong(RBXVersion::getDeployDate)).toList();
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
