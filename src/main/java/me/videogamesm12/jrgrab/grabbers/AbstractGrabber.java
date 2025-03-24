package me.videogamesm12.jrgrab.grabbers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.util.List;

@RequiredArgsConstructor
@Getter
public abstract class AbstractGrabber
{
    private final JRGConfiguration config;
    private final boolean parallelSupported;

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
