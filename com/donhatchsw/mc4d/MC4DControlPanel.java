package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import com.donhatchsw.awt.MyPanel;
import com.donhatchsw.awt.Col;
import com.donhatchsw.awt.Row;

public class MC4DControlPanel
    extends Panel
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    // gridbag constraint that allows the added component to stretch horizontally
    private static GridBagConstraints stretchx = new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;}};

    private static class TextAndSliderAndReset extends Row
    {
        private com.donhatchsw.util.Listenable.Float f;
        private TextField textfield;
        private Scrollbar slider;

        private void updateShownValues()
        {
            float value = f.get();
            float defaultValue = f.getDefaultValue();
            textfield.setText(""+value);
            float frac = (value-f.getMinValue())/(f.getMaxValue()-f.getMinValue());
            slider.setValue((int)(slider.getMinimum() + ((slider.getMaximum()-slider.getVisibleAmount())-slider.getMinimum())*frac));
        }

        public TextAndSliderAndReset(final com.donhatchsw.util.Listenable.Float initf)
        {
            super(new Object[][]{
                  {new TextField("99.99"){ // give it enough space for 99.999 (on my computer, always seems to give an extra space, which we don't need)
                       public Dimension getPreferredSize()
                       {
                           // default seems taller than necessary
                           // on my computer... and in recent VMs it's even worse
                           // (changed from 29 to 31).
                           // Fudge it a bit...
                           // XXX not sure this will look good on all systems... if it doesn't, we can just remove it
                           // XXX hmm, actually makes things mess up when growing and shrinking, that's weird
                           Dimension preferredSize = super.getPreferredSize();
                           //System.out.println("textfield.super.preferredSize() = "+preferredSize);
                           if (true)
                               preferredSize.height -= 2;
                           return preferredSize;
                       }
                       // weird, the following is called during horizontal shrinking
                       // but not during horizontal expanding... if we don't do this too
                       // then it looks wrong when shrinking.  what a hack...
                       public Dimension getMinimumSize()
                       {
                           Dimension minimumSize = super.getMinimumSize();
                           //System.out.println("textfield.super.minimumSize() = "+minimumSize);
                           if (true)
                               minimumSize.height -= 2;
                           return minimumSize;
                       }
                   }},
                  {new Scrollbar(Scrollbar.HORIZONTAL){
                      public Dimension getPreferredSize()
                      {
                          // default seems to be 50x18 on my computer...
                          // give it more horizontal space than that
                          Dimension preferredSize = super.getPreferredSize();
                          //System.out.println("scrollbar.super.preferredSize() = "+preferredSize);
                          preferredSize.width = 200;
                          return preferredSize;
                      }
                   }, stretchx},
                  {new ResetButton("Reset to default", initf)},
            });
            // awkward, but we can't set members
            // until the super ctor is done
            this.textfield = (TextField)this.getComponent(0);
            this.slider = (Scrollbar)this.getComponent(1);
            this.f = initf;

            // 3 significant digits seems reasonable...
            // XXX Hmm but it would be nice to have individual unit and block increments
            int min = (int)(f.getMinValue()*1000);
            int max = (int)(f.getMaxValue()*1000);
            int vis = (int)(.1*(max-min));
            slider.setValues(0,   // value (we'll set it right later)
                             vis,
                             min,
                             max+vis);
            slider.setUnitIncrement(1); // .001 units
            slider.setBlockIncrement(10); // .01 units

            f.addListener(new com.donhatchsw.util.Listenable.Listener() {
                public void valueChanged()
                {
                    updateShownValues();
                }
            });
            textfield.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        f.set(Float.valueOf(textfield.getText()).floatValue());
                        // will trigger valueChanged()
                        // which will call updateShownValues()
                    }
                    catch (java.lang.NumberFormatException nfe)
                    {
                        // maybe should print an error message or something
                        updateShownValues();
                    }
                }
            });
            slider.addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e)
                {
                    if (false)
                    {
                        System.out.println("==================");
                        System.out.println("min = "+slider.getMinimum());
                        System.out.println("max = "+slider.getMaximum());
                        System.out.println("visible = "+slider.getVisibleAmount());
                        System.out.println("max-vis-min = "+(slider.getMaximum()-slider.getVisibleAmount()-slider.getMinimum()));
                        System.out.println("e.getValue() = "+e.getValue());
                        System.out.println("slider.getValue() = "+slider.getValue());
                        System.out.println("slider.getUnitIncrement() = "+slider.getUnitIncrement());
                        System.out.println("slider.getBlockIncrement() = "+slider.getBlockIncrement());
                        System.out.println("slider.getSize() = "+slider.getSize());
                        System.out.println("slider.getPreferredSize() = "+slider.getPreferredSize());
                    }
                    // Doing the following in double precision makes a difference;
                    // if we do it in float, we get ugly values in the textfield
                    double frac = (double)(e.getValue()-slider.getMinimum())
                                / (double)((slider.getMaximum()-slider.getVisibleAmount())-slider.getMinimum());
                    f.set((float)(f.getMinValue() + frac*(f.getMaxValue()-f.getMinValue())));
                    // will trigger valueChanged()
                    // which will call updateShownValues()
                }
            });
            updateShownValues();
        }
    } // class LabelTextSlider

    private static class ColorSwatchMaybeAndCheckBoxMaybeAndReset extends Row
    {
        private com.donhatchsw.util.Listenable.Color color;
        private com.donhatchsw.util.Listenable.Boolean b;
        private Canvas swatch;
        private Checkbox checkbox;

        private void updateShownValues()
        {
            if (color != null)
                swatch.setBackground(color.get());
            if (b != null)
                checkbox.setState(b.get());
        }

        public ColorSwatchMaybeAndCheckBoxMaybeAndReset(
            final com.donhatchsw.util.Listenable.Color initcolor,
            final com.donhatchsw.util.Listenable.Boolean initb,
            String name)
        {
            super(new Object[][]{
                initcolor==null ? null : new Object[]{new Canvas(){{setSize(10,10); setBackground(initcolor.get());}}},
                {initb==null ? (Object)name : (Object)new Checkbox(name)},
                {"",stretchx}, // just stretchable space
                {new ResetButton("Reset to default", new com.donhatchsw.util.Listenable[]{initcolor, initb})},
            });
            // awkward, but we can't set members
            // until the super ctor is done
            int i = 0;
            if (initcolor != null)
                this.swatch = (Canvas)this.getComponent(i++);
            if (initb != null)
                this.checkbox = (Checkbox)this.getComponent(i++);
            this.color = initcolor;
            this.b = initb;

            if (b != null)
            {
                b.addListener(new com.donhatchsw.util.Listenable.Listener() {
                    public void valueChanged()
                    {
                        updateShownValues();
                    }
                });
                checkbox.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e)
                    {
                        //System.out.println("in checkbox callback");
                        b.set(e.getStateChange() == ItemEvent.SELECTED);
                        // will trigger valueChanged()
                        // which will call updateShownValues()
                    }
                });
            }
            updateShownValues();
        }
    } // ColorSwatch

    private static class CheckboxAndReset extends ColorSwatchMaybeAndCheckBoxMaybeAndReset
    {
        public CheckboxAndReset(com.donhatchsw.util.Listenable.Boolean b,
                                String name)
        {
            super(null, b, name);
        }
    }
    private static class ColorSwatchAndReset extends ColorSwatchMaybeAndCheckBoxMaybeAndReset
    {
        public ColorSwatchAndReset(com.donhatchsw.util.Listenable.Color color,
                                   String name)
        {
            super(color, null, name);
        }
    }

    // A button whose action resets one or more listenables,
    // and is enabled iff one or more of those listenables is non-default.
    private static class ResetButton extends Button
    {
        private boolean wasDefault[];
        private int nNonDefault;
        public ResetButton(final String buttonLabel,
                           final com.donhatchsw.util.Listenable listenables[])
        {
            super(buttonLabel);
            // XXX should really scrunch out null listeners here so that we don't suffer overhead for them every time the button is hit... not that anyone would ever notice though probably
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    for (int i = 0; i < listenables.length; ++i)
                        if (listenables[i] != null)
                            listenables[i].resetToDefault();
                    Assert(nNonDefault == 0); // due to our valueChanged getting called
                }
            });
            wasDefault = new boolean[listenables.length];
            nNonDefault = 0;
            for (int _i = 0; _i < listenables.length; ++_i)
            {
                final int i = _i;
                if (listenables[i] == null)
                    continue;
                if (!(wasDefault[i] = listenables[i].isDefault()))
                    nNonDefault++;
                listenables[i].addListener(new com.donhatchsw.util.Listenable.Listener() {
                    public void valueChanged()
                    {
                        boolean isDefault = listenables[i].isDefault();
                        if (wasDefault[i] && !isDefault)
                        {
                            if (nNonDefault++ == 0)
                                setEnabled(true);
                        }
                        else if (!wasDefault[i] && isDefault)
                        {
                            if (--nNonDefault == 0)
                                setEnabled(false);
                            Assert(nNonDefault >= 0);
                        }
                        wasDefault[i] = isDefault;
                    }
                });
            }
            setEnabled(nNonDefault > 0);
        }
        // Convenience constructor for when there's just one listenable
        public ResetButton(final String buttonLabel,
                           final com.donhatchsw.util.Listenable listenable)
        {
            this(buttonLabel, new com.donhatchsw.util.Listenable[]{listenable});
        }
    } // class ResetButton

    private static class HelpButton extends Button
    {
        public HelpButton(final String helpWindowTitle,
                          final String helpMessage[])
        {
            super("Help");
            if (helpMessage != null)
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        // The preferred height of a label
                        // whose font metrics says height=15
                        // is 21.. kinda weird...
                        // and it makes it so it's too spread out
                        // vertically when we stack them up.
                        // The external and internal padding
                        // added by the gridbagconstraint is 0 by default,
                        // so we adjust by setting the internal
                        // vertical padding to -6
                        // to get rid of that extra space.
                        // XXX I don't know if this behavior is the same on other VMs, need to check
                        GridBagConstraints c = new GridBagConstraints(){{anchor = WEST; ipady = -6;}};
                        Object labelConstraintPairs[][] = new Object[helpMessage.length][2];
                        for (int i = 0; i < helpMessage.length; ++i)
                        {
                            labelConstraintPairs[i][0] = helpMessage[i];
                            labelConstraintPairs[i][1] = c;
                        }
                        Container panel = new Col(labelConstraintPairs);
                        final Frame helpWindow = new Frame("MC4D Help: "+helpWindowTitle);
                        helpWindow.add(panel);
                        helpWindow.setLocation(200,200); // XXX do I really want this? can I center it instead?  doing it so the help window doesn't end up in same place as main window.
                        helpWindow.pack();
                        helpWindow.setVisible(true);

                        helpWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                            public void windowClosing(java.awt.event.WindowEvent we) {
                                helpWindow.dispose();
                            }
                        });
                    }
                });
            else
            {
                // XXX ARGH! just want to not draw it, but leave space. this sucks. really need to overhaul this whole thing and make use of actual grid coords
                setEnabled(false); // don't tease the user
            }
        }
    }


    /**
    * Creates a control panel for the given MC4DViewGuts.
    * <pre>
        +----------------------------------------------------------+
        |Behavior                                                  |
        | <-> Twist speed                             [Reset][Help]|
        | <-> Bounce                                  [Reset][Help]|
        | [ ] Require Ctrl to 3d Rotate               [Reset][Help]|
        | [ ] Restrict roll                           [Reset][Help]|
        | [ ] Stop between moves                      [Reset][Help]|
        +----------------------------------------------------------+
        |Appearance                                                |
        | <-> 4d Face Shrink                          [Reset][Help]|
        | <-> 4d Sticker Shrink                       [Reset][Help]|
        | <-> 4d Eye Distance                         [Reset][Help]|
        | <-> 3d Face Shrink                          [Reset][Help]|
        | <-> 3d Sticker Shrink                       [Reset][Help]|
        | <-> 3d Eye Distance                         [Reset][Help]|
        | [ ] Stickers shrink towards face boundaries [Reset][Help]|
        | [ ] Highlight by cubie                      [Reset][Help]|
        | [ ] Show shadows                            [Reset][Help]|
        | [ ] Antialias when still                    [Reset][Help]|
        | [] [ ] Draw non-shrunk face outlines        [Reset][Help]|
        | [] [ ] Draw shrunk face outlines            [Reset][Help]|
        | [] [ ] Draw non-shrunk sticker outlines     [Reset][Help]|
        | [] [ ] Draw shrunk sticker outlines         [Reset][Help]|
        | [] [ ] Draw Ground                          [Reset][Help]|
        | [] Background color                         [Reset][Help]|
        | Sticker colors  (XXX not sure what this will look like)  |
        +----------------------------------------------------------+
     </pre>
    */

    private int nRows = 0;
    private void addLabelRow(Label label)
    {
        // A label on a row by itself gets left justified
        this.add(label, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                  anchor = WEST;}});
        nRows++;
    }
    private void addRow(Button button)
    {
        // A button on a row by itself gets centered
        this.add(button, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                   anchor = CENTER;}});
        nRows++;
    }
    private void addRow(Component component)
    {
        // Any other component on a row by itself gets stretched
        this.add(component, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                     fill = HORIZONTAL; weightx = 1.;}});
        nRows++;
    }
    private void addRow(String labelString,
                        com.donhatchsw.util.Listenable.Float f,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(10,10);}}, // indent
                 new GridBagConstraints(){{gridx = 0; gridy = nRows;}});
        this.add(new Label(labelString+":"),
                 new GridBagConstraints(){{anchor = WEST;
                                           gridx = 1; gridy = nRows;}});
        this.add(new TextAndSliderAndReset(f),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridx = 2; gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridx = 3; gridy = nRows;}});
        nRows++;
    }
    private void addRow(String labelString,
                        com.donhatchsw.util.Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(10,10);}}, // indent
                 new GridBagConstraints(){{gridx = 0; gridy = nRows;}});
        this.add(new CheckboxAndReset(b, labelString),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridx = 1; gridwidth = 2; gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridx = 3; gridy = nRows;}});
        nRows++;
    }
    private void addRow(String labelString,
                        com.donhatchsw.util.Listenable.Color color,
                        com.donhatchsw.util.Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(10,10);}}, // indent
                 new GridBagConstraints(){{gridx = 0; gridy = nRows;}});
        this.add(new ColorSwatchMaybeAndCheckBoxMaybeAndReset(color, b, labelString),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           anchor = WEST; gridx = 1; gridwidth = 2; gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridx = 3; gridy = nRows;}});
        nRows++;
    }

    public MC4DControlPanel(Stuff view)
    {
        this.setLayout(new java.awt.GridBagLayout());
        addRow(new Label("Behavior"));
        addRow("Twist duration",
               view.twistDuration,
               new String[] {
                    "Controls the speed of puzzle twists (left- or right-click)",
                    "and 4d rotates (middle-click or alt-click).",
                    "",
                    "The units are in animation frames per 90 degree twist;",
                    "so if you set Twist Duration to 15,",
                    "you will see the animation refresh 15 times during each 90 degree",
                    "twist.  Twists of angles other than 90 degrees will take",
                    "a correspondingly longer or shorter number of frames.",
                    "",
                    "You can change this value in the middle of an animation if you want.",
               });
        addRow("Bounce",
               view.bounce,
               new String[] {
                    "Normally the forces used for twists and rotates",
                    "are those of a critically damped spring,",
                    "so that the moves complete smoothly in the required amount of time",
                    "with the smallest possible acceleration.",
                    "",
                    "Setting this option to a nonzero value",
                    "will cause the spring forces to be underdamped instead,",
                    "so that the twists and rotates will overshoot their targets",
                    "before settling, resulting in a bouncy feeling.",
                    "",
                    "You can change this value in the middle of an animation if you want.",
               });
        addRow("Stop Between Moves",
               view.stopBetweenMoves,
               new String[] {
                    "Normally this option is checked, which means",
                    "that during a solve or long undo or redo animation sequence,",
                    "the animation slows to a stop after each twist.",
                    "",
                    "Unchecking this option makes it so the animation does not stop",
                    "or slow down between twists,",
                    "which makes long sequences of moves complete more quickly.",
                    "",
                    "You can turn this option on or off in the middle of an animation",
                    "if you want.",
               });
        addRow("Require Ctrl to 3d Rotate",
               view.requireCtrlTo3dRotate,
               new String[] {
                     "When this option is checked,",
                     "ctrl-mouse actions affect only the 3d rotation",
                     "and un-ctrled mouse actions",
                     "never affect the 3d rotation.",
                     "",
                     "When it is unchecked (the default), the ctrl key is ignored",
                     "and mouse actions can both",
                     "start/stop the 3d rotation and do twists",
                     "(or 4d-rotate-to-center using middle mouse)",
                     "at the same time.",
               });
        addRow("Restrict Roll",
               view.restrictRoll,
               new String[] {
                     "When this option is checked,",
                     "3d rotations are restricted so that some hyperface",
                     "of the puzzle always points in a direction somewhere on the arc",
                     "between the top of the observer's head",
                     "and the back of the observer's head.",
                     "",
                     "You can turn this option on or off while the puzzle is spinning",
                     "if you want.",
               });
        addRow(new Canvas(){{setBackground(java.awt.Color.black); setSize(1,1);}}); // Totally lame separator
        addRow(new Label("Appearance"));
        addRow("4d Face Shrink",
               view.faceShrink4d,
               new String[] {
                    "Specifies how much each face should be shrunk towards its center in 4d",
                    "(before the 4d->3d projection).",
                    "Shrinking before the projection causes the apparent final 3d shape",
                    "of the face to become less distorted (more cube-like),",
                    "but more poorly fitting with its 3d neighbors.",
               });
        addRow("4d Sticker Shrink",
               view.stickerShrink4d,
               new String[] {
                    "Specifies how much each sticker should be shrunk towards its center in 4d",
                    "(before the 4d->3d projection).",
                    "Shrinking before the projection causes the apparent final 3d shape",
                    "of the sticker to become less distorted (more cube-like),",
                    "but more poorly fitting with its 3d neighbors.",
               });
        addRow("4d Eye Distance",
               view.eyeW,
               new String[] {
                    "Specifies the distance from the eye to the center of the puzzle in 4d.",
                    "(XXX coming soon: what the units mean exactly)",
               });
        addRow("3d Face Strink",
               view.faceShrink3d,
               new String[] {
                    "Specifies how much each face should be shrunk towards its center in 3d",
                    "(after the 4d->3d projection).  Shrinking after the projection",
                    "causes the face to retain its 3d shape as it shrinks.",
               });
        addRow("3d Sticker Strink",
               view.stickerShrink3d,
               new String[] {
                    "Specifies how much each sticker should be shrunk towards its center in 3d",
                    "(after the 4d->3d projection).  Shrinking after the projection",
                    "causes the sticker to retain its 3d shape as it shrinks.",
               });
        addRow("3d Eye Distance",
               view.eyeZ,
               new String[] {
                    "Specifies the distance from the eye to the center of the puzzle in 3d.",
                    "(XXX coming soon: what the units mean exactly)",
               });
        addRow("2d View Scale",
               view.viewScale2d,
               new String[] {
                    "Scales the final projected 2d image of the puzzle in the viewing window.",
                    "(XXX coming soon: what the units mean exactly)",
               });
        addRow("Stickers shrink to face boundaries",
               view.stickersShrinkTowardsFaceBoundaries,
               new String[] {
                    "Normally this option is set to 0 which causes stickers",
                    "to shrink towards their centers.",
                    "Setting it to 1 causes stickers to shrink towards",
                    "the face boundaries instead (so if the 4d and 3d face shrinks are 1,",
                    "this will cause all the stickers on a given cubie to be contiguous).",
                    "Setting it to a value between 0 and 1 will result",
                    "in shrinking towards some point in between.",
               });
        addRow("Highlight by cubie",
               view.highlightByCubie,
               new String[] {
                    "Normally when you hover the mouse pointer",
                    "over a sticker, the sticker becomes highlighted.",
                    "When this option is checked, hovering over a sticker",
                    "causes the entire cubie the sticker is part of to be highlighted.",
               });
        addRow("Show shadows",
               view.showShadows,
               new String[] {
                    "Shows shadows on the ground and/or in the air.",
                    "(It is a scientific fact that four dimensional",
                    "objects can cast shadows in the air.)",
               });
        addRow("Antialias when still",
               view.antialiasWhenStill,
               new String[] {
                    "If this option is checked,",
                    "the display will be antialiased (smooth edges)",
                    "when the puzzle is at rest,",
                    "if your computer's graphics hardware supports it.        ", // XXX hack to make the full window title visible on my computer
               });
        addRow("Draw non-shrunk face outlines",
               view.nonShrunkFaceOutlineColor,
               view.drawNonShrunkFaceOutlines,
               null); // no help string
        addRow("Draw shrunk face outlines",
               view.shrunkFaceOutlineColor,
               view.drawShrunkFaceOutlines,
               null); // no help string
        addRow("Draw non-shrunk sticker outlines",
               view.nonShrunkStickerOutlineColor,
               view.drawNonShrunkStickerOutlines,
               null); // no help string
        addRow("Draw shrunk sticker outlines",
               view.shrunkStickerOutlineColor,
               view.drawShrunkStickerOutlines,
               null); // no help string
        addRow("Draw ground",
               view.groundColor,
               view.drawGround,
               null); // no help string
        addRow("Background color",
               view.backgroundColor,
               null, // no checkbox
               null); // no help string
        addRow(new Canvas(){{setBackground(java.awt.Color.black); setSize(1,1);}}); // Totally lame separator
        addRow(new ResetButton(
               "Reset All To Defaults",
               new com.donhatchsw.util.Listenable[]{
                   view.twistDuration,
                   view.bounce,
                   view.faceShrink4d,
                   view.stickerShrink4d,
                   view.eyeW,
                   view.faceShrink3d,
                   view.stickerShrink3d,
                   view.eyeZ,
                   view.viewScale2d,
                   view.stickersShrinkTowardsFaceBoundaries,
                   view.requireCtrlTo3dRotate,
                   view.restrictRoll,
                   view.stopBetweenMoves,
                   view.highlightByCubie,
                   view.showShadows,
                   view.antialiasWhenStill,
                   view.drawNonShrunkFaceOutlines,
                   view.drawShrunkFaceOutlines,
                   view.drawNonShrunkStickerOutlines,
                   view.drawShrunkStickerOutlines,
                   view.drawGround,
                   view.shrunkFaceOutlineColor,
                   view.nonShrunkFaceOutlineColor,
                   view.shrunkStickerOutlineColor,
                   view.nonShrunkStickerOutlineColor,
                   view.groundColor,
                   view.backgroundColor,
               }));
    } // MC4DControlPanel ctor

    public static class Stuff
    {
        com.donhatchsw.util.Listenable.Float twistDuration = new com.donhatchsw.util.Listenable.Float(0.f, 100.f, 30.f);
        com.donhatchsw.util.Listenable.Float bounce = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, 0.f);
        com.donhatchsw.util.Listenable.Float faceShrink4d = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, .5f);
        com.donhatchsw.util.Listenable.Float stickerShrink4d = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, .5f);
        com.donhatchsw.util.Listenable.Float eyeW = new com.donhatchsw.util.Listenable.Float(1.f, 10.f, 2.f);
        com.donhatchsw.util.Listenable.Float faceShrink3d = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, .5f);
        com.donhatchsw.util.Listenable.Float stickerShrink3d = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, .5f);
        com.donhatchsw.util.Listenable.Float eyeZ = new com.donhatchsw.util.Listenable.Float(1.f, 10.f, 2.f);
        com.donhatchsw.util.Listenable.Float viewScale2d = new com.donhatchsw.util.Listenable.Float(1.f, 10.f, 2.f);
        com.donhatchsw.util.Listenable.Float stickersShrinkTowardsFaceBoundaries = new com.donhatchsw.util.Listenable.Float(0.f, 1.f, 0.f);
        com.donhatchsw.util.Listenable.Boolean requireCtrlTo3dRotate = new com.donhatchsw.util.Listenable.Boolean(false);
        com.donhatchsw.util.Listenable.Boolean restrictRoll = new com.donhatchsw.util.Listenable.Boolean(false);
        com.donhatchsw.util.Listenable.Boolean stopBetweenMoves = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean highlightByCubie = new com.donhatchsw.util.Listenable.Boolean(false);
        com.donhatchsw.util.Listenable.Boolean showShadows = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean antialiasWhenStill = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean drawNonShrunkFaceOutlines = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean drawShrunkFaceOutlines = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean drawNonShrunkStickerOutlines = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean drawShrunkStickerOutlines = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean drawGround = new com.donhatchsw.util.Listenable.Boolean(true);

        com.donhatchsw.util.Listenable.Color shrunkFaceOutlineColor = new com.donhatchsw.util.Listenable.Color(java.awt.Color.black);
        com.donhatchsw.util.Listenable.Color nonShrunkFaceOutlineColor = new com.donhatchsw.util.Listenable.Color(java.awt.Color.black);
        com.donhatchsw.util.Listenable.Color shrunkStickerOutlineColor = new com.donhatchsw.util.Listenable.Color(java.awt.Color.black);
        com.donhatchsw.util.Listenable.Color nonShrunkStickerOutlineColor = new com.donhatchsw.util.Listenable.Color(java.awt.Color.black);
        com.donhatchsw.util.Listenable.Color groundColor = new com.donhatchsw.util.Listenable.Color(new Color(20, 130, 20));
        com.donhatchsw.util.Listenable.Color backgroundColor = new com.donhatchsw.util.Listenable.Color(new Color(20, 170, 235));
    }

    public static void main(String args[])
    {
        Stuff stuff = new Stuff();
        final int nAlive[] = {0};
        for (int i = 0; i < 2; ++i)
        {
            final Frame frame = new Frame("MC4DControlPanel Test");

            // In >=1.5, this seems to be the only way
            // to get the window to close when the user hits the close
            // window button.
            // (overriding handleEvent doesn't work any more)
            {
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent we) {
                        frame.dispose();
                        if (--nAlive[0] == 0)
                        {
                            System.out.println("Ciao!");
                            System.exit(0); // asinine way of doing things
                        }
                        else
                        {
                            System.out.println("ciao!");
                        }
                    }
                });
            }

            frame.add(new MC4DControlPanel(stuff));
            frame.pack();
            frame.show();
            nAlive[0]++;
        }
    }
} // MC4DControlPanel
