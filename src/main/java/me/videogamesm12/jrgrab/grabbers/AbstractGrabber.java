package me.videogamesm12.jrgrab.grabbers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.util.List;

@Getter
public abstract class AbstractGrabber
{
    private final JRGConfiguration config;
    private final boolean macSupported;

    public AbstractGrabber(JRGConfiguration configuration, boolean macSupported)
    {
        // Sanity checks are performed here now.
        if (!macSupported && configuration.isMac())
        {
            throw new IllegalArgumentException("Mac clients cannot be scraped using this grabber");
        }

        this.config = configuration;
        this.macSupported = macSupported;
    }

    public void setup()
    {
    }

    /**
     * Fetch the versions and return them in a list.
     * @param channel   The channel name
     * @param blacklist A list of already known version hashes
     * @return          A list of RBXVersion hashes.
     */
    public abstract List<RBXVersion> getVersions(String channel, List<String> blacklist);
}
