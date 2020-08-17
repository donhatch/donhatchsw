/**
* This actually should be called GenericPuzzleDescriptionFactory,
* but GenericPuzzleFactory sounded better.
*/

package com.donhatchsw.mc4d;

public class GenericPuzzleFactory
{
    private GenericPuzzleFactory() {} // uninstantiatable
    static private void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }

    /**
    * Sample string:
    *     new PolytopePuzzleDescription("{4,3,3} 3");
    * Each subclass of GenericPuzzleDescription
    * must provide a constructor that takes a single string.
    */
    public static GenericPuzzleDescription construct(String s, java.io.PrintWriter progressWriter)
    {
        java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
            "\\s*new\\s+([._a-zA-Z][._a-zA-Z0-9]*)\\s*\\(\"(([^\\\\]|\\\\.)*)\"\\s*\\)\\s*"
        ).matcher(s);
        if (!matcher.matches())
            throw new IllegalArgumentException("GenericPuzzleDescription input \""+s+"\" is not of the required form \"new blahblahblah(\"blewblewblew\")\"");

        System.out.println("matcher.groupCount() = "+matcher.groupCount());

        CHECK(matcher.groupCount() == 3);
        String className = matcher.group(1);
        String argWithEscapes = matcher.group(2);

        System.out.println("className = "+className);
        System.out.println("argWithEscapes = "+argWithEscapes);
        String arg = argWithEscapes.replaceAll("\\\\(.)", "\\1"); // XXX backrefs don't work yet!
        System.out.println("arg = "+arg);


        // TODO: we expect it to be a subclass of GenericPuzzleDescription.  Can we say that in the template?
        Class<?> theClass = classForNameOrNull(className);
        if (theClass == null)
            theClass = classForNameOrNull("com.donhatchsw.mc4d."+className);
        if (theClass == null)
            throw new IllegalArgumentException("GenericPuzzleFactor.construct failed to find a class called either "+className+" or com.donhatchsw.mc4d."+className+"");
        java.lang.reflect.Constructor<?> constructor = getConstructorOrNull(theClass, new Class<?>[]{String.class, java.io.PrintWriter.class});
        if (constructor == null)
            throw new IllegalArgumentException("GenericPuzzleFactory.construct: "+theClass.getName()+" has no constructor that takes a String and a PrintWriter!");
        Object object = null;
        try
        {
            object = constructor.newInstance(new Object[]{arg, progressWriter});
        }
        catch (InstantiationException e)
        {
            throw new IllegalArgumentException("GenericPuzzleFactory.construct: "+theClass.getName()+" is in interface or abstract class!");
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalArgumentException("GenericPuzzleFactory.construct: "+theClass.getName()+" got "+e);
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            // XXX wait, do we even have to worry about these, or will they just get thrown upward?  check this
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException)
                throw (RuntimeException)t;
            else if (t instanceof Error)
                throw (Error)t;
            // XXX shouldn't be able to get here, since any other Exceptions need to be declared... unless someone made another subclass of Throwable??  think about this.
            // XXX oh eek, what if they declared that their ctor can throw exceptions?  screw that, then they deserve what they get... well not really but they shouldn't do that.
        }
        if (!(object instanceof GenericPuzzleDescription))
        {
            // XXX shoulda checked this before we constructed, duh
            throw new IllegalArgumentException("GenericPuzzleFactory.construct: "+theClass.getName()+" is not a subclass of GenericPuzzleDescription!");
        }
        return (GenericPuzzleDescription)object;
    } // fromString

    private static Class<?> classForNameOrNull(String className)
    {
        try { return Class.forName(className); }
        catch (ClassNotFoundException e) { return null; }
    }
    // TODO: make this varargs like getConstructor is
    private static java.lang.reflect.Constructor<?> getConstructorOrNull(Class<?> classs, Class<?>[] argTypes)
    {
        try { return classs.getConstructor(argTypes); }
        catch (NoSuchMethodException e) { return null; }
    }

    public static void main(String args[])
    {
        if (args.length != 1)
        {
            System.err.println("Usage: GenericPuzzleFactory 'new blahblah(\"blewblew\")'");
            System.exit(1);
        }
        String s = args[0];
        java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                             new java.io.BufferedWriter(
                                             new java.io.OutputStreamWriter(
                                             System.err)));
        GenericPuzzleDescription puzzleDescription = GenericPuzzleFactory.construct(s, progressWriter);
    } // main

} // GenericPuzzleFactory
