package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import com.donhatchsw.awt.MyPanel;
import com.donhatchsw.awt.Col;
import com.donhatchsw.awt.Row;

public class MC4DControlPanel
    extends Panel
{
    // gridbag constraint that allows the added component to stretch horizontally
    private static GridBagConstraints stretchx = new GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;}};

    private static class TextAndSliderAndReset extends Row
    {
        private com.donhatchsw.util.Listenable.Float f;
        private TextField textfield;
        private Scrollbar slider;
        private Button resetButton;

        private void updateShownValues()
        {
            float value = f.get();
            float defaultValue = f.getDefaultValue();
            textfield.setText(""+value);
            slider.setValue((int)(slider.getMinimum() + ((slider.getMaximum()-slider.getVisibleAmount())-slider.getMinimum())*value));
            resetButton.setEnabled(value != defaultValue);
        }

        public TextAndSliderAndReset(com.donhatchsw.util.Listenable.Float initf)
        {
            super(new Object[][]{
                  {new TextField("XXXXXX")}, // give it some space
                  {new Scrollbar(Scrollbar.HORIZONTAL){
                      public Dimension getPreferredSize()
                      {
                          // default seems to be 50x15 on my computer...
                          // give it more space than that
                          return new Dimension(200,15);
                      }
                   }, stretchx},
                  {new Button("Reset to default")},
            });
            // awkward, but we can't set members
            // until the super ctor is done
            this.textfield = (TextField)this.getComponent(0);
            this.slider = (Scrollbar)this.getComponent(1);
            this.resetButton = (Button)this.getComponent(2);
            this.f = initf;

            // 3 significant digits seems reasonable...
            int max = 1000;
            int min = 0;
            int vis = (int)(.1*max);
            slider.setValues(0,   // value (we'll set it right later)
                             vis,
                             min,
                             max+vis);
            slider.setUnitIncrement((int)(.001 * max));
            slider.setBlockIncrement((int)(.01 * max));
            // Slider width seems to be 50 by default on my machine (linux),
            // don't know if it's like that everywhere, can't find any mention of it
            // in the doc.
            // Expand it to 200.  I don't know what the difference is
            // between this and setPreferredSize() (other than
            // that setPreferredSize isn't available on old VMs).
            System.out.println("Before setting sise, slider width="+slider.getWidth()+", height="+slider.getHeight()+"");
            System.out.println("Before setting sise, slider preferred size = "+slider.getPreferredSize());
            //slider.setSize(200, slider.getHeight());
            //slider.setSize(200,200);
            slider.setSize(300,0);

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
                    if (true)
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
                    f.set((float)(e.getValue()-slider.getMinimum())
                        / (float)((slider.getMaximum()-slider.getVisibleAmount())-slider.getMinimum()));
                    // will trigger valueChanged()
                    // which will call updateShownValues()
                }
            });
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    f.set(f.getDefaultValue());
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
        private Button resetButton;

        private void updateShownValues()
        {
            if (color != null)
                swatch.setBackground(color.get());
            if (b != null)
                checkbox.setState(b.get());
            resetButton.setEnabled(color!=null && !color.get().equals(color.getDefaultValue())
                                || b!=null && b.get() != b.getDefaultValue());
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
                {new Button("Reset to default")},
            });
            // awkward, but we can't set members
            // until the super ctor is done
            int i = 0;
            if (initcolor != null)
                this.swatch = (Canvas)this.getComponent(i++);
            if (initb != null)
                this.checkbox = (Checkbox)this.getComponent(i++);
            else
                i++; // past the label
            i++; // past the stretchy space
            this.resetButton = (Button)this.getComponent(i++);
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
                        System.out.println("in checkbox callback");
                        b.set(e.getStateChange() == ItemEvent.SELECTED);
                        // will trigger valueChanged()
                        // which will call updateShownValues()
                    }
                });
            }
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    if (b != null)
                        b.set(b.getDefaultValue());
                    // will trigger valueChanged()
                    // which will call updateShownValues()
                }
            });

            updateShownValues();
        }
    } // ColorSwatch

    private static class CheckboxAndReset extends ColorSwatchMaybeAndCheckBoxMaybeAndReset
    {
        CheckboxAndReset(com.donhatchsw.util.Listenable.Boolean b,
                         String name)
        {
            super(null, b, name);
        }
    }
    private static class ColorSwatchAndReset extends ColorSwatchMaybeAndCheckBoxMaybeAndReset
    {
        ColorSwatchAndReset(com.donhatchsw.util.Listenable.Color color,
                            String name)
        {
            super(color, null, name);
        }
    }


    /**
    * Creates a control panel for the given MC4DViewGuts.
    * <pre>
        +---------------------------------------------+
        |Behavior                                     |
        | <-> Twist speed                             |
        | <-> Bounce                                  |
        | [ ] Require Ctrl to 3d Rotate               |
        | [ ] Restrict roll                           |
        | [ ] Stop between moves                      |
        +---------------------------------------------+
        |Appearance                                   |
        | <-> 4d Face Shrink                          |
        | <-> 4d Sticker Shrink                       |
        | <-> 4d Eye Distance                         |
        | <-> 3d Face Shrink                          |
        | <-> 3d Sticker Shrink                       |
        | <-> 3d Eye Distance                         |
        | [ ] Stickers shrink towards face boundaries |
        | [ ] Highlight by cubie                      |
        | [ ] Show shadows                            |
        | [ ] Antialias when still                    |
        | [] [ ] Draw non-shrunk face outlines        |
        | [] [ ] Draw shrunk face outlines            |
        | [] [ ] Draw non-shrunk sticker outlines     |
        | [] [ ] Draw shrunk sticker outlines         |
        | [] [ ] Draw Ground                          |
        | [] Background color                         |
        | Sticker colors                              |
        +---------------------------------------------+
     </pre>
    */
    public MC4DControlPanel(Stuff view)
    {
        this.setLayout(new java.awt.GridBagLayout());
        this.add(new Col(new Object[][]{
            {new Col("Behavior"," ", new Object[][]{
                {new MyPanel(new Object[][][] {
                    {{"Twist Speed:"},
                     {new TextAndSliderAndReset(view.twistSpeed),stretchx},
                     {new Button("Help")}},
                    {{"Bounce:"},
                     {new TextAndSliderAndReset(view.bounce),stretchx},
                     {new Button("Help")}},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.stopBetweenMoves, "Stop Between Moves"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.requireCtrlTo3dRotate, "Require Ctrl to 3d Rotate"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.restrictRoll, "Restrict Roll"),stretchx},
                    {new Button("Help")},
                }),stretchx},
             },stretchx),stretchx},
            {new Canvas() {{setBackground(java.awt.Color.black); setSize(1,1);}}, stretchx}, // Totally lame separator
            {new Col("Appearance"," ", new Object[][]{
                {new MyPanel(new Object[][][] {
                    {{"4d Face Shrink:"},
                     {new TextAndSliderAndReset(view.faceShrink4d),stretchx},
                     {new Button("Help")}},
                    {{"4d Sticker Shrink:"},
                     {new TextAndSliderAndReset(view.stickerShrink4d),stretchx},
                     {new Button("Help")}},
                    {{"4d Eye Distance:"},
                     {new TextAndSliderAndReset(view.eyeW),stretchx},
                     {new Button("Help")}},
                    {{"3d Face Shrink:"},
                     {new TextAndSliderAndReset(view.faceShrink3d),stretchx},
                     {new Button("Help")}},
                    {{"3d Sticker Shrink:"},
                     {new TextAndSliderAndReset(view.stickerShrink3d),stretchx},
                     {new Button("Help")}},
                    {{"3d Eye Distance:"},
                     {new TextAndSliderAndReset(view.eyeZ),stretchx},
                     {new Button("Help")}},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.stickersShrinkTowardsFaceBoundaries, "Stickers shrink towards face boundaries"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.highlightByCubie, "Highlight by cubie"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.showShadows, "Show shadows"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new CheckboxAndReset(view.antialiasWhenStill, "Antialias when still"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchMaybeAndCheckBoxMaybeAndReset(view.nonShrunkFaceOutlineColor, view.drawNonShrunkFaceOutlines, "Draw non-shrunk face outlines"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchMaybeAndCheckBoxMaybeAndReset(view.shrunkFaceOutlineColor, view.drawShrunkFaceOutlines, "Draw shrunk face outlines"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchMaybeAndCheckBoxMaybeAndReset(view.nonShrunkStickerOutlineColor, view.drawNonShrunkStickerOutlines, "Draw non-shrunk sticker outlines"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchMaybeAndCheckBoxMaybeAndReset(view.shrunkStickerOutlineColor, view.drawShrunkStickerOutlines, "Draw shrunk sticker outlines"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchMaybeAndCheckBoxMaybeAndReset(view.groundColor, view.drawGround, "Draw ground"),stretchx},
                    {new Button("Help")},
                }),stretchx},
                {new Row(new Object[][]{
                    {new ColorSwatchAndReset(view.backgroundColor, "Background color"),stretchx},
                    {new Button("Help")},
                }),stretchx},
            },stretchx),stretchx},
            {new Canvas() {{setBackground(java.awt.Color.black); setSize(1,1);}}, stretchx}, // totally lame separator
            {new Row(new Object[][]{{"",stretchx},{new Button("Reset All To Defaults")},{"",stretchx}}),stretchx}, // XXX weird way of centering, see if there's something cleaner
        }),stretchx);
    } // MC4DControlPanel ctor

    public static class Stuff
    {
        com.donhatchsw.util.Listenable.Float twistSpeed = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float bounce = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float faceShrink4d = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float stickerShrink4d = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float eyeW = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float faceShrink3d = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float stickerShrink3d = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Float eyeZ = new com.donhatchsw.util.Listenable.Float(.5f);
        com.donhatchsw.util.Listenable.Boolean requireCtrlTo3dRotate = new com.donhatchsw.util.Listenable.Boolean(false);
        com.donhatchsw.util.Listenable.Boolean restrictRoll = new com.donhatchsw.util.Listenable.Boolean(false);
        com.donhatchsw.util.Listenable.Boolean stopBetweenMoves = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean stickersShrinkTowardsFaceBoundaries = new com.donhatchsw.util.Listenable.Boolean(true);
        com.donhatchsw.util.Listenable.Boolean highlightByCubie = new com.donhatchsw.util.Listenable.Boolean(true);
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
        for (int i = 0; i < 2; ++i)
        {
            Frame frame = new Frame("MC4DControlPanel Test");
            frame.add(new MC4DControlPanel(stuff));
            frame.pack();
            frame.show();
        }
    }
} // MC4DControlPanel
