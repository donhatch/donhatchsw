package com.donhatchsw.javacpp;

public class javacpp
{
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    public static void main(String args[])
    {
        Cpp1.ParsedCommandLineArgs parsedArgs = Cpp1.parseCommandLineArgs(args);

        if (parsedArgs.inFileNames.length == 0)
        {
            System.err.println("Usage:");
            System.err.println("    javacpp <<cpp options> <file>.prejava [<file1>.prejava ...]");
            System.err.println("    javacpp <cpp options> javac <javac options> <file>.prejava [<file1>.prejava ...]");
        }
        for (int i = 0; i < parsedArgs.inFileNames.length; ++i)
        {
            String inFileName = parsedArgs.inFileNames[i];
            if (!inFileName.endsWith(".prejava"))
            {
                System.err.println("Error: the input file \""+inFileName+"\" does not have the \".prejava\" extension.");
                System.exit(1);
            }
        }

        for (int i = 0; i < parsedArgs.inFileNames.length; ++i)
        {
            String prejavaFileName = parsedArgs.inFileNames[i];
            AssertAlways(prejavaFileName.endsWith(".prejava"));
            String javaFileName = prejavaFileName.substring(0, prejavaFileName.length()-".prejava".length())+".java";
            System.err.println("    prejavaFileName = \""+prejavaFileName+"\"");
            System.err.println("       javaFileName = \""+javaFileName+"\"");

            /*
            java.io.Reader reader = 
            open a reader to the filename
            */

        }
    }
} // class javacpp
