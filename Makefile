#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#


# Sticking points:
#    - mc4d/GenericGlue.java - wants to use the version of showInputDialog with an initial string, doesn't exist in 1.3
#    - Arrows uses javax.swing.Icon or something, doesn't exist in 1.2

# To use cookies, need netscape.javascript.JSObject
# which is in /usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar



CLASSPATH=/usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar

#JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAC=${JAVAROOT}/bin/javac -target 1.1 classpath=${CLASSPATH}

JAVAROOT=/usr/java/j2sdk1.4.2
#JAVAROOT=/usr/java/jdk1.5.0
#JAVAROOT=/usr/java/jdk1.5.0_01
JAVAC=${JAVAROOT}/bin/javac -source 1.2 -target 1.1 -g -classpath ${CLASSPATH}:.

#JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAC=jikes +P -source 1.4 -classpath ${JAVAROOT}/jre/lib/rt.jar:${CLASSPATH}

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
com/donhatchsw/compat/Double.class: com/donhatchsw/compat/Double.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/Double.prejava
	javarenumber -v 0 com/donhatchsw/compat/Double*.class
com/donhatchsw/compat/Float.class: com/donhatchsw/compat/Float.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/Float.prejava
	javarenumber -v 0 com/donhatchsw/compat/Float*.class
com/donhatchsw/compat/Boolean.class: com/donhatchsw/compat/Boolean.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/Boolean.prejava
	javarenumber -v 0 com/donhatchsw/compat/Boolean*.class
com/donhatchsw/compat/Misc.class: com/donhatchsw/compat/Misc.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/Misc.prejava
	javarenumber -v 0 com/donhatchsw/compat/Misc*.class
com/donhatchsw/compat/regex.class: com/donhatchsw/compat/regex.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/regex.prejava
	javarenumber -v 0 com/donhatchsw/compat/regex*.class
com/donhatchsw/compat/Format.class: com/donhatchsw/compat/Format.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/Format.prejava
	javarenumber -v 0 com/donhatchsw/compat/Format*.class
com/donhatchsw/util/SortStuff.class: com/donhatchsw/util/SortStuff.prejava
	javacpp ${JAVAC} com/donhatchsw/util/SortStuff.prejava
	javarenumber -v 0 com/donhatchsw/util/SortStuff*.class
com/donhatchsw/util/Minimizer.class: com/donhatchsw/util/Minimizer.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Minimizer.prejava
	javarenumber -v 0 com/donhatchsw/util/Minimizer*.class
com/donhatchsw/util/MyPanel.class: com/donhatchsw/util/MyPanel.prejava
	javacpp ${JAVAC} com/donhatchsw/util/MyPanel.prejava
	javarenumber -v 0 com/donhatchsw/util/MyPanel*.class
com/donhatchsw/util/FuzzyPointHashTable.class: com/donhatchsw/util/FuzzyPointHashTable.prejava
	javacpp ${JAVAC} com/donhatchsw/util/FuzzyPointHashTable.prejava
	javarenumber -v 0 com/donhatchsw/util/FuzzyPointHashTable*.class
com/donhatchsw/util/MergeFind.class: com/donhatchsw/util/MergeFind.prejava
	javacpp ${JAVAC} com/donhatchsw/util/MergeFind.prejava
	javarenumber -v 0 com/donhatchsw/util/MergeFind*.class
com/donhatchsw/util/TopSorter.class: com/donhatchsw/util/TopSorter.prejava
	javacpp ${JAVAC} com/donhatchsw/util/TopSorter.prejava
	javarenumber -v 0 com/donhatchsw/util/TopSorter*.class

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


com/donhatchsw/applet/DoubleBufferedCanvas.class: com/donhatchsw/applet/DoubleBufferedCanvas.prejava
	javacpp ${JAVAC} com/donhatchsw/applet/DoubleBufferedCanvas.prejava
	javarenumber -v 0 com/donhatchsw/applet/DoubleBufferedCanvas*.class
com/donhatchsw/applet/AppletUtils.class: com/donhatchsw/applet/AppletUtils.prejava
	javacpp ${JAVAC} com/donhatchsw/applet/AppletUtils.prejava
	javarenumber -v 0 com/donhatchsw/applet/AppletUtils*.class
com/donhatchsw/applet/AppletViewer.class: com/donhatchsw/applet/AppletViewer.prejava
	javacpp ${JAVAC} com/donhatchsw/applet/AppletViewer.prejava
	javarenumber -v 0 com/donhatchsw/applet/AppletViewer*.class
com/donhatchsw/applet/CookieUtils.class: com/donhatchsw/applet/CookieUtils.prejava
	javacpp ${JAVAC} com/donhatchsw/applet/CookieUtils.prejava
	javarenumber -v 0 com/donhatchsw/applet/CookieUtils*.class
com/donhatchsw/applet/ExampleApplet.class: com/donhatchsw/applet/ExampleApplet.prejava
	javacpp ${JAVAC} com/donhatchsw/applet/ExampleApplet.prejava
	javarenumber -v 0 com/donhatchsw/applet/ExampleApplet*.class


com/donhatchsw/mc4d/GenericPuzzleDescription.class: com/donhatchsw/mc4d/GenericPuzzleDescription.java
	${JAVAC} com/donhatchsw/mc4d/GenericPuzzleDescription.java
com/donhatchsw/mc4d/PolytopePuzzleDescription.class: com/donhatchsw/mc4d/PolytopePuzzleDescription.java
	${JAVAC} com/donhatchsw/mc4d/PolytopePuzzleDescription.java
com/donhatchsw/mc4d/GenericPipelineUtils.class: com/donhatchsw/mc4d/GenericPipelineUtils.java
	${JAVAC} com/donhatchsw/mc4d/GenericPipelineUtils.java
com/donhatchsw/mc4d/MC4DModel.class: com/donhatchsw/mc4d/MC4DModel.java
	${JAVAC} com/donhatchsw/mc4d/MC4DModel.java
com/donhatchsw/mc4d/GenericGlue.class: com/donhatchsw/mc4d/GenericGlue.java
	${JAVAC} com/donhatchsw/mc4d/GenericGlue.java
com/donhatchsw/mc4d/MC4DViewGuts.class: com/donhatchsw/mc4d/MC4DViewGuts.java
	${JAVAC} com/donhatchsw/mc4d/MC4DViewGuts.java
com/donhatchsw/mc4d/MC4DViewApplet.class: com/donhatchsw/mc4d/MC4DViewApplet.java
	${JAVAC} com/donhatchsw/mc4d/MC4DViewApplet.java

com/donhatchsw/util/Arrows.class: com/donhatchsw/util/Arrows.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Arrows.prejava
	javarenumber -v 0 com/donhatchsw/util/Arrows*.class



donhatchsw.jar: Makefile META-INF/MANIFEST.MF com/donhatchsw/util/MyMath.class com/donhatchsw/util/Arrays.class com/donhatchsw/util/VecMath.class com/donhatchsw/util/LinearProgramming.class com/donhatchsw/compat/Double.class com/donhatchsw/compat/Float.class com/donhatchsw/compat/Boolean.class com/donhatchsw/compat/Misc.class com/donhatchsw/compat/regex.class com/donhatchsw/compat/Format.class com/donhatchsw/util/SortStuff.class com/donhatchsw/util/Minimizer.class com/donhatchsw/util/MyPanel.class com/donhatchsw/util/FuzzyPointHashTable.class com/donhatchsw/util/MergeFind.class com/donhatchsw/util/TopSorter.class com/donhatchsw/util/TriangulationOptimizer.class com/donhatchsw/util/Triangulator.class com/donhatchsw/util/Poly.class com/donhatchsw/util/CSG.class com/donhatchsw/util/PolyCSG.class com/donhatchsw/applet/DoubleBufferedCanvas.class com/donhatchsw/applet/AppletUtils.class com/donhatchsw/applet/AppletViewer.class com/donhatchsw/applet/CookieUtils.class com/donhatchsw/applet/ExampleApplet.class com/donhatchsw/mc4d/GenericPuzzleDescription.class com/donhatchsw/mc4d/PolytopePuzzleDescription.class com/donhatchsw/mc4d/GenericPipelineUtils.class com/donhatchsw/mc4d/MC4DModel.class com/donhatchsw/mc4d/GenericGlue.class com/donhatchsw/mc4d/MC4DViewGuts.class com/donhatchsw/mc4d/MC4DViewApplet.class com/donhatchsw/util/Arrows.class
	/bin/rm -rf scratch
	mkdir scratch
	cp -a Makefile RCS com scratch
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../donhatchsw.jar ../META-INF/MANIFEST.MF \
            com/donhatchsw/util/*.class \
            com/donhatchsw/util/*.prejava \
            com/donhatchsw/util/*.java \
            com/donhatchsw/util/macros.h \
            com/donhatchsw/util/RCS \
            com/donhatchsw/compat/*.class \
            com/donhatchsw/compat/*.prejava \
            com/donhatchsw/compat/*.java \
            com/donhatchsw/compat/macros.h \
            com/donhatchsw/compat/RCS \
            com/donhatchsw/applet/*.class \
            com/donhatchsw/applet/*.prejava \
            com/donhatchsw/applet/*.java \
            com/donhatchsw/applet/macros.h \
            com/donhatchsw/applet/RCS \
            com/donhatchsw/mc4d/*.class \
            com/donhatchsw/mc4d/*.java \
            com/donhatchsw/mc4d/RCS/* \
            Makefile RCS)

clean:
	# do NOT remove the .java files in mc4d!!!
	/bin/rm -rf \
            *.jar \
            com/donhatchsw/*/*.class \
            com/donhatchsw/*/*.html \
            scratch \
            com/donhatchsw/util/*.java \
            com/donhatchsw/util/*.java.lines \
            com/donhatchsw/compat/*.java \
            com/donhatchsw/compat/*.java.lines \
            com/donhatchsw/applet/*.java \
            com/donhatchsw/applet/*.java.lines \
            *.html \
            *.css

doc: donhatchsw.jar
	${JAVAROOT}/bin/javadoc com/donhatchsw/*/*.java
send: doc
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
	(cd ..; scp -r donhatchsw hatch@plunk.org:public_html/private/.)
senddoc: doc
	scp -r *.html *.css resources com hatch@plunk.org:public_html/donhatchsw/javadoc/.
