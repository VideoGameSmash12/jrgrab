package me.videogamesm12.jrgrab.grabbers;

import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;

import java.util.List;

public class UnimplementedGrabber extends AbstractGrabber
{
    public UnimplementedGrabber(JRGConfiguration config)
    {
        super(config);
    }

    @Override
    public void setup()
    {
    }

    @Override
    public List<RBXVersion> getVersions()
    {
        return List.of();
    }
}
