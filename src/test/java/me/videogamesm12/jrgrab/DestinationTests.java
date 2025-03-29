package me.videogamesm12.jrgrab;

import me.videogamesm12.jrgrab.data.JRGConfiguration;
import me.videogamesm12.jrgrab.data.RBXVersion;
import me.videogamesm12.jrgrab.destinations.DeployHistoryDestination;
import me.videogamesm12.jrgrab.grabbers.DeployGrabber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DestinationTests
{
	/**
	 * This tests to make sure that DeployHistory files from a previous jrgrab run does not lose any information
	 */
	@Test
	public void testLosslessDeployHistoryDestination()
	{
		// Stage 1 - Pre-grab, which gets a DeployHistory file for reference, saves it, then dumps it to disk
		JRGConfiguration configuration = JRGConfiguration.builder().build();
		List<RBXVersion> stage1 = new DeployGrabber(configuration).getVersions("zprojectloadingscreendeployment", new ArrayList<>());
		DeployHistoryDestination destination = new DeployHistoryDestination(configuration);
		destination.sendVersions(stage1, "zprojectloadingscreendeployment");

		// Stage 2 - Post-grab
		configuration = JRGConfiguration.builder().file("zprojectloadingscreendeployment/DeployHistory.txt").build();
		List<RBXVersion> stage2 = new DeployGrabber(configuration).getVersions("zprojectloadingscreendeployment", new ArrayList<>());

		// Nuke the files
		new File("zprojectloadingscreendeployment/DeployHistory.txt").delete();
		new File("zprojectloadingscreendeployment").delete();

		Assertions.assertLinesMatch(stage1.stream().map(RBXVersion::toString).toList(), stage2.stream().map(RBXVersion::toString).toList());
	}
}
