#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#

JAVAROOT=/usr/java/jdk1.5.0_01
JAVAC=${JAVAROOT}/bin/javac

#JAVAROOT=/usr/java/j2sdk1.4.2
#JAVAC=jikes +P -source 1.4 -classpath ${JAVAROOT}/jre/lib/rt.jar

Arrows.jar: Arrows.prejava Makefile META-INF/MANIFEST.MF
	javacpp ${JAVAC} Arrows.prejava
	javarenumber -v 0 *.class
	/bin/rm -rf scratch
	mkdir scratch
	cp *.class scratch
	cp Arrows.prejava scratch/Arrows.java
	cp Makefile scratch
	cp -a RCS scratch
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../Arrows.jar ../META-INF/MANIFEST.MF *.class Arrows.java Makefile RCS)
clean:
	/bin/rm -rf *.jar *.class scratch *.java.lines *.html *.css

doc:
	${JAVAROOT}/bin/javadoc Arrows.java
send: Arrows.jar
	scp Arrows.jar hatch@plunk.org:tmp/.
