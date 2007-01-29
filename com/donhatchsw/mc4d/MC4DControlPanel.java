package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import com.donhatchsw.awt.MyPanel;
import com.donhatchsw.awt.Col;
import com.donhatchsw.awt.Row;
import com.donhatchsw.util.Listenable;

public class MC4DControlPanel
    extends Panel
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }


    // a label in the default font, except bold and one point size larger.
    private static class BigBoldLabel extends Label
    {
        public BigBoldLabel(String labelString)
        {
            super(labelString);
        }
        public Font getFont()
        {
            Font superfont = super.getFont();
            //System.out.println("label superfont = "+superfont);
            Font font = new Font(superfont.getName(), Font.BOLD, superfont.getSize()+1);
            return font;
        }
    }

    private static class CanvasOfSize extends Canvas
    {
        private Dimension preferredSize;
        public CanvasOfSize(int width, int height)
        {
            super();
            preferredSize = new Dimension(width, height);
        }
        public Dimension getPreferredSize()
        {
            return preferredSize;
        }
    }

    public static class TextFieldForFloat extends TextField
    {
        private void updateText(Listenable.Number f)
        {
            if (f instanceof Listenable.Float)
                setText(""+((Listenable.Float)f).get());
            else
                setText(""+(float)((Listenable.Double)f).get()); // XXX ARGH! we lose precision with this (float) cast, but if we don't do it, we can get, for example, 37.092999999999996 which looks lame.  should figure out another way to prevent that.
        }

        public TextFieldForFloat(final Listenable.Number f)
        {
            super("99.99"); // give it enough space for 99.999 (on my computer, always seems to give an extra space, which we don't need)
            updateText(f);
            f.addListener(new Listenable.Listener() {
                public void valueChanged()
                {
                    updateText(f);
                }
            });
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        f.setDouble(Double.valueOf(getText()).doubleValue());
                    }
                    catch (java.lang.NumberFormatException nfe)
                    {
                        // maybe should print an error message or something
                        updateText(f);
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
        private void updateThumb(Listenable.Number f)
        {
            double value = f.getDouble();
            double defaultValue = f.defaultDouble();
            double frac = (value-f.minDouble())/(f.maxDouble()-f.minDouble());
            setValue((int)(getMinimum() + ((getMaximum()-getVisibleAmount())-getMinimum())*frac));
        }

        public SliderForFloat(final Listenable.Number f)
        {
            super(Scrollbar.HORIZONTAL);

            // 3 significant digits seems reasonable...
            int min = (int)(f.minDouble()*1000);
            int max = (int)(f.maxDouble()*1000);
            int vis = (int)(.1*(max-min));
            setValues(0,   // value (we'll set it right later)
                      vis,
                      min,
                      max+vis);
            setUnitIncrement(1);   // .001 units
            setBlockIncrement(10); // .01 units

            f.addListener(new Listenable.Listener() {
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
                    f.setDouble(f.minDouble() + frac*(f.maxDouble()-f.minDouble()));
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

    private static class ColorSwatch extends CanvasOfSize
    {
        ColorSwatch(final Listenable.Color color, int width, int height)
        {
            super(width, height);
            setBackground(color.get());
            addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent me)
                {
                    //System.out.println("mouseClicked");
                } // mouseClicked
                public void mousePressed(MouseEvent me)
                {
                    //System.out.println("mousePressed");
                    color.set(new Color((float)Math.random(), (float)Math.random(), (float)Math.random())); // poor man's color chooser
                } // mousePressed
                public void mouseReleased(MouseEvent me)
                {
                    //System.out.println("mouseReleased");
                } // mouseReleased
                public void mouseEntered(MouseEvent me)
                {
                    //System.out.println("mouseEntered");
                } // mouseEntered
                public void mouseExited(MouseEvent me)
                {
                    //System.out.println("mouseExited");
                } // mouseExited
            }); // mouse listener
            color.addListener(new Listenable.Listener() {
                public void valueChanged()
                {
                    ColorSwatch.this.setBackground(color.get());
                }
            });
        }
    } // ColorSwatch

    private static class ColorSwatchMaybeAndCheckBoxMaybe extends Row
    {
        private Listenable.Color color;
        private Listenable.Boolean b;
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
            final Listenable.Color initcolor,
            final Listenable.Boolean initb,
            String name)
        {
            super(new Object[][]{
                (initcolor==null ? null : new Object[]{new ColorSwatch(initcolor,16,16)}),
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
                b.addListener(new Listenable.Listener() {
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
        public CheckboxThing(Listenable.Boolean b,
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
                           final Listenable listenables[])
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
                listenables[i].addListener(new Listenable.Listener() {
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
                           final Listenable listenable)
        {
            this(buttonLabel, new Listenable[]{listenable});
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
                            labelConstraintPairs[i][0] = new Label(helpMessage[i]);
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
                        Listenable.Number f, // Float or Double
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
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
                        Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
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
                        Listenable.Color color,
                        Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new GridBagConstraints(){{gridy = nRows;}});
        this.add(new ColorSwatchMaybeAndCheckBoxMaybe(color, b, labelString),
                 new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", new Listenable[]{color, b}),
                 new GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }

    public MC4DControlPanel(MC4DViewGuts.ViewParams viewParams)
    {
        this.setLayout(new GridBagLayout());
        addSingleLabelRow(new BigBoldLabel("Behavior"));
        addFloatSliderRow(
            "Twist duration",
            viewParams.nFrames90,
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
            viewParams.bounce,
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
            viewParams.stopBetweenMoves,
            new String[] {
                "Normally this option is checked, which means",
                "that during a solve or long undo or redo animation sequence,",
                "the animation slows to a stop after each different twist.",
                "",
                "Unchecking this option makes it so the animation does not stop",
                "or slow down between different twists,",
                "which makes long sequences of moves complete more quickly.",
                "",
                "You can turn this option on or off in the middle of an animation",
                "if you want.",
            });
        addCheckboxRow(
            "Require Ctrl to 3d Rotate",
            viewParams.requireCtrlTo3dRotate,
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
            viewParams.restrictRoll,
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
        addSingleComponentRow(new CanvasOfSize(1,1){{setBackground(Color.black);}}); // Totally lame separator
        addSingleLabelRow(new BigBoldLabel("Appearance"));
        addFloatSliderRow(
            "4d Face Shrink",
            viewParams.faceShrink4d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 4d",
                "(before the 4d->3d projection).",
                "Shrinking before the projection causes the apparent final 3d shape",
                "of the face to become less distorted (more cube-like),",
                "but more poorly fitting with its 3d neighbors.",
            });
        addFloatSliderRow(
            "4d Sticker Shrink",
            viewParams.stickerShrink4d,
            new String[] {
                "Specifies how much each sticker should be shrunk towards its center in 4d",
                "(before the 4d->3d projection).",
                "Shrinking before the projection causes the apparent final 3d shape",
                "of the sticker to become less distorted (more cube-like),",
                "but more poorly fitting with its 3d neighbors.",
            });
        addFloatSliderRow(
            "4d Eye Distance",
            viewParams.eyeW,
            new String[] {
                "Specifies the distance from the eye to the center of the puzzle in 4d.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "3d Face Strink",
            viewParams.faceShrink3d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).  Shrinking after the projection",
                "causes the face to retain its 3d shape as it shrinks.",
            });
        addFloatSliderRow(
            "3d Sticker Strink",
            viewParams.stickerShrink3d,
            new String[] {
                "Specifies how much each sticker should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).  Shrinking after the projection",
                "causes the sticker to retain its 3d shape as it shrinks.",
            });
        addFloatSliderRow(
            "3d Eye Distance",
            viewParams.eyeZ,
            new String[] {
                "Specifies the distance from the eye to the center of the puzzle in 3d.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "2d View Scale",
            viewParams.viewScale2d,
            new String[] {
                "Scales the final projected 2d image of the puzzle in the viewing window.",
                "(XXX coming soon: what the units mean exactly)",
            });
        addFloatSliderRow(
            "Stickers shrink to face boundaries",
            viewParams.stickersShrinkTowardsFaceBoundaries,
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
            viewParams.highlightByCubie,
            new String[] {
                "Normally when you hover the mouse pointer",
                "over a sticker, the sticker becomes highlighted.",
                "When this option is checked, hovering over a sticker",
                "causes the entire cubie the sticker is part of to be highlighted.",
            });
        addCheckboxRow(
            "Show shadows",
            viewParams.showShadows,
            new String[] {
                "Shows shadows on the ground and/or in the air.",
                "(It is a scientific fact that four dimensional",
                "objects can cast shadows in the air.)",
            });
        addCheckboxRow(
            "Antialias when still",
            viewParams.antialiasWhenStill,
            new String[] {
                "If this option is checked,",
                "the display will be antialiased (smooth edges)",
                "when the puzzle is at rest,",
                "if your computer's graphics hardware supports it.        ", // XXX hack to make the full window title visible on my computer
            });
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk face outlines",
            viewParams.nonShrunkFaceOutlineColor,
            viewParams.drawNonShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw shrunk face outlines",
            viewParams.shrunkFaceOutlineColor,
            viewParams.drawShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk sticker outlines",
            viewParams.nonShrunkStickerOutlineColor,
            viewParams.drawNonShrunkStickerOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw shrunk sticker outlines",
            viewParams.shrunkStickerOutlineColor,
            viewParams.drawShrunkStickerOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw ground",
            viewParams.groundColor,
            viewParams.drawGround,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Background color",
            viewParams.backgroundColor,
            null, // no checkbox
            null); // no help string
        {
            add(new Label("Face Colors:"),
                new GridBagConstraints(){{gridy = nRows; anchor = WEST; gridwidth = 4;}});
            add(new ResetButton("Reset to default", viewParams.faceColors),
                new GridBagConstraints(){{gridy = nRows;}});
            nRows++;
        }
        {
            int nFaces = 120;
            int nFacesPerRow = 32;
            int iFace = 0;
            int indents[] = {0,8,4,12,2,10,6,14,1,9,5,13,3,11,7,15};
            final int swatchWidth = 16;
            final int swatchHeight = 16;
            for (int iRow = 0; iRow * nFacesPerRow < nFaces; ++iRow)
            {
                int nFacesThisRow = nFaces - iRow*nFacesPerRow;
                if (nFacesThisRow > nFacesPerRow)
                    nFacesThisRow = nFacesPerRow;
                Canvas canvases[][] = new Canvas[nFacesThisRow][1];
                for (int i = 0; i < nFacesThisRow; ++i)
                {
                    Listenable.Color colorListenable = viewParams.faceColors[iFace];
                    canvases[i][0] = new ColorSwatch(colorListenable, swatchWidth,swatchHeight);
                    iFace++;
                }
                final int indent = indents[iRow%indents.length]*swatchWidth/indents.length;
                canvases = (Canvas[][])com.donhatchsw.util.Arrays.concat(new Canvas[][]{{new CanvasOfSize(indent,swatchHeight)}}, canvases);
                this.add(new CanvasOfSize(20,swatchHeight), // overall additional indent
                         new GridBagConstraints(){{gridy = nRows;}});
                add(new Row(canvases),
                    new GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER; anchor = WEST;}});
                nRows++;
            }
        }

        addSingleComponentRow(new CanvasOfSize(1,1){{setBackground(Color.black);}}); // Totally lame separator
        addSingleButtonRow(new ResetButton(
            "Reset All To Defaults",
            Listenable.allListenablesInObject(viewParams)));
    } // MC4DControlPanel ctor


    public static void main(String args[])
    {
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

            frame.add(new MC4DControlPanel(new MC4DViewGuts.ViewParams()));
            frame.pack();
            frame.show();
            nAlive[0]++;
        }
    }
} // MC4DControlPanel