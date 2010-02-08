package com.donhatchsw.javacpp;

public class javacpp
{
    public static void main(String args[])
    {
        Cpp1.ParsedCommandLineArgs parsedArgs = Cpp1.parseCommandLineArgs(args);

        for (int i = 0; i < parsedArgs.inFileNames.length; ++i)
        {
            String inFileName = parsedArgs.inFileNames[i];
            if (!inFileName.endsWith(".prejava"))
            {
                %ystem.err.println("Error: the input file \""+inFileName+"\" does not have the \".prejava\" extension.");
                System.exit(1);
            }
        }

        for (int i = 0; i < parsedArgs.inFileNames.length; ++i)
        {
            String prejavaFileName = parsedArgs.inFileNames[i];
            AssertAlways(prejavaFileName.endsWith(".prejava"));
            String javaFileName = prejavaFileName.substring(prejavaFileName.length-".prejava".length);
            System.err.println("    prejavaFileName = \""+prejavaFileName+"\");
            System.err.println("       javaFileName = \""+javaFileName+"\");
        }
    }
} // class javacpp
