#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#

AssetGraphStuff.jar: AssetGraphStuff.prejava Makefile
	javacpp /usr/java/jdk1.5.0_01/bin/javac AssetGraphStuff.prejava
	javarenumber -v 0 *.class
	/bin/rm -rf scratch
	mkdir scratch
	cp *.class scratch
	cp AssetGraphStuff.prejava scratch/AssetGraphStuff.java
	cp Makefile scratch
	(cd scratch; jar -cfm ../AssetGraphStuff.jar ../META-INF/MANIFEST.MF *.class AssetGraphStuff.java Makefile)
clean:
	/bin/rm -rf *.jar *.class scratch *.java.lines *.html *.css

doc:
	/usr/java/jdk1.5.0_01/bin/javadoc AssetGraphStuff.java
send: AssetGraphStuff.jar
	scp AssetGraphStuff.jar hatch@plunk.org:tmp/.
