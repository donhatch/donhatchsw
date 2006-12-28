package com.donhatchsw.MagicCube;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;



// See http://www.cookiecentral.com/code/javacook2.htm
// for how do do cookies.
// To compile, need put one of the following in my classpath:
/*
    /usr/java/jdk1.3.1_18/jre/lib/javaplugin.jar
*/
import netscape.javascript.JSObject;
import netscape.javascript.JSException;



public class MC4DViewApplet
    extends Applet
{
    //
    // Note, all public fields are settable as params
    // from the web page (e.g. <PARAM NAME='schlafli' VALUE='{4,3,3}'>)
    // or command line (e.g. "schlafli='{4,3,3}'")
    // XXX ha ha, if I had a command line
    //
    public String schlafli = "{4,3,3}";
    public int length = 3;
    public double doubleLength = -1;
    public int x = 50, y = 50; // for spawned viewers
    public int w = 300, h = 300; // for spawned viewers
    public boolean doDoubleBuffer = false; // crappier than we need to

    private int backBufferWidth = -1;
    private int backBufferHeight = -1;
    Image backBuffer = null;

    public MC4DViewApplet()
    {
    }

    public void init()
    {
        //System.out.println("in MC4DViewApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, 0);

        if (doubleLength == -1)
            doubleLength = (double)length;
        final MC4DViewGuts guts = new MC4DViewGuts(schlafli, length, doubleLength);

        Canvas canvas = new Canvas() {
            public void update(Graphics g) { paint(g); } // don't flash
            public void paint(Graphics frontBufferGraphics)
            {
                int w = getWidth(), h = getHeight();
                if (doDoubleBuffer)
                {
                    if (backBuffer == null
                     || w != backBufferWidth
                     || h != backBufferHeight)
                    {
                        System.out.println("    creating back buffer of size "+w+"x"+h+"");
                        backBuffer = this.createImage(w, h);
                        backBufferWidth = w;
                        backBufferHeight = h;
                    }
                }
                else
                    backBuffer = null;
                Graphics g = backBuffer != null ? backBuffer.getGraphics() : frontBufferGraphics;

                g.setColor(new Color(20,170,235)); // sky
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(20, 130, 20)); // ground
                g.fillRect(0, h*6/9, w, h);
                guts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another ancient view", 10, h-10);

                if (backBuffer != null)
                    frontBufferGraphics.drawImage(backBuffer, 0, 0, this);
            }
        };
        guts.attachListeners(canvas, true);

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        canvas.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                switch (c)
                {
                    case 'N'-'A'+1: // ctrl-n
                        if (ke.isShiftDown())
                            MC4DViewGuts.makeExampleModernViewer(guts,x+20-w,y+20,w,h); // ctrl-shift-N
                        else
                            MC4DViewGuts.makeExampleAncientViewer(guts,x+20,y+20,w,h,doDoubleBuffer);  // ctrl-n
                        break;
                    case 'S'-'A'+1: // ctrl-s -- save
                        saveTheCookie("moose", "a\nb\r\nc");
                        saveTheCookie("mc4dpuzzlestate", guts.model.genericPuzzleDescription.toString());
                        break;
                    case 'L'-'A'+1: // ctrl-l -- load
                        String stateString = loadTheCookie("mc4dpuzzlestate");
                        break;
                    case 'E'-'A'+1: // ctrl-e -- example
                        cookieExample();
                        break;
                }
            }
        });

        setLayout(new BorderLayout());
        add("Center", canvas);

        //System.out.println("out MC4DViewApplet init");
    } // init



    private void cookieExample()
    {
        System.out.println("in cookieExample");
     try
         {
         JSObject window = JSObject.getWindow(this );
         JSObject document = (JSObject)window.getMember( "document" );

         // write a one more new cookie
         //document.setMember( "cookie", "drink='root beer'; expires=Fri, 31-Jan-2007 00:00:01 GMT;" );
         java.util.Calendar cal = java.util.Calendar.getInstance();
         cal.add(java.util.Calendar.YEAR, 1); // expires in 1 year
         String expires=cal.getTime().toString();
         document.setMember( "cookie", "drink='coke beer'; expires="+expires+";");

         // get all the unexpired cookies
         String mycookies = (String) document.getMember( "cookie" );
         System.out.println("======================");
         System.out.println(mycookies);
         System.out.println("======================");
         }
      catch ( Exception e )
         {
         }
        System.out.println("out cookieExample");
    }

    private void saveTheCookie(String name, String rawValue)
    {
        String cookedValue = null;
        try
        {
            cookedValue = java.net.URLEncoder.encode(rawValue, "UTF-8");
        }
        catch (Throwable ex)
        {
            System.out.println("couldn't save cooked named "+name+": uuencoding failed");
        }

        System.out.println("in saveTheCookie");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.YEAR, 1); // expires in 1 year
        String expires = cal.getTime().toString();
        try
        {
            JSObject window = JSObject.getWindow(this );
            JSObject document = (JSObject)window.getMember( "document" );
            // write a one more new cookie
            document.setMember( "cookie", name+"="+cookedValue+"; expires="+expires+";");


        }
        catch (Throwable ex)
        {
            System.out.println("Caught something bad trying to save the cookie: "+ex);
        }
        System.out.println("out saveTheCookie");
    } // saveTheCookie

    private String loadTheCookie(String name)
    {
        System.out.println("in loadTheCookie");
        StringBuffer sb = new StringBuffer();
        try
        {
            JSObject window = JSObject.getWindow(this );
            JSObject document = (JSObject)window.getMember( "document" );
            // get all the unexpired cookies
            String mycookies = (String) document.getMember( "cookie" );
            System.out.println("======================");
            System.out.println(mycookies);
            System.out.println("======================");
            sb.append(mycookies);
        }
        catch (Throwable ex)
        {
            System.out.println("This browser may not support Java to Javascript communication:"+ex);
        }
        System.out.println("out loadTheCookie");
        return "moose";
    } // loadTheCookie


} // class MC4DViewApplet
