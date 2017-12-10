Signals
==================

This is the source of Signals. It's licenced under GPLv3, so have fun. Also have fun learning from the code where possible!
I'm open to pull requests.

To download Signals, head to [Curse](https://minecraft.curseforge.com/projects/signals)

=======
Developing with Signals
=======
If you want to use the Signals API in your mod, it's really easy to include the mod or API to your development environment, as the mod has a maven.

In your build.gradle, add:

	repositories {
		maven {
			name = "K4 Maven"
			url = "http://maven.k-4u.nl/"
		}
	}

	dependencies {
		deobfCompile "signals:Signals-1.12.2:1.1.0-1:universal"
	}

It should be clear that the version number used in the 'deobfCompile' is an example, to see which versions you can use, go to http://maven.k-4u.nl/signals/

The com.minemaarten.signals.api.* package should contain everything you need. If you are missing something, feel free to open an issue requesting an API hook.

=======
Contributing to Signals
=======
If you're planning to contribute to the Signals's mods source, the best thing you can do is fork this github repository, and run 'gradle setupDecompWorkspace idea/eclipse' on the build.gradle file in this repository.

After you've made changes, do a pull request :)

For more details on pull-requests see [link](https://help.github.com/articles/using-pull-requests/)
