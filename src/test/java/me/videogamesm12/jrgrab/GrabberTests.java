package me.videogamesm12.jrgrab;

import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.grabbers.LegacyGrabber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class GrabberTests
{
	@Test
	public void testLegacyGrabber()
	{
		JRGConfiguration config = JRGConfiguration.builder().source(JRGConfiguration.Source.LEGACY).build();
		LegacyGrabber legacyGrabber = new LegacyGrabber(config);
		Assertions.assertEquals("version-6552be68b05d41a5", legacyGrabber.getVersions("live", Collections.emptyList()).get(0).getVersionHash());
	}
}
