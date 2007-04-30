#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#


# Sticking points:
#    - Arrows uses javax.swing.Icon or something, doesn't exist in 1.2
#    - weak pointer doesn't exist in 1.1... is it of any use to me then?
#    - something's going funny with jikes and renumbering, it's looking for .prejava.lines which is wrong

# To use cookies, need netscape.javascript.JSObject
# which is in /usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar
# That's needed at runtime but not compile time (since
# we use reflection to get at the API).


##JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAC=${JAVAROOT}/bin/javac -target 1.1

#JAVAROOT=/usr/java/j2sdk1.4.2
##JAVAROOT=/usr/java/jdk1.5.0
##JAVAROOT=/usr/java/jdk1.5.0_01
#JAVAC=${JAVAROOT}/bin/javac -source 1.2 -target 1.1 -g
##JAVAC=${JAVAROOT}/bin/javac -source 1.4 -target 1.4 -g

#JAVAROOT=/usr/java/jdk1.2.2
JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAROOT=/usr/java/j2sdk1.4.2
# there is no -source 1.2 or -source 1.1 for jikes
JAVAC=jikes +P -source 1.3 -target 1.1 -classpath ${JAVAROOT}/jre/lib/rt.jar

donhatchsw.jar:

# XXX Poly is a prefix of PolyCSG, so the * thing is not robust.
# XXX also UndoTree and UndoTreeViewer and UndoTreeSquirrel

com/donhatchsw/compat/ArrayList.class: com/donhatchsw/compat/ArrayList.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/ArrayList.prejava
	javarenumber -v 0 com/donhatchsw/compat/ArrayList*.class
com/donhatchsw/compat/IntArrayList.class: com/donhatchsw/compat/IntArrayList.prejava
	javacpp ${JAVAC} com/donhatchsw/compat/IntArrayList.prejava
	javarenumber -v 0 com/donhatchsw/compat/IntArrayList*.class

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
com/donhatchsw/util/FuzzyPointHashTable.class: com/donhatchsw/util/FuzzyPointHashTable.prejava
	javacpp ${JAVAC} com/donhatchsw/util/FuzzyPointHashTable.prejava
	javarenumber -v 0 com/donhatchsw/util/FuzzyPointHashTable*.class
com/donhatchsw/util/MergeFind.class: com/donhatchsw/util/MergeFind.prejava
	javacpp ${JAVAC} com/donhatchsw/util/MergeFind.prejava
	javarenumber -v 0 com/donhatchsw/util/MergeFind*.class
com/donhatchsw/util/TopSorter.class: com/donhatchsw/util/TopSorter.prejava
	javacpp ${JAVAC} com/donhatchsw/util/TopSorter.prejava
	javarenumber -v 0 com/donhatchsw/util/TopSorter*.class
com/donhatchsw/util/Listenable.class: com/donhatchsw/util/Listenable.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Listenable.prejava
	javarenumber -v 0 com/donhatchsw/util/Listenable*.class



# XXX currently must come before UndoTreeViewer but I should get rid of this dependency, it's for the justified labels
com/donhatchsw/util/Arrows.class: com/donhatchsw/util/Arrows.prejava
	javacpp ${JAVAC} com/donhatchsw/util/Arrows.prejava
	javarenumber -v 0 com/donhatchsw/util/Arrows*.class

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

com/donhatchsw/awt/OldMyPanel.class: com/donhatchsw/awt/OldMyPanel.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/OldMyPanel.prejava
	javarenumber -v 0 com/donhatchsw/awt/OldMyPanel*.class
com/donhatchsw/awt/OldRow.class: com/donhatchsw/awt/OldRow.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/OldRow.prejava
	javarenumber -v 0 com/donhatchsw/awt/OldRow*.class
com/donhatchsw/awt/OldCol.class: com/donhatchsw/awt/OldCol.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/OldCol.prejava
	javarenumber -v 0 com/donhatchsw/awt/OldCol*.class
com/donhatchsw/awt/OldMyPanelExample.class: com/donhatchsw/awt/OldMyPanelExample.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/OldMyPanelExample.prejava
	javarenumber -v 0 com/donhatchsw/awt/OldMyPanelExample*.class
com/donhatchsw/awt/MyGraphics.class: com/donhatchsw/awt/MyGraphics.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/MyGraphics.prejava
	javarenumber -v 0 com/donhatchsw/awt/MyGraphics*.class
com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.class: com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.prejava
	javarenumber -v 0 com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful*.class
com/donhatchsw/awt/TableLayout.class: com/donhatchsw/awt/TableLayout.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/TableLayout.prejava
	javarenumber -v 0 com/donhatchsw/awt/TableLayout*.class
com/donhatchsw/awt/RowLayout.class: com/donhatchsw/awt/RowLayout.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/RowLayout.prejava
	javarenumber -v 0 com/donhatchsw/awt/RowLayout*.class
com/donhatchsw/awt/ColLayout.class: com/donhatchsw/awt/ColLayout.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/ColLayout.prejava
	javarenumber -v 0 com/donhatchsw/awt/ColLayout*.class
com/donhatchsw/awt/TablePanel.class: com/donhatchsw/awt/TablePanel.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/TablePanel.prejava
	javarenumber -v 0 com/donhatchsw/awt/TablePanel*.class
com/donhatchsw/awt/NewRow.class: com/donhatchsw/awt/NewRow.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/NewRow.prejava
	javarenumber -v 0 com/donhatchsw/awt/NewRow*.class
com/donhatchsw/awt/NewCol.class: com/donhatchsw/awt/NewCol.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/NewCol.prejava
	javarenumber -v 0 com/donhatchsw/awt/NewCol*.class
com/donhatchsw/awt/LayoutExample.class: com/donhatchsw/awt/LayoutExample.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/LayoutExample.prejava
	javarenumber -v 0 com/donhatchsw/awt/LayoutExample*.class
com/donhatchsw/awt/MainWindowCount.class: com/donhatchsw/awt/MainWindowCount.prejava
	javacpp ${JAVAC} com/donhatchsw/awt/MainWindowCount.prejava
	javarenumber -v 0 com/donhatchsw/awt/MainWindowCount*.class

# SmoothlyVaryingViewingParameter demo depends on MyGraphics
com/donhatchsw/util/SmoothlyVaryingViewingParameter.class: com/donhatchsw/util/SmoothlyVaryingViewingParameter.prejava
	javacpp ${JAVAC} com/donhatchsw/util/SmoothlyVaryingViewingParameter.prejava
	javarenumber -v 0 com/donhatchsw/util/SmoothlyVaryingViewingParameter*.class
# And UndoTreeViewer depends on SmoothlyVaryingViewingParameter
com/donhatchsw/util/UndoTreeSquirrel.class: com/donhatchsw/util/UndoTreeSquirrel.prejava
	javacpp ${JAVAC} com/donhatchsw/util/UndoTreeSquirrel.prejava
	javarenumber -v 0 com/donhatchsw/util/UndoTreeSquirrel*.class
com/donhatchsw/util/UndoTreeViewer.class: com/donhatchsw/util/UndoTreeViewer.prejava
	javacpp ${JAVAC} com/donhatchsw/util/UndoTreeViewer.prejava
	javarenumber -v 0 com/donhatchsw/util/UndoTreeViewer*.class


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
com/donhatchsw/mc4d/GenericPuzzleFactory.class: com/donhatchsw/mc4d/GenericPuzzleFactory.java
	${JAVAC} com/donhatchsw/mc4d/GenericPuzzleFactory.java
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
com/donhatchsw/mc4d/MC4DControlPanel.class: com/donhatchsw/mc4d/MC4DControlPanel.java
	${JAVAC} com/donhatchsw/mc4d/MC4DControlPanel.java
com/donhatchsw/mc4d/MC4DApplet.class: com/donhatchsw/mc4d/MC4DApplet.java
	${JAVAC} com/donhatchsw/mc4d/MC4DApplet.java



donhatchsw.jar: Makefile META-INF/MANIFEST.MF com/donhatchsw/compat/ArrayList.class com/donhatchsw/compat/IntArrayList.class com/donhatchsw/util/MyMath.class com/donhatchsw/util/Arrays.class com/donhatchsw/util/VecMath.class com/donhatchsw/util/LinearProgramming.class com/donhatchsw/compat/regex.class com/donhatchsw/compat/Format.class com/donhatchsw/util/SortStuff.class com/donhatchsw/util/Minimizer.class com/donhatchsw/util/FuzzyPointHashTable.class com/donhatchsw/util/MergeFind.class com/donhatchsw/util/TopSorter.class com/donhatchsw/util/Listenable.class com/donhatchsw/util/Arrows.class com/donhatchsw/util/TriangulationOptimizer.class com/donhatchsw/util/Triangulator.class com/donhatchsw/util/Poly.class com/donhatchsw/util/CSG.class com/donhatchsw/util/PolyCSG.class com/donhatchsw/awt/OldMyPanel.class com/donhatchsw/awt/OldRow.class com/donhatchsw/awt/OldCol.class com/donhatchsw/awt/OldMyPanelExample.class com/donhatchsw/awt/MyGraphics.class com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.class com/donhatchsw/awt/TableLayout.class com/donhatchsw/awt/RowLayout.class com/donhatchsw/awt/ColLayout.class com/donhatchsw/awt/TablePanel.class com/donhatchsw/awt/NewRow.class com/donhatchsw/awt/NewCol.class com/donhatchsw/awt/LayoutExample.class com/donhatchsw/awt/MainWindowCount.class com/donhatchsw/util/SmoothlyVaryingViewingParameter.class com/donhatchsw/util/UndoTreeSquirrel.class com/donhatchsw/util/UndoTreeViewer.class com/donhatchsw/applet/DoubleBufferedCanvas.class com/donhatchsw/applet/AppletUtils.class com/donhatchsw/applet/AppletViewer.class com/donhatchsw/applet/CookieUtils.class com/donhatchsw/applet/ExampleApplet.class com/donhatchsw/mc4d/GenericPuzzleDescription.class com/donhatchsw/mc4d/GenericPuzzleFactory.class com/donhatchsw/mc4d/PolytopePuzzleDescription.class com/donhatchsw/mc4d/GenericPipelineUtils.class com/donhatchsw/mc4d/MC4DModel.class com/donhatchsw/mc4d/GenericGlue.class com/donhatchsw/mc4d/MC4DViewGuts.class com/donhatchsw/mc4d/MC4DControlPanel.class com/donhatchsw/mc4d/MC4DApplet.class
	/bin/rm -rf scratch
	mkdir scratch
	cp -a Makefile RCS com scratch
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../donhatchsw.jar ../META-INF/MANIFEST.MF \
            com/donhatchsw/util/*.class \
            com/donhatchsw/util/*.prejava \
            com/donhatchsw/util/*.java \
            com/donhatchsw/util/macros.h \
            com/donhatchsw/util/RCS \
            com/donhatchsw/awt/*.class \
            com/donhatchsw/awt/*.prejava \
            com/donhatchsw/awt/*.java \
            com/donhatchsw/awt/macros.h \
            com/donhatchsw/awt/RCS \
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
            com/donhatchsw/awt/*.java \
            com/donhatchsw/awt/*.java.lines \
            *.html \
            *.css

doc: donhatchsw.jar
	# Copy .prehtml files to .html files
	# (they are in .prehtml files so that make clean won't remove them)
	# XXX should check for writability of the .html file before clobbering! only clobber a read-only one!
	for f in `find . -name package.prehtml -print`; do \
	    echo "    found $$f"; \
	    g=`echo $$f | sed s/.prehtml/.html/`; \
	    echo "    converting to $$g"; \
            rm -f $$g; \
	    (tcsh -f -c 'repeat 100 echo "<!-- THIS FILE IS AUTOMATICALLY GENERATED-- DO NOT EDIT -->"'; cat $$f) > $$g; \
	    chmod a-w $$g; \
	done

	${JAVAROOT}/bin/javadoc com/donhatchsw/*/*.java
send: doc
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
	(cd ..; scp -r donhatchsw hatch@plunk.org:public_html/private/.)
senddoc: doc
	scp -r *.html *.css resources com hatch@plunk.org:public_html/donhatchsw/javadoc/.
