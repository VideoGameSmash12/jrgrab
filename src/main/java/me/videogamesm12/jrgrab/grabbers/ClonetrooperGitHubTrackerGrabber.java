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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * <h1>ClonetrooperGitHubTrackerGrabber</h1>
 * <p>Grabber that scrapes the commit history of MaximumADHD's Roblox-Client-Tracker GitHub repository to find
 * versions of the client.</p>
 * @implNote <p>Only supports finding Studio and Studio64 clients for Windows.</p>
 */
public class ClonetrooperGitHubTrackerGrabber extends AbstractGrabber
{
    private final File folder = new File("temp");
    private final List<RBXVersion> found = new ArrayList<>();
    private Git repository = null;

    public ClonetrooperGitHubTrackerGrabber(JRGConfiguration config)
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
                    .setURI(getConfig().getRepositoryUrl() != null ? getConfig().getRepositoryUrl() : "https://github.com/MaximumADHD/Roblox-Client-Tracker.git")
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

    public void getVersions(List<String> known)
    {
        try
        {
            final List<RevCommit> commits = StreamSupport.stream(repository.log().call().spliterator(), false).toList();

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

                final File versionFile = new File(folder, "version.txt");
                final File guidVersionFile = new File(folder, "version-guid.txt");
                final File pkgManifestFile = new File(folder, "rbxPkgManifest.txt");

                final String versionGuid;
                final String version;
                final long deployDate;

                if (!versionFile.exists())
                {
                    return;
                }

                if (guidVersionFile.exists())
                {
                    try (BufferedReader versionReader = Files.newBufferedReader(versionFile.toPath());
                         BufferedReader guidVersionReader = Files.newBufferedReader(guidVersionFile.toPath()))
                    {
                        versionGuid = guidVersionReader.readLine();
                        version = versionReader.readLine();
                    }
                    catch (IOException ex)
                    {
                        Main.getLogger().error("Failed to read version txt files", ex);
                        return;
                    }
                }
                else
                {
                    try (BufferedReader reader = Files.newBufferedReader(versionFile.toPath()))
                    {
                        versionGuid = reader.readLine();
                    }
                    catch (IOException ex)
                    {
                        Main.getLogger().error("Failed to read version.txt", ex);
                        return;
                    }

                    version = "0, 0, 0, 0";
                }
                deployDate = ((long) commit.getCommitTime()) * 1000L;

                if (version.isBlank() || version.isEmpty())
                {
                    return;
                }

                final RBXVersion rbxVersion;

                if (found.stream().noneMatch(v -> v.getVersionHash().equalsIgnoreCase(versionGuid)))
                {
                    rbxVersion = RBXVersion.fromClientSettings(
                            commit.getCommitTime() > 1569391200 ? RBXVersion.VersionType.WINDOWS_STUDIO_X64 : RBXVersion.VersionType.WINDOWS_STUDIO_X86,
                            versionGuid, deployDate, version, "live", known, false);
                    found.add(rbxVersion);
                }
                else
                {
                    // We know it's there, shut up Intellij
                    rbxVersion = found.stream().filter(v -> v.getVersionHash().equalsIgnoreCase(versionGuid)).findAny().get();
                }

                if (pkgManifestFile.exists())
                {
                    try (BufferedReader reader = Files.newBufferedReader(pkgManifestFile.toPath()))
                    {
                        rbxVersion.fetchFiles(reader.lines().toList());
                    }
                    catch (IOException ex)
                    {
                        Main.getLogger().error("Failed to read rbxPkgManifest.txt for version {}", versionGuid, ex);
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
    public List<RBXVersion> getVersions(String channel, List<String> known)
    {
        if (repository == null || !channel.equalsIgnoreCase("live"))
        {
            return List.of();
        }

        if (found.isEmpty())
        {
            getVersions(known);
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
