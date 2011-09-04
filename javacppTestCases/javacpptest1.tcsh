#!/bin/tcsh -f

foreach f ( `find . -name \*.prejava` )
    echo ====== $f
    echo javacpp $f
    javacpp $f
    echo "java com/donhatchsw/javacpp/Cpp $f > $f:r.java.MINE"
    java com/donhatchsw/javacpp/Cpp $f > $f:r.java.MINE
end
