#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#

#JAVAROOT=/usr/java/jdk1.5.0_01
JAVAROOT=/usr/java/jdk1.5.0
JAVAC=${JAVAROOT}/bin/javac

#JAVAROOT=/usr/java/j2sdk1.4.2
#JAVAC=jikes +P -source 1.4 -classpath ${JAVAROOT}/jre/lib/rt.jar

donhatchsw.jar:

# XXX Poly is a prefix of PolyCSG, so the * thing is not robust

com/donhatchsw/util/MyMath.class: com/donhatchsw/util/MyMath.prejava
	javacpp ${JAVAC} com/donhatchsw/util/MyMath.prejava
	javarenumber -v 0 com/donhatchsw/util/MyMath*.class
com/donhatchsw/util/Arrays.class: com/donhatchsw/util/Arrays.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Arrays.prejava
	javarenumber -v 0 com/donhatchsw/util/Arrays*.class
com/donhatchsw/util/VecMath.class: com/donhatchsw/util/VecMath.prejava
	javacpp ${JAVAC} com/donhatchsw/util/VecMath.prejava
	javarenumber -v 0 com/donhatchsw/util/VecMath*.class
com/donhatchsw/util/LinearProgramming.class: com/donhatchsw/util/LinearProgramming.prejava
	javacpp ${JAVAC} com/donhatchsw/util/LinearProgramming.prejava
	javarenumber -v 0 com/donhatchsw/util/LinearProgramming*.class
com/donhatchsw/util/SortStuff.class: com/donhatchsw/util/SortStuff.prejava
	javacpp ${JAVAC} com/donhatchsw/util/SortStuff.prejava
	javarenumber -v 0 com/donhatchsw/util/SortStuff*.class
com/donhatchsw/util/Minimizer.class: com/donhatchsw/util/Minimizer.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Minimizer.prejava
	javarenumber -v 0 com/donhatchsw/util/Minimizer*.class
com/donhatchsw/util/MyPanel.class: com/donhatchsw/util/MyPanel.prejava
	javacpp ${JAVAC} com/donhatchsw/util/MyPanel.prejava
	javarenumber -v 0 com/donhatchsw/util/MyPanel*.class

com/donhatchsw/util/TriangulationOptimizer.class: com/donhatchsw/util/TriangulationOptimizer.prejava
	javacpp ${JAVAC} com/donhatchsw/util/TriangulationOptimizer.prejava
	javarenumber -v 0 com/donhatchsw/util/TriangulationOptimizer*.class
com/donhatchsw/util/Triangulator.class: com/donhatchsw/util/Triangulator.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Triangulator.prejava
	javarenumber -v 0 com/donhatchsw/util/Triangulator*.class
com/donhatchsw/util/Poly.class: com/donhatchsw/util/Poly.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Poly.prejava
	javarenumber -v 0 com/donhatchsw/util/Poly*.class
com/donhatchsw/util/CSG.class: com/donhatchsw/util/CSG.prejava
	javacpp ${JAVAC} com/donhatchsw/util/CSG.prejava
	javarenumber -v 0 com/donhatchsw/util/CSG*.class
com/donhatchsw/util/PolyCSG.class: com/donhatchsw/util/PolyCSG.prejava
	javacpp ${JAVAC} com/donhatchsw/util/PolyCSG.prejava
	javarenumber -v 0 com/donhatchsw/util/PolyCSG*.class

com/donhatchsw/util/Arrows.class: com/donhatchsw/util/Arrows.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Arrows.prejava
	javarenumber -v 0 com/donhatchsw/util/Arrows*.class

donhatchsw.jar: Makefile META-INF/MANIFEST.MF com/donhatchsw/util/MyMath.class com/donhatchsw/util/Arrays.class com/donhatchsw/util/VecMath.class com/donhatchsw/util/LinearProgramming.class com/donhatchsw/util/SortStuff.class com/donhatchsw/util/Minimizer.class com/donhatchsw/util/MyPanel.class com/donhatchsw/util/TriangulationOptimizer.class com/donhatchsw/util/Triangulator.class com/donhatchsw/util/Poly.class com/donhatchsw/util/CSG.class  com/donhatchsw/util/PolyCSG.class com/donhatchsw/util/Arrows.class
	/bin/rm -rf scratch
	mkdir scratch
	cp -a Makefile RCS com scratch
	mv -f scratch/com/donhatchsw/util/MyMath.prejava scratch/com/donhatchsw/util/MyMath.java
	mv -f scratch/com/donhatchsw/util/Arrays.prejava scratch/com/donhatchsw/util/Arrays.java
	mv -f scratch/com/donhatchsw/util/VecMath.prejava scratch/com/donhatchsw/util/VecMath.java
	mv -f scratch/com/donhatchsw/util/LinearProgramming.prejava scratch/com/donhatchsw/util/LinearProgramming.java
	mv -f scratch/com/donhatchsw/util/SortStuff.prejava scratch/com/donhatchsw/util/SortStuff.java
	mv -f scratch/com/donhatchsw/util/Minimizer.prejava scratch/com/donhatchsw/util/Minimizer.java
	mv -f scratch/com/donhatchsw/util/MyPanel.prejava scratch/com/donhatchsw/util/MyPanel.java
	mv -f scratch/com/donhatchsw/util/TriangulationOptimizer.prejava scratch/com/donhatchsw/util/TriangulationOptimizer.java
	mv -f scratch/com/donhatchsw/util/Triangulator.prejava scratch/com/donhatchsw/util/Triangulator.java
	mv -f scratch/com/donhatchsw/util/Poly.prejava scratch/com/donhatchsw/util/Poly.java
	mv -f scratch/com/donhatchsw/util/CSG.prejava scratch/com/donhatchsw/util/CSG.java
	mv -f scratch/com/donhatchsw/util/PolyCSG.prejava scratch/com/donhatchsw/util/PolyCSG.java
	mv -f scratch/com/donhatchsw/util/Arrows.prejava scratch/com/donhatchsw/util/Arrows.java
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../donhatchsw.jar ../META-INF/MANIFEST.MF com/donhatchsw/util/*.class com/donhatchsw/util/*.java com/donhatchsw/util/RCS Makefile RCS)

clean:
	/bin/rm -rf *.jar com/donhatchsw/util/*.class scratch com/donhatchsw/util/*.java.lines *.html com/donhatchsw/util/*.html *.css

doc: donhatchsw.jar
	${JAVAROOT}/bin/javadoc com/donhatchsw/util/*.java
send: doc
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
	(cd ..; scp -r donhatchsw hatch@plunk.org:public_html/private/.)
senddoc: doc
	scp -r *.html *.css resources com hatch@plunk.org:public_html/donhatchsw/javadoc/.
