package me.videogamesm12.jrgrab.destinations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class AbstractDestination
{
    private final JRGConfiguration config;

    /**
     * Handle a list of versions along with their intended channel.
     * @param version   A list of RBXVersion instances.
     * @param channel   The channel's name
     */
    public abstract void sendVersions(List<RBXVersion> version, String channel);
}
