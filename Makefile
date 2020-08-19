#
# Totally hacky special purpose Makefile that probably won't work
# for you.
#


# Sticking points:
#    - weak reference doesn't exist in 1.1... is it of any use to me then??
#    - something's going funny with jikes and renumbering, it's looking for .prejava.lines which is wrong

# TODO: versions should be command line params I think
# TODO: find automatic way to find all failures to declare @Override

# To find all places where I forgot to mark something public or private or protected:
#    javap `find . -name '*.prejava' | sed 's/.prejava//' | sed 's/^\.\///'` | grep -v "\bpublic " | grep -v '\bprotected ' | fgrep -v ' access$' | grep -v ' static {};' | grep -v '^}$' | fgrep -v ' class$' | fgrep -v ' array$'

# To use cookies, need netscape.javascript.JSObject
# which is in /usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar
# That's needed at runtime but not compile time (since
# we use reflection to get at the API).

# Uncomment one of the following sections.
# To find rt.jar: locate -r '/rt.jar$'

##JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
#JAVAC=${JAVAROOT}/bin/javac -target 1.1
#JAR=${JAVAROOT}/bin/jar
#JAVADOC=${JAVADOCROOT}/bin/javadoc

##JAVAROOT=/usr/java/j2sdk1.4.2
##JAVAROOT=/usr/java/jdk1.5.0
##JAVAROOT=/usr/java/jdk1.5.0_01
#JAVAROOT=/usr/java/jdk1.6.0
#JAVAC=${JAVAROOT}/bin/javac -source 1.2 -target 1.1 -g
##JAVAC=${JAVAROOT}/bin/javac -source 1.4 -target 1.4 -g
#JAR=${JAVAROOT}/bin/jar
#JAVADOC=${JAVADOCROOT}/bin/javadoc

##JAVAROOT=/usr/java/jdk1.2.2
#JAVAROOT=/usr/java/jdk1.3.1_18
##JAVAROOT=/usr/java/j2sdk1.4.2
## there is no -source 1.2 or -source 1.1 for jikes
#JAVAC=jikes +P -source 1.3 -target 1.1 -classpath ${JAVAROOT}/jre/lib/rt.jar
#JAR=${JAVAROOT}/bin/jar
#JAVADOC=${JAVADOCROOT}/bin/javadoc


# cygwin on my laptop
# with fake renumbering (so it's not quite fair):
#     javac1.2: 1:08
#     javac1.6: 1:42
#     jikes with 1.2: 53 seconds
##JAVAC=javac1.6
##JAVAROOT=c:/jdk1.3.1_20
#JAVAC=javac1.2
#JAVAROOT=c:/jdk1.2.2
## use 1.6 javadoc, since it produces nicer package summaries from the package.html files
#JAVADOCROOT="c:/Program Files (x86)/Java/jdk1.6.0_17"
## there is no -source 1.2 or -source 1.1 for jikes
##JAVAC=jikes +P -source 1.3 -target 1.1 -classpath ${JAVAROOT}/jre/lib/rt.jar
## hmm, if I do it that way, with jikes 1.22, and run it using java1.2, I get a "monitor is in illegal state" error in the jikes-compiled code... specifically, on exiting from any synchronized(someObject) {...} block.  So it seems I have to use javac1.2 instead of jikes.
#JAR=${JAVAROOT}/bin/jar
#JAVADOC=${JAVADOCROOT}/bin/javadoc


# XXX TODO: deal with warning "warning: [options] bootstrap class path not set in conjunction with -source 1.2" (or whatever). what does it mean?

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.2, which allows any -target >= 1.1. As backwards-compatible as possible; use this for releases.
#JAVACPPFLAGS=-DOVERRIDE=
#JAVAC=javac -source 1.2 -target 1.1
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.3, which allows any -target >= 1.1
#JAVACPPFLAGS=-DOVERRIDE=
#JAVAC=javac -source 1.3 -target 1.1
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.4 which is last version in which @Override doesn't exist, and requires -target >=1.4
#JAVACPPFLAGS=-DOVERRIDE=
#JAVAC=javac -source 1.4 -target 1.4
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.5 which is first version in which @Override exists, and requires -target >= 1.5
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.5 -target 1.5
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.6 which requires -target >= 1.6
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.6 -target 1.6
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.7 which requires -target >= 1.7
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.7 -target 1.7 -bootclasspath /usr/local/buildtools/java/jdk7-google-v6-64/jre/lib/rt.jar
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.7 which requires -target >= 1.7
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.7 -target 1.7 -bootclasspath /usr/local/buildtools/java/jdk7-google-v6-64/jre/lib/rt.jar -deprecation
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.8 which requires -target >= 1.8
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.8 -target 1.8 -bootclasspath /usr/local/buildtools/java/jdk8-google-v7-64/jre/lib/rt.jar
#JAR=jar
#JAVADOC=javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling for 1.8 which requires -target >= 1.8
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac -source 1.8 -target 1.8 -bootclasspath /usr/local/buildtools/java/jdk8-google-v7-64/jre/lib/rt.jar -deprecation
#JAR=jar
#JAVADOC=javadoc

# ubuntu, /usr/bin/javac seems to have more recent java versions than what's in my PATH.  need to run using /usr/bin/java, though.
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=/usr/bin/javac -source 1.10 -target 1.10 -deprecation
#JAR=/usr/bin/jar
#JAVADOC=/usr/bin/javadoc

# ubuntu, oldest at the moment.  -Xlint:-options suppresses warning about old version (and also about bootpath. argh! I DO want to see that!)
JAVACPPFLAGS=-DOVERRIDE=@Override
JAVAC=/usr/bin/javac -source 6 -target 6 -deprecation -Xlint:all -Xlint:-options
JAR=/usr/bin/jar
JAVADOC=/usr/bin/javadoc

# ubuntu, newest at the moment
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=/usr/bin/javac -source 11 -target 11 -deprecation -Xlint:all
#JAR=/usr/bin/jar
#JAVADOC=/usr/bin/javadoc

# ubuntu or macbook, java 1.7 or 1.8, compiling as modern as possible, whatever that means
#JAVACPPFLAGS=-DOVERRIDE=@Override
#JAVAC=javac
#JAR=jar
#JAVADOC=javadoc


# Pattern rule for making a .class file out of a .prejava file.
# Note: for the class files associated with Foo.prejava,
# what we really want is the regular expression Foo($.*)?\.class,
# but I don't think there's any way to express that using a glob pattern,
# so we use a slightly more general glob pattern which will also remove
# some non-.class file names such as Foo.barclass, Foo$class, and Foo$barclass
# (all of which are unlikely to be legitimate precious files,
# so this seems pretty safe).
%.class : %.prejava
	rm -f $*[.$$]*class
	./javacpp ${JAVACPPFLAGS} ${JAVAC} $<
	./javarenumber -v 0 $*[.$$]*class

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
    com/donhatchsw/util/SortStuff.class \
    com/donhatchsw/util/MyMath.class \
    com/donhatchsw/util/Complex.class \
    com/donhatchsw/util/Arrays.class \
    com/donhatchsw/util/VecMath.class \
    com/donhatchsw/util/LinearProgramming.class \
    com/donhatchsw/compat/regex.class \
    com/donhatchsw/compat/Format.class \
    com/donhatchsw/util/SpecializedHashMap.class \
    com/donhatchsw/util/Minimizer.class \
    com/donhatchsw/util/FuzzyPointHashTable.class \
    com/donhatchsw/util/FuzzyPointHashSet.class \
    com/donhatchsw/util/MergeFind.class \
    com/donhatchsw/util/IndexBinaryHeap.class \
    com/donhatchsw/util/IndexBinaryHeapKeyed.class \
    com/donhatchsw/util/TopSorter.class \
    com/donhatchsw/util/ConvexHull.class \
    com/donhatchsw/util/Listenable.class \
    com/donhatchsw/util/Arrows.class \
    com/donhatchsw/util/TriangulationOptimizer.class \
    com/donhatchsw/util/Triangulator.class \
    com/donhatchsw/util/Poly.class \
    com/donhatchsw/util/CSG.class \
    com/donhatchsw/util/PolyCSG.class \
    com/donhatchsw/util/MiniBall.class \
    com/donhatchsw/util/BlueNoise.class \
    com/donhatchsw/awt/MyGraphics.class \
    com/donhatchsw/awt/GridBagLayoutInWhichRELATIVEMeansSomethingUseful.class \
    com/donhatchsw/awt/TableLayout.class \
    com/donhatchsw/awt/RowLayout.class \
    com/donhatchsw/awt/ColLayout.class \
    com/donhatchsw/awt/TablePanel.class \
    com/donhatchsw/awt/Row.class \
    com/donhatchsw/awt/Col.class \
    com/donhatchsw/awt/JTablePanel.class \
    com/donhatchsw/awt/JRow.class \
    com/donhatchsw/awt/JCol.class \
    com/donhatchsw/awt/LayoutExample.class \
    com/donhatchsw/awt/MainWindowCount.class \
    com/donhatchsw/util/NewtonSolver.class \
    com/donhatchsw/util/GoldenSectionSearch.class \
    com/donhatchsw/util/Catenary.class \
    com/donhatchsw/util/CatenaryRotated.class \
    com/donhatchsw/util/SmoothlyVaryingViewingParameter.class \
    com/donhatchsw/util/UndoTreeSquirrel.class \
    com/donhatchsw/util/UndoTreeViewer.class \
    com/donhatchsw/applet/DoubleBufferedCanvas.class \
    com/donhatchsw/applet/AppletUtils.class \
    com/donhatchsw/applet/CookieUtils.class \
    com/donhatchsw/shims_for_deprecated/java_applet_AppletContext.class \
    com/donhatchsw/shims_for_deprecated/java_applet_AppletStub.class \
    com/donhatchsw/shims_for_deprecated/java_applet_Applet.class \
    com/donhatchsw/shims_for_deprecated/javax_swing_JApplet.class \
    com/donhatchsw/shims_for_deprecated/com_donhatchsw_applet_AppletViewer.class \
    com/donhatchsw/shims_for_deprecated/com_donhatchsw_applet_ExampleApplet.class \
    com/donhatchsw/mc4d/GenericPuzzleDescription.class \
    com/donhatchsw/mc4d/GenericPuzzleFactory.class \
    com/donhatchsw/mc4d/PolytopePuzzleDescription.class \
    com/donhatchsw/mc4d/VeryCleverPaintersSortingOfStickers.class \
    com/donhatchsw/mc4d/GenericPipelineUtils.class \
    com/donhatchsw/mc4d/MC4DModel.class \
    com/donhatchsw/mc4d/GenericGlue.class \
    com/donhatchsw/mc4d/MC4DViewGuts.class \
    com/donhatchsw/mc4d/MC4DControlPanelInterface.class \
    com/donhatchsw/mc4d/MC4DLegacyControlPanel.class \
    com/donhatchsw/mc4d/MC4DSwingControlPanel.class \
    com/donhatchsw/mc4d/MC4DApplet.class \
    com/donhatchsw/mc4d/MC4DJApplet.class \
    ${NULL}
	/bin/rm -rf scratch
	mkdir scratch
	cp -a Makefile com scratch
	(cd scratch; ${JAR} -cfm ../donhatchsw.jar ../META-INF/MANIFEST.MF \
            com/donhatchsw/javacpp/*.class \
            com/donhatchsw/javacpp/*.java \
            com/donhatchsw/shims_for_deprecated/*.class \
            com/donhatchsw/shims_for_deprecated/*.prejava \
            com/donhatchsw/shims_for_deprecated/*.java \
            com/donhatchsw/shims_for_deprecated/macros.h \
            com/donhatchsw/util/*.class \
            com/donhatchsw/util/*.prejava \
            com/donhatchsw/util/*.java \
            com/donhatchsw/util/macros.h \
            com/donhatchsw/awt/*.class \
            com/donhatchsw/awt/*.prejava \
            com/donhatchsw/awt/*.java \
            com/donhatchsw/awt/macros.h \
            com/donhatchsw/compat/*.class \
            com/donhatchsw/compat/*.prejava \
            com/donhatchsw/compat/*.java \
            com/donhatchsw/compat/macros.h \
            com/donhatchsw/applet/*.class \
            com/donhatchsw/applet/*.prejava \
            com/donhatchsw/applet/*.java \
            com/donhatchsw/applet/macros.h \
            com/donhatchsw/mc4d/*.class \
            com/donhatchsw/mc4d/*.java \
            Makefile)
	/bin/rm -rf scratch

clean:
	# do NOT remove the .java files in javacpp or mc4d !!!
        # ASSUMPTION: everything named .html or .css is automatically generated.
        # If you have a real .html file, put it into a .prehtml file to get around this.
	/bin/rm -rf \
            *.jar \
            com/donhatchsw/*/*.class \
            com/donhatchsw/*/*.html \
            scratch \
            com/donhatchsw/util/*.java{,.lines} \
            com/donhatchsw/compat/*.java{,.lines} \
            com/donhatchsw/applet/*.java{,.lines} \
            com/donhatchsw/awt/*.java{,.lines} \
            com/donhatchsw/shims_for_deprecated/*.java{,.lines} \
            *.html \
            *.css \
            resources/ \
            package-list \

doc: donhatchsw.jar
	# Copy .prehtml files to .html files
	# (they are in .prehtml files just so that make clean won't remove them)
	# XXX should check for writability of the .html file before clobbering! only clobber a read-only one!
	for f in `find . -name package.prehtml -print`; do \
	    echo "    found $$f"; \
	    g=`echo $$f | sed s/.prehtml/.html/`; \
	    echo "    converting to $$g"; \
            rm -f $$g; \
	    (tcsh -f -c 'repeat 100 echo "<!-- THIS FILE IS AUTOMATICALLY GENERATED-- DO NOT EDIT -->"'; cat $$f) > $$g; \
	    chmod a-w $$g; \
	done

	${JAVADOC} com/donhatchsw/*/*.java
sendMinimal: donhatchsw.jar
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
send: doc
	scp donhatchsw.jar hatch@plunk.org:public_html/donhatchsw/.
	(cd ..; scp -r donhatchsw hatch@plunk.org:public_html/private/.)
senddoc: doc
	scp -r *.html *.css package-list resources com hatch@plunk.org:public_html/donhatchsw/.




# XXX temporary? for expedience, so these get recompiled properly when I change macros.h since I'm working on them.  Would be good if we could do something more genuine.
# Note that we don't make anything depend on Makefile.  This is arguably
# wrong, but a dependency on Makefile would cause a lot of pain.
com/donhatchsw/util/SmoothlyVaryingViewingParameter.class: com/donhatchsw/util/macros.h
com/donhatchsw/util/MyMath.class: com/donhatchsw/util/macros.h
com/donhatchsw/util/Catenary.class: com/donhatchsw/util/macros.h
com/donhatchsw/util/CatenaryRotated.class: com/donhatchsw/util/macros.h

