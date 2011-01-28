#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#


# Sticking points:
#    - weak reference doesn't exist in 1.1... is it of any use to me then??
#    - something's going funny with jikes and renumbering, it's looking for .prejava.lines which is wrong

# To use cookies, need netscape.javascript.JSObject
# which is in /usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar
# That's needed at runtime but not compile time (since
# we use reflection to get at the API).


##JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAC=${JAVAROOT}/bin/javac -target 1.1

##JAVAROOT=/usr/java/j2sdk1.4.2
##JAVAROOT=/usr/java/jdk1.5.0
##JAVAROOT=/usr/java/jdk1.5.0_01
#JAVAROOT=/usr/java/jdk1.6.0
#JAVAC=${JAVAROOT}/bin/javac -source 1.2 -target 1.1 -g
##JAVAC=${JAVAROOT}/bin/javac -source 1.4 -target 1.4 -g

##JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
##JAVAROOT=/usr/java/j2sdk1.4.2
## there is no -source 1.2 or -source 1.1 for jikes
#JAVAC=jikes +P -source 1.3 -target 1.1 -classpath ${JAVAROOT}/jre/lib/rt.jar


# cygwin on my laptop
# with fake renumbering (so it's not quite fair):
#     javac1.2: 1:08
#     javac1.6: 1:42
#     jikes with 1.2: 53 seconds
#JAVAC=javac1.6
#JAVAROOT=c:/jdk1.3.1_20
JAVAC=javac1.2
#JAVAROOT=c:/jdk1.2.2
# there is no -source 1.2 or -source 1.1 for jikes
#JAVAC=jikes +P -source 1.3 -target 1.1 -classpath ${JAVAROOT}/jre/lib/rt.jar
# hmm, if I do it that way, with jikes 1.22, and run it using java1.2, I get a "monitor is in illegal state" error in the jikes-compiled code... specifically, on exiting from any synchronized(someObject) {...} block.  So it seems I have to use javac1.2 instead of jikes.


# Pattern rule for making a .class file out of a .prejava file
%.class : %.prejava
	rm -f $*[.$$]class
	javacpp ${JAVAC} $<
	javarenumber -v 0 $*[.$$]class

# Pattern rule for making a .class file out of a .java file
%.class : %.java
	${JAVAC} $<


# Arrows currently must come before UndoTreeViewer but I should get rid of this dependency, it's for the justified labels
# SmoothlyVaryingViewingParameter demo depends on MyGraphics
# And UndoTreeViewer depends on SmoothlyVaryingViewingParameter

# default target
donhatchsw.jar: \
    Makefile \
    META-INF/MANIFEST.MF \
    com/donhatchsw/javacpp/ExpressionParser.class \
    com/donhatchsw/javacpp/Cpp.class \
    com/donhatchsw/javacpp/javacpp.class \
    com/donhatchsw/compat/ArrayList.class \
    com/donhatchsw/compat/IntArrayList.class \
    com/donhatchsw/compat/DoubleArrayList.class \
    com/donhatchsw/util/MyMath.class \
    com/donhatchsw/util/Arrays.class \
    com/donhatchsw/util/VecMath.class \
    com/donhatchsw/util/LinearProgramming.class \
    com/donhatchsw/compat/regex.class \
    com/donhatchsw/compat/Format.class \
    com/donhatchsw/util/SortStuff.class \
    com/donhatchsw/util/Minimizer.class \
    com/donhatchsw/util/FuzzyPointHashTable.class \
    com/donhatchsw/util/MergeFind.class \
    com/donhatchsw/util/IndexBinaryHeap.class \
    com/donhatchsw/util/TopSorter.class \
    com/donhatchsw/util/ConvexHull.class \
    com/donhatchsw/util/Listenable.class \
    com/donhatchsw/util/Arrows.class \
    com/donhatchsw/util/TriangulationOptimizer.class \
    com/donhatchsw/util/Triangulator.class \
    com/donhatchsw/util/Poly.class \
    com/donhatchsw/util/CSG.class \
    com/donhatchsw/util/PolyCSG.class \
    com/donhatchsw/awt/MyGraphics.class \
    com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.class \
    com/donhatchsw/awt/TableLayout.class \
    com/donhatchsw/awt/RowLayout.class \
    com/donhatchsw/awt/ColLayout.class \
    com/donhatchsw/awt/TablePanel.class \
    com/donhatchsw/awt/Row.class \
    com/donhatchsw/awt/Col.class \
    com/donhatchsw/awt/LayoutExample.class \
    com/donhatchsw/awt/MainWindowCount.class \
    com/donhatchsw/util/SmoothlyVaryingViewingParameter.class \
    com/donhatchsw/util/UndoTreeSquirrel.class \
    com/donhatchsw/util/UndoTreeViewer.class \
    com/donhatchsw/applet/DoubleBufferedCanvas.class \
    com/donhatchsw/applet/AppletUtils.class \
    com/donhatchsw/applet/AppletViewer.class \
    com/donhatchsw/applet/CookieUtils.class \
    com/donhatchsw/applet/ExampleApplet.class \
    com/donhatchsw/mc4d/GenericPuzzleDescription.class \
    com/donhatchsw/mc4d/GenericPuzzleFactory.class \
    com/donhatchsw/mc4d/PolytopePuzzleDescription.class \
    com/donhatchsw/mc4d/GenericPipelineUtils.class \
    com/donhatchsw/mc4d/MC4DModel.class \
    com/donhatchsw/mc4d/GenericGlue.class \
    com/donhatchsw/mc4d/MC4DViewGuts.class \
    com/donhatchsw/mc4d/MC4DControlPanel.class \
    com/donhatchsw/mc4d/MC4DApplet.class \
    ${NULL}
	/bin/rm -rf scratch
	mkdir scratch
	cp -a Makefile RCS com scratch
	(cd scratch; ${JAVAROOT}/bin/jar -cfm ../donhatchsw.jar ../META-INF/MANIFEST.MF \
            com/donhatchsw/javacpp/*.class \
            com/donhatchsw/javacpp/*.java \
            com/donhatchsw/javacpp/RCS/* \
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
	# do NOT remove the .java files in javacpp or mc4d !!!
        # ASSUMPTION: everything named .html or .css is automatically generated
	/bin/rm -rf \
            *.jar \
            com/donhatchsw/*/*.class \
            com/donhatchsw/*/*.html \
            scratch \
            com/donhatchsw/util/*.java{,.lines} \
            com/donhatchsw/compat/*.java{,.lines} \
            com/donhatchsw/applet/*.java{,.lines} \
            com/donhatchsw/awt/*.java{,.lines} \
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
sendMinimal: donhatchsw.jar
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
send: doc
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
	(cd ..; scp -r donhatchsw hatch@plunk.org:public_html/private/.)
senddoc: doc
	scp -r *.html *.css resources com hatch@plunk.org:public_html/donhatchsw/.
