# PDIoT# android
cuberGraph has finished the graph function in cuber Android code.
features: 1. no windows(since it has to plot every point).
	  2. plot page cannot see the steps, and everytime it comes to plot page, the graph goes from zero.
	  3. it run pca to all data, so some bad data may influence the subsequent.

cuberGraphWindow uses the window again and update the graph every second(translate 10 points once and plot them in one second)

cuberGraphNowindow keep updating step without windows but try to drop the bad data.

