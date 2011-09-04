#!/usr/bin/perl -w
use strict;

sub System(@)
{
    print("@_\n");
    # use tcsh -f -c instead of sending it straight, so that >! will be understood
    system("tcsh -f -c '@_'") and warn("*** HEY! exit status (or something) was $? ***\n");
}


my $javacppCommandBase = "java -cp donhatchsw.jar com.donhatchsw.javacpp.Cpp -x c++";
my $cppCommandBase =     "                               /usr/bin/cpp -C -x c++";


my $inStart = "/usr/include/math.h"; # can be file or directory
#my $inStart = "/usr/include/c++/3.4.3"; # can be file or directory
-e $inStart or die "$inStart: $!\n";
my $inStartDir = $inStart;
if (! -d $inStartDir)
{
    $inStartDir =~ s/\/[^\/]+$//;
    print "inStartDir = $inStartDir\n";
    -d $inStartDir or die;
}
my $outVolatileBaseDir = "tmp/COMPARE";
$inStartDir =~ /^\// or die;
my $outStartDir = "$outVolatileBaseDir$inStartDir";

if (1)
{
    print("Clearing directories...\n");
    System("/bin/rm -rf $outVolatileBaseDir; mkdir -p $outStartDir");

    if (-d $inStart)
    {
        print("Making subdirectories...\n");
        for my $dir (split(/\n/, `(cd $inStartDir; find . -type d)`))
        {
            $dir ne "." or next;
            $dir =~ s/^\.\/// or die; # remove initial "./"
            print("    making dir '$outStartDir/$dir'\n");
            mkdir("$outStartDir/$dir");
        }
        print("done.\n");
    }
}

if (1)
{
    my $files;
    if (-d $inStart)
    {
        $files = `(cd $inStartDir; find . -type f)`;
    }
    else
    {
        $files = $inStart;
        $files =~ s/.*\/// or die;
        $inStart eq "$inStartDir/$files" or die;
    }

    my @files = split(/\n/, $files);
    my $nFiles = @files;
    print("$nFiles files\n");
    for my $file (@files)
    {
        $file =~ s/^\.\///;

        System("    $cppCommandBase $inStartDir/$file >! $outStartDir/$file.0THEIRS");
        System("$javacppCommandBase $inStartDir/$file >! $outStartDir/$file.1MINE");

        System("    $cppCommandBase < $inStartDir/$file >! $outStartDir/$file.FROMSTDIN.0THEIRS");
        System("$javacppCommandBase < $inStartDir/$file >! $outStartDir/$file.FROMSTDIN.1MINE");


        System("    $cppCommandBase $outStartDir/$file.0THEIRS >! $outStartDir/$file.0THEIRS.0THEIRS");
        System("$javacppCommandBase $outStartDir/$file.0THEIRS >! $outStartDir/$file.0THEIRS.1MINE");
        System("    $cppCommandBase $outStartDir/$file.1MINE >! $outStartDir/$file.1MINE.0THEIRS");
        System("$javacppCommandBase $outStartDir/$file.1MINE >! $outStartDir/$file.1MINE.1MINE");
    }
}
