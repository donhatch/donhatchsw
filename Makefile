#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#

#JAVAROOT=/usr/java/jdk1.5.0_01
#JAVAC=${JAVAROOT}/bin/javac

JAVAROOT=/usr/java/j2sdk1.4.2
JAVAC=jikes +P -source 1.4 -classpath ${JAVAROOT}/jre/lib/rt.jar

AssetGraphStuff.jar: AssetGraphStuff.prejava Makefile
	javacpp ${JAVAC} AssetGraphStuff.prejava
	javarenumber -v 0 *.class
	/bin/rm -rf scratch
	mkdir scratch
	cp *.class scratch
	cp AssetGraphStuff.prejava scratch/AssetGraphStuff.java
	cp Makefile scratch
	cp -a RCS scratch
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../AssetGraphStuff.jar ../META-INF/MANIFEST.MF *.class AssetGraphStuff.java Makefile RCS)
clean:
	/bin/rm -rf *.jar *.class scratch *.java.lines *.html *.css

doc:
	${JAVAROOT}/bin/javadoc AssetGraphStuff.java
send: AssetGraphStuff.jar
	scp AssetGraphStuff.jar hatch@plunk.org:tmp/.
