package com.donhatchsw.mc4d;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;



public class MC4DViewApplet
    extends Applet
{
    //
    // Note, all public fields are settable as params
    // from the web page (e.g. <PARAM NAME='puzzleDescription' VALUE='{4,3,3} 3'>)
    // or command line (e.g. "puzzleDescription='{4,3,3} 3'")
    //
    public String puzzleDescription = "{4,3,3} 3";
    public int x = 50, y = 50; // for spawned viewers
    public int w = 300, h = 300; // for spawned viewers
    public boolean doDoubleBuffer = false; // crappier than we need to

    Image backBuffer = null;
    private Dimension backBufferSize = null;

    public MC4DViewApplet()
    {
    }

    public void init()
    {
        //System.out.println("in MC4DViewApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, 0);

        final MC4DViewGuts guts = new MC4DViewGuts(puzzleDescription);

        Canvas canvas = new Canvas() {
            public void update(Graphics g) { paint(g); } // don't flash
            public void paint(Graphics frontBufferGraphics)
            {
                Dimension size = size();
                int w = size.width, h = size.height;

                if (doDoubleBuffer)
                {
                    if (backBuffer == null
                     || !size.equals(backBufferSize))
                    {
                        System.out.println("    creating back buffer of size "+w+"x"+h+"");
                        backBuffer = this.createImage(w, h);
                        backBufferSize = size;
                    }
                }
                else
                    backBuffer = null;
                Graphics g = doDoubleBuffer ? backBuffer.getGraphics() : frontBufferGraphics;

                g.setColor(new Color(20,170,235)); // sky
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(20, 130, 20)); // ground
                g.fillRect(0, h*6/9, w, h);
                guts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another ancient view", 10, h-10);

                if (doDoubleBuffer)
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
                    case 'S'-'A'+1: // ctrl-s -- save to a cookie
                        com.donhatchsw.applet.CookieUtils.setCookie(MC4DViewApplet.this, "mc4dmodelstate", guts.model.toString());
                        break;
                    case 'L'-'A'+1: // ctrl-l -- load from a cookie
                        String stateString = com.donhatchsw.applet.CookieUtils.getCookie(MC4DViewApplet.this, "mc4dmodelstate");
                        // XXX do something with it! model needs a fromString!
                        break;
                }
            }
        });

        setLayout(new BorderLayout());
        add("Center", canvas);

        //System.out.println("out MC4DViewApplet init");
    } // init

    /**
    * Invoking this main is the same thing as invoking
    * the applet viewer's main with this class name as the first arg.
    * You can set the parameters on the command line,
    * e.g. puzzleDescription="{4,3,3} 3"
    */
    public static void main(String args[])
    {
        String appletViewerArgs[] = new String[args.length+1];
        appletViewerArgs[0] = "com.donhatchsw.mc4d.MC4DViewApplet";
        for (int i = 0; i < args.length; ++i)
            appletViewerArgs[i+1] = args[i];

        com.donhatchsw.applet.AppletViewer.main(appletViewerArgs);
    } // main

} // class MC4DViewApplet
