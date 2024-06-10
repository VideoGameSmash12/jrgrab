# jrgrab
jrgrab is a Java-based reimplementation of [rgrab](https://github.com/VideoGameSmash12/rgrab) (another project of mine)
with a focus on speed and flexibility. In addition to being able to grab clients from DeployHistory.txt, it is now also
possible to scrape GitHub trackers like [MaximumADHD's Roblox-Client-Tracker](https://github.com/MaximumADHD/Roblox-Client-Tracker),
and [bluepilledgreat's Roblox-DeployHistory-Tracker](https://github.com/bluepilledgreat/Roblox-DeployHistory-Tracker).

In addition to sending the files to an aria2 daemon, jrgrab also supports dumping information about what it finds to
JSON files or even a DeployHistory-formatted file.

## Performance Benchmarks (vs. rgrab)
I timed the task of scraping various channels between jrgrab and the original rgrab. These metrics are not of download
times, but rather how long it took for each application to be executed from start to finish. 

| Channel   | rgrab (Python) | jrgrab (Java) | Difference   |
|-----------|----------------|---------------|--------------|
| gametest2 | 608.280s       | 77.800s       | **530.480s** |
| live      | 119.880s       | 17.554s       | **102.326s** |
| zlive     | 22.545s        | 4.765s        | 17.780s      |