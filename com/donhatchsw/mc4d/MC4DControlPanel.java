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

    // XXX this needs to go elsewhere

        // First observation:
        //  If you look at the standard hue wheel,
        //  perceptually it changes very slowly
        //  near the primary colors r,g,b
        //  and very quickly near the secondary colors c,m,y.
        //  So if we want to evenly spread out colors around the wheel
        //  in terms of perception, we should crowd lots of samples
        //  around the secondary colors and not so many
        //  around the primary colors.
        //  Eyeballing it, it looks like the perceptual
        //  halfway point between a primary color
        //  and an adjacent secondary color
        //  is about 3/4 of the way towards the secondary color,
        //  so I'll use that as the basis for everything.
        // Second observation:
        //  This may be completely subjective,
        //  but it seems to me that there are 8 perceptually
        //  distinct hues:  the 6 usual primary&secondary hues,
        //  plus orange and violet.
        private static double linearizeHue(double perceptualHue)
        {
            double hue = perceptualHue - Math.floor(perceptualHue);
            if (true)
            {
                // The original perceptual hue is 1/8-oriented,
                // i.e. it thinks in terms of r,o,y.g,c,b,v,m.
                // Convert this into something that's 1/6-oriented,
                // via the piecewise linear mapping:
                //      0/8 -> 0/6  red
                //      1/8 ->  1/12  orange
                //      2/8 -> 1/6  yellow
                //      3/8 -> 2/6  green
                //      4/8 -> 3/6  cyan
                //      5/8 -> 4/6  blue
                //      6/8 ->  3/4   violet
                //      7/8 -> 5/6  magenta
                //      8/8 -> 6/6  red again
                final double perceptualHues[] = {
                    0/6.,   // red
                    1/12.,  //   orange
                    1/6.,   // yellow
                    2/6.,   // green
                    3/6.,   // cyan
                    4/6.,   // blue
                    3/4.,   //   violet
                    5/6.,   // magenta
                    6/6.,   // red again
                };
                int i = (int)(hue*8);
                double frac = hue*8 - i;
                hue = perceptualHues[i]*(1-frac)
                    + perceptualHues[i+1]*frac;
            }
            // Now, consider the fraction of the way
            // we are from the nearest secondary color
            // to the nearest primary color,
            // and square that fraction.
            // E.g. halfway from cyan to blue
            // turns into 1/4 of the way from cyan to blue.
            // That keeps us closer to the secondary color
            // longer, which is what we want,
            // because that's where the hue is varying fastest
            // perceptually.
            if (true)
            {
                int i = (int)(hue*6);
                double frac = hue*6 - i;
                if (i % 2 == 1)
                {
                    // y->g or c->b or m->r
                    frac = frac*frac;
                }
                else
                {
                    // y->r or c->g or m->b
                    frac = 1-(1-frac)*(1-frac);
                }
                hue = (i+frac)/6;
            }
            return hue;
        } // linearizeHue

        //
        // Saturations vary fastest perceptually
        // near zero.  So just square the saturation
        // so it doesn't vary as fast in linear space near zero.
        private static double linearizeSat(double perceptualSat)
        {
            return perceptualSat*perceptualSat;
        }


        //
        // Attempt to autogenerate some nice colors.
        // The sequence will be:
        //     1. Fully saturated 8 colors:
        //             red
        //             orange
        //             yellow
        //             green
        //             cyan
        //             blue
        //             violet
        //             magenta
        //             red
        //    2. Same 8 colors with saturation = .6
        //    3. Same 8 colors with saturation = .8
        //    4. Same 8 colors with saturation = .4
        //    Then repeat all of the above with in-between hues
        //    Then repeat all of the above with in-between saturations
        //        .9,.5,.7,.3
        // Eh, on second thought, use only those 4 saturations,
        // then keep doing in-between hues only;
        // that makes something that's easier to comprehend
        // when laying it out in rows of 32.
        // 
        private static void autoGenerateHueAndSat(int iFace,
                                                  double hueAndSat[/*2*/])
        {
            Assert(iFace >= 0);

            double hue = iFace/8.;
            double sat = 1.;
            iFace /= 8;

            double satDecrement = .4;
            double hueIncrement = 1/16.;

            for (int i = 0; i < 2; ++i)
            {
                if (iFace > 0)
                {
                    if (iFace % 2 == 1)
                        sat -= satDecrement;
                    satDecrement /= 2;
                    iFace /= 2;
                }
            }
            while (iFace > 0)
            {
                if (iFace % 2 == 1)
                    hue += hueIncrement;
                hueIncrement /= 2;
                iFace /= 2;
                /*
                if (iFace > 0)
                {
                    if (iFace % 2 == 1)
                        sat -= satDecrement;
                    satDecrement /= 2;
                    iFace /= 2;
                }
                */
            }
            hueAndSat[0] = hue;
            hueAndSat[1] = sat;
        } // autoGenerateHueAndSat

        private static Color autoGenerateColor(int iFace)
        {
            double hueAndSat[] = new double[2];
            autoGenerateHueAndSat(iFace, hueAndSat);
            double hue = hueAndSat[0];
            double sat = hueAndSat[1];
            hue = linearizeHue(hue);
            sat = linearizeSat(sat);
            return Color.getHSBColor((float)hue, (float)sat, 1.f);
        } // autoGenerateColor


    public static class TextFieldForFloat extends TextField
    {
        public TextFieldForFloat(final com.donhatchsw.util.Listenable.Float f)
        {
            super("99.99"); // give it enough space for 99.999 (on my computer, always seems to give an extra space, which we don't need)
            setText(""+f.get());
            f.addListener(new com.donhatchsw.util.Listenable.Listener() {
                public void valueChanged()
                {
                    setText(""+f.get());
                }
            });
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        f.set(Float.valueOf(getText()).floatValue());
                    }
                    catch (java.lang.NumberFormatException nfe)
                    {
                        // maybe should print an error message or something
                        setText(""+f.get());
                    }
                }
            });
        }
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
    } // TextFieldForFloat

    public static class SliderForFloat extends Scrollbar
    {
        private void updateThumb(com.donhatchsw.util.Listenable.Float f)
        {
            float value = f.get();
            float defaultValue = f.getDefaultValue();
            float frac = (value-f.getMinValue())/(f.getMaxValue()-f.getMinValue());
            setValue((int)(getMinimum() + ((getMaximum()-getVisibleAmount())-getMinimum())*frac));
        }

        public SliderForFloat(final com.donhatchsw.util.Listenable.Float f)
        {
            super(Scrollbar.HORIZONTAL);

            // 3 significant digits seems reasonable...
            int min = (int)(f.getMinValue()*1000);
            int max = (int)(f.getMaxValue()*1000);
            int vis = (int)(.1*(max-min));
            setValues(0,   // value (we'll set it right later)
                      vis,
                      min,
                      max+vis);
            setUnitIncrement(1);   // .001 units
            setBlockIncrement(10); // .01 units

            f.addListener(new com.donhatchsw.util.Listenable.Listener() {
                public void valueChanged()
                {
                    updateThumb(f);
                }
            });
            addAdjustmentListener(new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e)
                {
                    if (false)
                    {
                        System.out.println("==================");
                        System.out.println("min = "+getMinimum());
                        System.out.println("max = "+getMaximum());
                        System.out.println("visible = "+getVisibleAmount());
                        System.out.println("max-vis-min = "+(getMaximum()-getVisibleAmount()-getMinimum()));
                        System.out.println("e.getValue() = "+e.getValue());
                        System.out.println("getValue() = "+getValue());
                        System.out.println("getUnitIncrement() = "+getUnitIncrement());
                        System.out.println("getBlockIncrement() = "+getBlockIncrement());
                        System.out.println("getSize() = "+getSize());
                        System.out.println("getPreferredSize() = "+getPreferredSize());
                    }
                    // Doing the following in double precision makes a difference;
                    // if we do it in float, we get ugly values in the textfield
                    double frac = (double)(e.getValue()-getMinimum())
                                / (double)((getMaximum()-getVisibleAmount())-getMinimum());
                    f.set((float)(f.getMinValue() + frac*(f.getMaxValue()-f.getMinValue())));
                    // will trigger valueChanged()
                    // which will call updateThumb()
                }
            });
            updateThumb(f);
        }
        public Dimension getPreferredSize()
        {
            // default seems to be 50x18 on my computer...
            // give it more horizontal space than that
            Dimension preferredSize = super.getPreferredSize();
            //System.out.println("scrollbar.super.preferredSize() = "+preferredSize);
            preferredSize.width = 200;
            return preferredSize;
        }
    } // SliderForFloat

    private static class ColorSwatchMaybeAndCheckBoxMaybe extends Row
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

        public ColorSwatchMaybeAndCheckBoxMaybe(
            final com.donhatchsw.util.Listenable.Color initcolor,
            final com.donhatchsw.util.Listenable.Boolean initb,
            String name)
        {
            super(new Object[][]{
                (initcolor==null ? null : new Object[]{new Canvas(){{setSize(16,16); setBackground(initcolor.get());}}}),   // XXX this is messed up... if it gets compressed, it doesn't spring back
                {initb==null ? (Object)name : (Object)new Checkbox(name)},
                {"",new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;}}}, // just stretchable space
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

    // XXX think of a name
    private static class CheckboxThing extends ColorSwatchMaybeAndCheckBoxMaybe
    {
        public CheckboxThing(com.donhatchsw.util.Listenable.Boolean b,
                                String name)
        {
            super(null, b, name);
        }
    }

    // A button whose action resets one or more listenables,
    // and is enabled iff any of those listenables is non-default.
    private static class ResetButton extends Button
    {
        private boolean wasDefault[]; // one for each listenable
        private int nNonDefault; // number of falses in wasDefault
        public ResetButton(final String buttonLabel,
                           final com.donhatchsw.util.Listenable listenables[])
        {
            super(buttonLabel);
            // XXX to be clean, should really scrunch out null listeners here so that we don't suffer overhead for them every time the button is hit... not that anyone would ever notice though probably
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
                        // XXX I don't know if this behavior is the same on other VMs besides linux, need to check
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

                        helpWindow.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent we) {
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

    // I think I need this to keep track...
    // GridBagLayout, no matter how hard it tries,
    // is just defective, I think
    private int nRows = 0;
    private void addSingleLabelRow(Label label)
    {
        // A label on a row by itself gets left justified
        this.add(label, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                  anchor = WEST;}});
        nRows++;
    }
    private void addSingleButtonRow(Button button)
    {
        // A button on a row by itself gets centered
        this.add(button, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                   anchor = CENTER;}});
        nRows++;
    }
    private void addSingleComponentRow(Component component)
    {
        // Any other component on a row by itself gets stretched
        this.add(component, new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                      fill = HORIZONTAL; weightx = 1.;}});
        nRows++;
    }
    private void addFloatSliderRow(String labelString,
                        com.donhatchsw.util.Listenable.Float f,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(20,10);}}, // indent   XXX this is messed up... if it gets compressed, it doesn't spring back
                 new GridBagConstraints(){{gridy = nRows;}});
        this.add(new Label(labelString+":"),
                 new GridBagConstraints(){{anchor = WEST;
                                           gridy = nRows;}});
        this.add(new TextFieldForFloat(f),
                 new GridBagConstraints(){{gridy = nRows;}});
        this.add(new SliderForFloat(f),
                 new GridBagConstraints(){{gridy = nRows;
                                           fill = HORIZONTAL; weightx = 1.;}});
        this.add(new ResetButton("Reset to default", f),
                 new GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void addCheckboxRow(String labelString,
                        com.donhatchsw.util.Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(20,10);}}, // indent   XXX this is messed up... if it gets compressed, it doesn't spring back
                 new GridBagConstraints(){{gridy = nRows;}});
        this.add(new CheckboxThing(b, labelString),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", b),
                 new GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void addColorSwatchAndCheckboxRow(String labelString,
                        com.donhatchsw.util.Listenable.Color color,
                        com.donhatchsw.util.Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new Canvas(){{setSize(20,10);}}, // indent   XXX this is messed up... if it gets compressed, it doesn't spring back
                 new GridBagConstraints(){{gridy = nRows;}});
        this.add(new ColorSwatchMaybeAndCheckBoxMaybe(color, b, labelString),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", new com.donhatchsw.util.Listenable[]{color, b}),
                 new GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }

    public MC4DControlPanel(Stuff view)
    {
        this.setLayout(new GridBagLayout());
        addSingleLabelRow(new Label("Behavior"));
        addFloatSliderRow(
            "Twist duration",
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
        addFloatSliderRow(
            "Bounce",
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
        addCheckboxRow(
            "Stop Between Moves",
            view.stopBetweenMoves,
            new String[] {
                "Normally this option is checked, which means",
                "that during a solve or long undo or redo animation sequence,",
                "the animation slows to a stop after each different twist.",
                "",
                "Unchecking this option makes it so the animation does not stop",
                "or slow down between twists,",
                "which makes long sequences of moves complete more quickly.",
                "",
                "You can turn this option on or off in the middle of an animation",
                "if you want.",
            });
        addCheckboxRow(
            "Require Ctrl to 3d Rotate",
            view.requireCtrlTo3dRotate,
            new String[] {
                 "When this option is checked,",
                 "ctrl-mouse actions affect only the 3d rotation",
                 "and un-ctrled mouse actions",
                 "never affect the 3d rotation.",
                 "",
                 "When it is unchecked (the default),",
                 "mouse actions can both",
                 "start/stop the 3d rotation and do twists",
                 "(or 4d-rotate-to-center using middle mouse)",
                 "at the same time.",
            });
        addCheckboxRow(
            "Restrict Roll",
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
        addSingleComponentRow(new Canvas(){{setBackground(Color.black); setSize(1,1);}}); // Totally lame separator
        addSingleLabelRow(new Label("Appearance"));
        addFloatSliderRow(
            "4d Face Shrink",
            view.faceShrink4d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 4d",
                "(before the 4d->3d projection).",
                "Shrinking before the projection causes the apparent final 3d shape",
                "of the face to become less distorted (more cube-like),",
                "but more poorly fitting with its 3d neighbors.",
            });
        addFloatSliderRow(
            "4d Sticker Shrink",
            view.stickerShrink4d,
            new String[] {
                "Specifies how much each sticker should be shrunk towards its center in 4d",
                "(before the 4d->3d projection).",
                "Shrinking before the projection causes the apparent final 3d shape",
                "of the sticker to become less distorted (more cube-like),",
                "but more poorly fitting with its 3d neighbors.",
            });
        addFloatSliderRow(
            "4d Eye Distance",
            view.eyeW,
            new String[] {
                "Specifies the distance from the eye to the center of the puzzle in 4d.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "3d Face Strink",
            view.faceShrink3d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).  Shrinking after the projection",
                "causes the face to retain its 3d shape as it shrinks.",
            });
        addFloatSliderRow(
            "3d Sticker Strink",
            view.stickerShrink3d,
            new String[] {
                "Specifies how much each sticker should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).  Shrinking after the projection",
                "causes the sticker to retain its 3d shape as it shrinks.",
            });
        addFloatSliderRow(
            "3d Eye Distance",
            view.eyeZ,
            new String[] {
                "Specifies the distance from the eye to the center of the puzzle in 3d.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "2d View Scale",
            view.viewScale2d,
            new String[] {
                "Scales the final projected 2d image of the puzzle in the viewing window.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "Stickers shrink to face boundaries",
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
        addCheckboxRow(
            "Highlight by cubie",
            view.highlightByCubie,
            new String[] {
                "Normally when you hover the mouse pointer",
                "over a sticker, the sticker becomes highlighted.",
                "When this option is checked, hovering over a sticker",
                "causes the entire cubie the sticker is part of to be highlighted.",
            });
        addCheckboxRow(
            "Show shadows",
            view.showShadows,
            new String[] {
                "Shows shadows on the ground and/or in the air.",
                "(It is a scientific fact that four dimensional",
                "objects can cast shadows in the air.)",
            });
        addCheckboxRow(
            "Antialias when still",
            view.antialiasWhenStill,
            new String[] {
                "If this option is checked,",
                "the display will be antialiased (smooth edges)",
                "when the puzzle is at rest,",
                "if your computer's graphics hardware supports it.        ", // XXX hack to make the full window title visible on my computer
            });
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk face outlines",
            view.nonShrunkFaceOutlineColor,
            view.drawNonShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw shrunk face outlines",
            view.shrunkFaceOutlineColor,
            view.drawShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk sticker outlines",
            view.nonShrunkStickerOutlineColor,
            view.drawNonShrunkStickerOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw shrunk sticker outlines",
            view.shrunkStickerOutlineColor,
            view.drawShrunkStickerOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw ground",
            view.groundColor,
            view.drawGround,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Background color",
            view.backgroundColor,
            null, // no checkbox
            null); // no help string
        addSingleLabelRow(new Label("Face Colors:"));
        {
            int nFaces = 500;
            int nFacesPerRow = 32;
            int iFace = 0;
            int indents[] = {0,8,4,12,2,10,6,14,1,9,5,13,3,11,7,15}; // assumes swatch width = 16
            final int swatchSize = 16;
            for (int iRow = 0; iRow * nFacesPerRow < nFaces; ++iRow)
            {
                int nFacesThisRow = nFaces - iRow*nFacesPerRow;
                if (nFacesThisRow > nFacesPerRow)
                    nFacesThisRow = nFacesPerRow;
                Canvas canvases[][] = new Canvas[nFacesThisRow][1];
                for (int i = 0; i < nFacesThisRow; ++i)
                {
                    final Color color = autoGenerateColor(iFace);
                    canvases[i][0] = new Canvas(){{setSize(swatchSize,swatchSize); setBackground(color);}};
                    iFace++;
                }
                final int indent = indents[iRow%indents.length]*swatchSize/16;
                canvases = (Canvas[][])com.donhatchsw.util.Arrays.concat(new Canvas[][]{{new Canvas(){{setSize(indent,swatchSize);}}}}, canvases);
                this.add(new Canvas(){{setSize(20,swatchSize);}}, // indent   XXX this is messed up... if it gets compressed, it doesn't spring back
                         new GridBagConstraints(){{gridy = nRows;}});
                add(new Row(canvases),
                    new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER; anchor = WEST;}});
                nRows++;
            }
        }

        addSingleComponentRow(new Canvas(){{setBackground(Color.black); setSize(1,1);}}); // Totally lame separator
        addSingleButtonRow(new ResetButton(
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

        com.donhatchsw.util.Listenable.Color shrunkFaceOutlineColor = new com.donhatchsw.util.Listenable.Color(Color.black);
        com.donhatchsw.util.Listenable.Color nonShrunkFaceOutlineColor = new com.donhatchsw.util.Listenable.Color(Color.black);
        com.donhatchsw.util.Listenable.Color shrunkStickerOutlineColor = new com.donhatchsw.util.Listenable.Color(Color.black);
        com.donhatchsw.util.Listenable.Color nonShrunkStickerOutlineColor = new com.donhatchsw.util.Listenable.Color(Color.black);
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
                frame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
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
