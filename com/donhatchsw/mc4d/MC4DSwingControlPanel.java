// TODO: help windows are way too wide, wtf?  even worse than legacy
package com.donhatchsw.mc4d;

import java.awt.event.*;
import javax.swing.*;
import com.donhatchsw.awt.JRow;
import com.donhatchsw.awt.JCol;
import com.donhatchsw.util.Listenable;

@SuppressWarnings("serial")
public class MC4DSwingControlPanel
    extends JPanel
    implements MC4DControlPanelInterface
{
    static private void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }


    // We want to make the font plain, on:
    //   - JLabels (except the BigBoldJLabels)
    //   - JCheckBoxes
    //   - JButtons
    // Empirically, we can do that by just calling setFont() on everything;
    // that will be ignored by BigBoldJLabel since that has its own override of getFont()
    // CBB: that's weird behavior to be taking advantage of
    private static void SetFontAll(java.awt.Component c, java.awt.Font font) {
        c.setFont(font);
        if (c instanceof java.awt.Container) {
            java.awt.Container C = (java.awt.Container)c;
	    int n = C.getComponentCount();
	    for (int i = 0; i < n; ++i)
	    {
		SetFontAll(C.getComponent(i), font);
	    }
        }
    }

    // a label in the default font, except bold (which is the default for JLabel anyway) and one point size larger.
    @SuppressWarnings("serial")
    private static class BigBoldJLabel extends JLabel
    {
        public BigBoldJLabel(String labelString)
        {
            super(labelString);
        }
        // Empirically, overriding this takes precedence over anything set by setFont().
        @Override public java.awt.Font getFont()
        {
            return new java.awt.Font("Dialog", java.awt.Font.BOLD, 13);
        }
    }

    @SuppressWarnings("serial")
    private static class CanvasOfSize extends JPanel  // dog science: if I derive from JComponent, background doesn't get drawn (and setOpaque(true) doesn't help).  deriving from JPanel instead makes it work, don't know why
    {
        private java.awt.Dimension preferredSize;
        public CanvasOfSize(int width, int height)
        {
            super();
            preferredSize = new java.awt.Dimension(width, height);
        }
        @Override public java.awt.Dimension getPreferredSize()
        {
            return preferredSize;
        }
    }

    // Stolen from ShepardsPlayApplet
    // A textfield that turns green or red when editing and not yet committed.
    // Esc reverts.
    @SuppressWarnings("serial")
    private static class JValidatingTextField extends JTextField
    {
	// subclass can override this
	public boolean validate(String text)
	{
	    return true; // it's all good by default
	}

	private String committedText;
	public JValidatingTextField(String text)
	{
	    super(text);
	    committedText = getText();

	    getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
		private void anyUpdate(javax.swing.event.DocumentEvent e)
		{
		    if (!getText().equals(committedText)) // TODO: == doesn't work here?  I forget the difference
		    {
			if (validate(getText()))
			{
			    // TODO: if value semantically equals committed value, turn white instead.  hmm, can build this into validate() by having it return a value (Object), maybe?
			    setBackground(new java.awt.Color(192,255,192)); // light green
			}
			else
			    setBackground(new java.awt.Color(255,192,192)); // pink
		    }
		    else
			setBackground(java.awt.Color.WHITE);
		}
		@Override public void insertUpdate(javax.swing.event.DocumentEvent e)  {anyUpdate(e);}
		@Override public void removeUpdate(javax.swing.event.DocumentEvent e)  {anyUpdate(e);}
		@Override public void changedUpdate(javax.swing.event.DocumentEvent e) {anyUpdate(e);}
	    });
	    addKeyListener(new java.awt.event.KeyAdapter() {
		@Override public void keyPressed(java.awt.event.KeyEvent e)
		{
		    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)
		    {
			setText(committedText);
		    }
		}
	    });
	    addActionListener(new java.awt.event.ActionListener() {
		@Override public void actionPerformed(java.awt.event.ActionEvent e)
		{
		    if (validate(getText()))
		    {
			committedText = getText();
			setBackground(java.awt.Color.WHITE);
		    }
		}
	    });
	}
	@Override public void setText(String text)
	{
	    super.setText(text);
	    if (validate(getText()))
	    {
		committedText = getText();
		setBackground(java.awt.Color.WHITE);
	    }
	}
    } // JValidatingTextField

    // Stolen from ShephardsPlayApplet (which claims it stole it from MC4DControlPanel.java,
    // but then must have made improvements)
    @SuppressWarnings("serial")
    public static class JTextFieldForNumber extends JValidatingTextField
    {
        private Listenable.Listener listener; // need to keep a strong ref to it for as long as I'm alive
        private Listenable.Number f;

        @Override public boolean validate(String text)
        {
            try {
                // attempt to parse, ignore return value
                if (f instanceof Listenable.Int)
                    Integer.parseInt(text);
                else if (f instanceof Listenable.Long)
                    Long.parseLong(text);
                else
                    Double.parseDouble(text);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private void updateText(Listenable.Number f)
        {
            if (f instanceof Listenable.Int)
                setText(""+((Listenable.Int)f).get());
            else if (f instanceof Listenable.Long)
                setText(""+((Listenable.Long)f).get());
            else if (f instanceof Listenable.Float)
                setText(""+((Listenable.Float)f).get());
            else
                setText(""+(float)((Listenable.Double)f).get()); // XXX ARGH! we lose precision with this (float) cast, but if we don't do it, we can get, for example, 37.092999999999996 which looks lame.  should figure out another way to prevent that.
        }

        public JTextFieldForNumber(final Listenable.Number f)
        {
            super(f instanceof Listenable.Int ? ""+((Listenable.Int)f).max() // XXX not quite right, min may be longer string than max
                : f instanceof Listenable.Long ? ""+((Listenable.Long)f).max()
                : "99.99"); // give it enough space for 99.999 (on my computer, always seems to give an extra space, which we don't need)
            this.f = f;
            updateText(f);
            f.addListener(listener = new Listenable.Listener() {
                @Override public void valueChanged()
                {
                    updateText(f);
                }
            });
            addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e)
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
        @Override public java.awt.Dimension getPreferredSize()
        {
            //System.out.println("in JTextFieldForNumber.getPreferredSize()");
            // default seems taller than necessary
            // on my computer... and in recent VMs it's even worse
            // (changed from 29 to 31).
            // Fudge it a bit...
            // XXX not sure this will look good on all systems... if it doesn't, we can just remove it
            // XXX hmm, actually makes things mess up when growing and shrinking, that's weird
            java.awt.Dimension preferredSize = super.getPreferredSize();
            //System.out.println("textfield.super.preferredSize() = "+preferredSize);
            if (true)
                preferredSize.height -= 2;

            // XXX another hack, wtf?
            if (true)
            {
                if (f instanceof Listenable.Int)
                    preferredSize.width = 50;
                else if (f instanceof Listenable.Long)
                    preferredSize.width = 100;
                else
                    preferredSize.width = 50;  // seems to be what I want for this particular app
            }

            //System.out.println("out JTextFieldForNumber.getPreferredSize(), returning "+preferredSize);
            return preferredSize;
        }
        // weird, the following is called during horizontal shrinking
        // but not during horizontal expanding... if we don't do this too
        // then it looks wrong when shrinking.  what a hack...
        @Override public java.awt.Dimension getMinimumSize()
        {
            //System.out.println("in JTextFieldForNumber.getMinimumSize()");
            java.awt.Dimension minimumSize = super.getMinimumSize();
            //System.out.println("textfield.super.minimumSize() = "+minimumSize);
            if (true)
                minimumSize.height -= 2;

            // XXX another hack, wtf?
            if (true)
            {
                if (f instanceof Listenable.Int)
                    minimumSize.width = 50;
                else if (f instanceof Listenable.Long)
                    minimumSize.width = 100;
                else
                    minimumSize.width = 50;  // seems to be what I want for this particular app
            }

            //System.out.println("out JTextFieldForNumber.getMinimumSize(), returning "+minimumSize);
            return minimumSize;
        }
    } // JTextFieldForNumber

    // stolen from ShephardsPlayApplet.
    @SuppressWarnings("serial")
    public static class JSliderForFloat extends JSlider
    {
        private static int verboseLevel = 0;

        private Listenable.Listener listener; // need to keep a strong ref to it for as long as I'm alive

        // private helper function
        private void updateThumb(Listenable.Number f)
        {
            if (verboseLevel >= 1) System.out.println("    in updateThumb (just a private helper function)");
            if (verboseLevel >= 1) System.out.println("      The number:");
            if (verboseLevel >= 1) System.out.println("          f.minDouble() = "+f.minDouble());
            if (verboseLevel >= 1) System.out.println("          f.maxDouble() = "+f.maxDouble());
            if (verboseLevel >= 1) System.out.println("          f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
            if (verboseLevel >= 1) System.out.println("      The slider:");
            if (verboseLevel >= 1) System.out.println("          min = "+getMinimum());
            if (verboseLevel >= 1) System.out.println("          max = "+getMaximum());
            if (verboseLevel >= 1) System.out.println("          max-min = "+(getMaximum()-getMinimum()));
            if (verboseLevel >= 1) System.out.println("          val = "+getValue()+" (corresponds to number value "+relerp(getValue(), getMinimum(), getMaximum(), f.minDouble(), f.maxDouble())+")");

            int intendedValue = (int)Math.round(relerp(f.getDouble(), f.minDouble(),f.maxDouble(), getMinimum(),getMaximum()));
            if (verboseLevel >= 1) System.out.println("      calling setValue("+intendedValue+") on the slider");
            setValue(intendedValue);
            if (verboseLevel >= 1) System.out.println("      returned from setValue("+intendedValue+") on the slider");

            if (verboseLevel >= 1) System.out.println("    out updateThumb (just a private helper function)");
        }

        public JSliderForFloat(final Listenable.Number f)
        {
            super(JSlider.HORIZONTAL, /*min=*/0, /*max=*/100, /*value=*/0);
            // Reverse engineer to try to make a change of 1 unit in the integer slider value
            // corresponds to .001 unit in the number value, as long as maxDouble is <= 1.
            // Annoyance: the default 4d eye distance is .867, which we'd like to be honored.
            // And its max is 4.  So, tailor to that.  (I'm not proud of this)
            double desired_increment = f.maxDouble() > 100. ? .1
                                     : f.maxDouble() > 10. ? .01
                                     : .001;
            int max = (int)Math.round((f.maxDouble()-f.minDouble()) / desired_increment);
            setMaximum(max);

            // I can't think of a good general principle,
            // so I just hand-picked the following ticks based on what looks nice for the ranges we use.
            if (f.minDouble() == 0. && f.maxDouble() == 1.) {
                CHECK(getMaximum() == 1000);
                setMinorTickSpacing(100);  // at .1, .2, ...
                setMajorTickSpacing(500); // at 0, .5, 1
                setPaintTicks(true);
                //setPaintLabels(true);  // can't do this unless I give it custom labels
            }
            if (f.minDouble() == 0. && f.maxDouble() == 4.) {
                CHECK(getMaximum() == 4000);
                setMinorTickSpacing(100);  // at .1, .2, ...
                setMajorTickSpacing(1000);  // at 0,1,2,3,4
                setPaintTicks(true);
                //setPaintLabels(true);  // can't do this unless I give it custom labels
            }
            if (f.minDouble() == 0. && f.maxDouble() == 20.) {
                CHECK(getMaximum() == 2000);
                setMinorTickSpacing(100);   // at 1,2,3,...
                setMajorTickSpacing(1000);  // at 0,10,20
                setPaintTicks(true);
                //setPaintLabels(true);  // can't do this unless I give it custom labels
            }
            if (f.minDouble() == 0. && f.maxDouble() == 300.) {
                CHECK(getMaximum() == 3000);
                setMinorTickSpacing(100);   // at 1,2,...
                setMajorTickSpacing(1000);  // at 0,10,20,30
                setPaintTicks(true);
                //setPaintLabels(true);  // can't do this unless I give it custom labels
            }

            // TODO: try to make it so clicking on a tick will snap to it?  That would be somewhat intuitive


            boolean XXXdebug = (""+f.getDouble()).equals("0.8669999837875366");  // 4d eye, the one that I'm struggling with
            if (verboseLevel >= 1 && XXXdebug) System.out.println("=====================================================");
            if (verboseLevel >= 1) System.out.println("in JSliderForFloat ctor");
            if (verboseLevel >= 1) System.out.println("  The number:");
            if (verboseLevel >= 1) System.out.println("      f.minDouble() = "+f.minDouble());
            if (verboseLevel >= 1) System.out.println("      f.maxDouble() = "+f.maxDouble());
            if (verboseLevel >= 1) System.out.println("      f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
            if (verboseLevel >= 1) System.out.println("  The slider:");
            if (verboseLevel >= 1) System.out.println("      min = "+getMinimum());
            if (verboseLevel >= 1) System.out.println("      max = "+getMaximum());
            if (verboseLevel >= 1) System.out.println("      max-min = "+(getMaximum()-getMinimum()));
            if (verboseLevel >= 1) System.out.println("      val = "+getValue()+" (corresponds to number value "+relerp(getValue(), getMinimum(), getMaximum(), f.minDouble(), f.maxDouble())+")");
            if (verboseLevel >= 1) System.out.println("      extent = "+getExtent());

            /*
            // this may or may not be the amount by which things change when clicked
            setExtent((getMaximum()-getMinimum())/10);  // .01 units, if numeric range is 0..1
            if (verboseLevel >= 1) System.out.println("      extent = "+getExtent());
            setExtent(getMaximum());
            if (verboseLevel >= 1) System.out.println("      extent = "+getExtent());
            */

            f.addListener(listener = new Listenable.Listener() {
                @Override public void valueChanged()
                {
                    if (verboseLevel >= 1) System.out.println("in JSliderForFloat valueChanged (i.e. someone changed the number)");
                    if (verboseLevel >= 1) System.out.println("  The number changed to:");
                    if (verboseLevel >= 1) System.out.println("      f.minDouble() = "+f.minDouble());
                    if (verboseLevel >= 1) System.out.println("      f.maxDouble() = "+f.maxDouble());
                    if (verboseLevel >= 1) System.out.println("      f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
                    if (verboseLevel >= 1) System.out.println("  The slider was:");
                    if (verboseLevel >= 1) System.out.println("      min = "+getMinimum());
                    if (verboseLevel >= 1) System.out.println("      max = "+getMaximum());
                    if (verboseLevel >= 1) System.out.println("      max-min = "+(getMaximum()-getMinimum()));
                    if (verboseLevel >= 1) System.out.println("      val = "+getValue()+" (corresponds to number value "+relerp(getValue(), getMinimum(), getMaximum(), f.minDouble(), f.maxDouble())+")");
                    if (verboseLevel >= 1) System.out.println("  f.getDouble() = "+f.getDouble());
                    if (verboseLevel >= 1) System.out.println("  calling updateThumb(f)");
                    updateThumb(f);
                    if (verboseLevel >= 1) System.out.println("  returned from updateThumb(f)");
                    if (verboseLevel >= 1) System.out.println("  f.getDouble() = "+f.getDouble());
                    if (verboseLevel >= 1) System.out.println("out JSliderForFloat valueChanged (i.e. someone changed the number)");
                }
            });
            addChangeListener(new javax.swing.event.ChangeListener() {
                @Override public void stateChanged(javax.swing.event.ChangeEvent e)
                {
                    if (verboseLevel >= 1) System.out.println("            in JSliderForFloat stateChanged (i.e. someone changed the slider, either from setValue() or dragging or clicking it)");
                    if (verboseLevel >= 1) System.out.println("              e = "+e);
                    if (verboseLevel >= 1) System.out.println("              getValueIsAdjusting() = "+getValueIsAdjusting());
                    if (verboseLevel >= 1) System.out.println("              The slider changed to:");
                    if (verboseLevel >= 1) System.out.println("                  min = "+getMinimum());
                    if (verboseLevel >= 1) System.out.println("                  max = "+getMaximum());
                    if (verboseLevel >= 1) System.out.println("                  max-min = "+(getMaximum()-getMinimum()));
                    if (verboseLevel >= 1) System.out.println("                  val = "+getValue()+" (corresponds to number value "+relerp(getValue(), getMinimum(), getMaximum(), f.minDouble(), f.maxDouble())+")");
                    if (verboseLevel >= 1) System.out.println("              The number, before we now mess with it, is:");
                    if (verboseLevel >= 1) System.out.println("                  f.minDouble() = "+f.minDouble());
                    if (verboseLevel >= 1) System.out.println("                  f.maxDouble() = "+f.maxDouble());
                    if (verboseLevel >= 1) System.out.println("                  f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");

                    if (verboseLevel >= 3)
                    {
                        // incidental stuff about the slider, hopefully not relevant to the logic
                        System.out.println("              getMinorTickSpacing() = "+getMinorTickSpacing());
                        System.out.println("              getMajorTickSpacing() = "+getMajorTickSpacing());
                        System.out.println("              getSize() = "+getSize());
                        System.out.println("              getPreferredSize() = "+getPreferredSize());
                    }

                    double intendedValue = relerp(getValue(), getMinimum(),getMaximum(), f.minDouble(),f.maxDouble());
                    if (verboseLevel >= 1) System.out.println("              intendedValue = "+intendedValue);
                    if (false)   // hmm, maybe not needed after all now that I am micromanaging the slider min and max
                    {
                        // Try to make it look nice: round to thousandths.  Hope this does not result in a feedback loop (this kind of stuff *should* reach a fixed point).
                        intendedValue = Math.round(intendedValue*1000.)/1000.;
                    }
                    if (verboseLevel >= 1) System.out.println("              intendedValue = "+intendedValue);
                    if (verboseLevel >= 1) System.out.println("              f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
                    if (verboseLevel >= 1) System.out.println("              calling f.setDouble("+intendedValue+")");
                    f.setDouble(intendedValue);
                    // will trigger valueChanged()
                    // which will call updateThumb()
                    // which may call us recursively, whee!
                    if (verboseLevel >= 1) System.out.println("              returned from f.setDouble("+intendedValue+")");
                    if (verboseLevel >= 1) System.out.println("              f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
                    if (verboseLevel >= 1) System.out.println("            out JSliderForFloat stateChanged (i.e. someone changed the slider, either from setValue() or dragging or clicking it)");
                }
            });

            // still in ctor here
            if (verboseLevel >= 1) System.out.println("  f.getDouble() = "+f.getDouble());
            if (verboseLevel >= 1) System.out.println("  calling updateThumb(f)");
            updateThumb(f); // because we fucked it up earlier in this contructor?
            if (verboseLevel >= 1) System.out.println("  returned from updateThumb(f)");
            if (verboseLevel >= 1) System.out.println("  f.getDouble() = "+f.getDouble());
            if (verboseLevel >= 1) System.out.println("  The number:");
            if (verboseLevel >= 1) System.out.println("      f.minDouble() = "+f.minDouble());
            if (verboseLevel >= 1) System.out.println("      f.maxDouble() = "+f.maxDouble());
            if (verboseLevel >= 1) System.out.println("      f.getDouble() = "+f.getDouble()+" (corresponds to slider val "+relerp(f.getDouble(),f.minDouble(),f.maxDouble(),getMinimum(),getMaximum())+")");
            if (verboseLevel >= 1) System.out.println("  The slider:");
            if (verboseLevel >= 1) System.out.println("      min = "+getMinimum());
            if (verboseLevel >= 1) System.out.println("      max = "+getMaximum());
            if (verboseLevel >= 1) System.out.println("      max-min = "+(getMaximum()-getMinimum()));
            if (verboseLevel >= 1) System.out.println("      val = "+getValue()+" (corresponds to number value "+relerp(getValue(), getMinimum(), getMaximum(), f.minDouble(), f.maxDouble())+")");
            if (verboseLevel >= 1) System.out.println("out JSliderForFloat ctor");
            if (verboseLevel >= 1 && XXXdebug) System.out.println("=====================================================");
        }
        private double relerp(double a, double a0, double a1, double b0, double b1) {
            double frac = (a-a0)/(a1-a0);
            return (1.-frac)*b0 + frac*b1;
        }

    } // JSliderForFloat


    @SuppressWarnings("serial")
    private static class ColorSwatch extends CanvasOfSize
    {
        private Listenable.Listener listener; // need to keep a strong ref to it for as long as I'm alive

        ColorSwatch(final Listenable.Color color, int width, int height)
        {
            super(width, height);
            setBackground(color.get());
            addMouseListener(new MouseListener() {
                @Override public void mouseClicked(MouseEvent me)
                {
                    //System.out.println("mouseClicked");
                } // mouseClicked
                @Override public void mousePressed(MouseEvent me)
                {
                    //System.out.println("mousePressed");
                    color.set(new java.awt.Color((float)Math.random(), (float)Math.random(), (float)Math.random())); // poor man's color chooser
                } // mousePressed
                @Override public void mouseReleased(MouseEvent me)
                {
                    //System.out.println("mouseReleased");
                } // mouseReleased
                @Override public void mouseEntered(MouseEvent me)
                {
                    //System.out.println("mouseEntered");
                } // mouseEntered
                @Override public void mouseExited(MouseEvent me)
                {
                    //System.out.println("mouseExited");
                } // mouseExited
            }); // mouse listener
            color.addListener(listener = new Listenable.Listener() {
                @Override public void valueChanged()
                {
                    ColorSwatch.this.setBackground(color.get());
                }
            });
        }
    } // ColorSwatch

    @SuppressWarnings("serial")
    private static class ColorSwatchMaybeAndCheckBoxMaybe extends JRow
    {
        private Listenable.Listener listener; // need to keep a strong ref to it for as long as I'm alive

        private Listenable.Color color;
        private Listenable.Boolean b;
        private JComponent swatch;
        private JCheckBox checkbox;

        private void updateShownValues()
        {
            if (color != null)
                swatch.setBackground(color.get());
            if (b != null)
                checkbox.setSelected(b.get());
        }

        public ColorSwatchMaybeAndCheckBoxMaybe(
            final Listenable.Color initcolor,
            final Listenable.Boolean initb,
            String name)
        {
            if (initcolor != null)
                super.add(new ColorSwatch(initcolor,16,16));
            super.add(initb==null ? (JComponent)new JLabel(name) : (JComponent)new JCheckBox(name));
            super.add(new JLabel(""), new java.awt.GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;}}); // just stretchable space

            // awkward, but we can't set members
            // until the super ctor is done
            int i = 0;
            if (initcolor != null)
                this.swatch = (JComponent)this.getComponent(i++);
            if (initb != null)
                this.checkbox = (JCheckBox)this.getComponent(i++);
            this.color = initcolor;
            this.b = initb;

            if (b != null)
            {
                b.addListener(listener = new Listenable.Listener() {
                    @Override public void valueChanged()
                    {
                        updateShownValues();
                    }
                });
                checkbox.addItemListener(new ItemListener() {
                    @Override public void itemStateChanged(ItemEvent e)
                    {
                        //System.out.println("in checkbox callback");
                        b.set(e.getStateChange() == ItemEvent.SELECTED);
                        // will trigger b.valueChanged()
                        // which will call updateShownValues()
                    }
                });
            }
            if (this.checkbox != null
             && name.indexOf("(not yet implemented)") != -1)
            {
                this.checkbox.setEnabled(false);
            }
            updateShownValues();
        }
    } // ColorSwatchMaybeAndCheckBoxMaybe ctor

    // XXX think of a name
    @SuppressWarnings("serial")
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
    @SuppressWarnings("serial")
    private static class ResetButton extends JButton
    {
        private Listenable.Listener keepalive[]; // need to keep strong refs to them for as long as I'm alive
        private boolean wasDefault[]; // one for each listenable
        private int nNonDefault; // number of falses in wasDefault
        public ResetButton(final String buttonLabel,
                           final Listenable listenables[])
        {
            super(buttonLabel);
            // XXX to be clean, should really scrunch out null listeners here so that we don't suffer overhead for them every time the button is hit... not that anyone would ever notice though probably
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e)
                {
                    for (int i = 0; i < listenables.length; ++i)
                        if (listenables[i] != null)
                            listenables[i].resetToDefault();
                    //System.out.println("nNonDefault = "+nNonDefault);
                    CHECK(nNonDefault == 0); // due to our valueChanged getting called
                }
            });
            keepalive = new Listenable.Listener[listenables.length];
            wasDefault = new boolean[listenables.length];
            nNonDefault = 0;
            for (int _i = 0; _i < listenables.length; ++_i)
            {
                final int i = _i;
                if (listenables[i] == null)
                    continue;
                if (!(wasDefault[i] = listenables[i].isDefault()))
                    nNonDefault++;
                Listenable.Listener listener = new Listenable.Listener() {
                    @Override public void valueChanged()
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
                            CHECK(nNonDefault >= 0);
                        }
                        wasDefault[i] = isDefault;
                    }
                };
                listenables[i].addListener(listener);
                keepalive[i] = listener;
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

    // String.join doesn't exist until 1.8
    private static String String_join(String delimiter,
                                      CharSequence... charSequences)
    {
        StringBuilder sb = new StringBuilder();
        boolean didSomething = false;
        for (CharSequence charSequence : charSequences)
        {
            if (didSomething) sb.append(delimiter);
            sb.append(charSequence);
            didSomething = true;
        }
        return sb.toString();
    }

    @SuppressWarnings("serial")
    private static class HelpButton extends JButton
    {
        public HelpButton(final String helpWindowTitle,
                          final String helpMessage[])
        {
            super("Help");
            if (helpMessage != null)
            {
                addActionListener(new ActionListener() {
                    @Override public void actionPerformed(ActionEvent e)
                    {
                        JComponent panel;
                        {
                            int nRows = helpMessage.length;
                            int nCols = 0;
                            for (int i = 0; i < helpMessage.length; ++i)
                                nCols = Math.max(nCols, helpMessage[i].length());
                            panel = new JScrollPane(
                                new JTextArea(String_join("\n", helpMessage), nRows, nCols) {{ setEditable(false); }},
                                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                                // this is generally fine-- if too small horizontally, it wraps at words rather than making a horizontal scrollbar.
                                // Note that both ALWAYS is not well behaved on linux-- the window starts a bit not all enough.  TODO: check whether this is still true for swing; it was true for legacy
                        }

                        final JFrame helpWindow = new JFrame("MC4D Help: "+helpWindowTitle);
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
            }
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
        | <-> Twist duration                          [Reset][Help]|
        | <-> Bounce                                  [Reset][Help]|
        | [ ] Stop between moves                      [Reset][Help]|
        | [ ] Require Ctrl to 3d Rotate               [Reset][Help]|
        | [ ] Restrict roll                           [Reset][Help]|
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
    private void addSingleLabelRow(JLabel label)
    {
        // A label on a row by itself gets left justified
        this.add(label, new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                  anchor = WEST;}});
        nRows++;
    }
    private void addSingleButtonRow(JButton button)
    {
        // A button on a row by itself gets centered
        this.add(button, new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                   anchor = CENTER;}});
        nRows++;
    }
    private void addSingleComponentRow(JComponent component)
    {
        // Any other component on a row by itself gets stretched
        this.add(component, new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER;
                                                      fill = HORIZONTAL; weightx = 1.;}});
        nRows++;
    }
    private void addLabelAndResetButtonRow(String labelString,
                                           Listenable listenable,
                                           String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new JLabel(labelString),
                 new java.awt.GridBagConstraints(){{anchor = WEST;
                                           gridwidth = 3;
                                           gridy = nRows;}});
        this.add(new ResetButton("Reset to default", listenable),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void addFloatSliderRow(String labelString,
                        Listenable.Number f, // Float or Double
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new JLabel(labelString+":"),
                 new java.awt.GridBagConstraints(){{anchor = WEST;
                                           gridy = nRows;}});
        this.add(new JTextFieldForNumber(f),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new JSliderForFloat(f),
                 new java.awt.GridBagConstraints(){{gridy = nRows;
                                           fill = HORIZONTAL; weightx = 1.;}});
        this.add(new ResetButton("Reset to default", f),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void addCheckboxRow(String labelString,
                        Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new CheckboxThing(b, labelString),
                 new java.awt.GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", b),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void add3CheckboxesRow(final String labelString,
                                   final String labelString0,
                                   final String labelString1,
                                   final String labelString2,
                                   final Listenable.Int listenableInt,
                                   String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new JRow() {{
                     this.add(new JLabel(labelString));
                     String[] labels012 = {labelString0, labelString1, labelString2};
                     for (int i = 0; i < 3; ++i) {
                         final int final_i = i;;
                         this.add(new JRadioButton(labels012[i], listenableInt.get()==i) {
                             Listenable.Listener listener;  // needed to keep a strong ref to it for as long as I'm alive
                         {
                             // don't need a ButtonGroup-- the listeners (2 way binding) accomplish the mutual exclusion!
                             addActionListener(new java.awt.event.ActionListener() {
                                 @Override public void actionPerformed(java.awt.event.ActionEvent e)
                                 {
                                     listenableInt.set(final_i);
                                 }
                             });
                             listenableInt.addListener(listener = new Listenable.Listener() {
                                 @Override public void valueChanged() {
                                     setSelected(listenableInt.get() == final_i);
                                 }
                             });
                         }});
                     }
                 }}, new java.awt.GridBagConstraints(){{anchor = WEST;
                                                        gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", listenableInt),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }
    private void addColorSwatchAndCheckboxRow(String labelString,
                        Listenable.Color color,
                        Listenable.Boolean b,
                        String helpMessage[])
    {
        this.add(new CanvasOfSize(20,10), // indent
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        this.add(new ColorSwatchMaybeAndCheckBoxMaybe(color, b, labelString),
                 new java.awt.GridBagConstraints(){{fill = HORIZONTAL; weightx = 1.;
                                           gridwidth = 3; gridy = nRows;}});
        this.add(new ResetButton("Reset to default", new Listenable[]{color, b}),
                 new java.awt.GridBagConstraints(){{gridy = nRows;}});
        if (helpMessage != null)
            this.add(new HelpButton(labelString, helpMessage),
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
        nRows++;
    }

    // XXX should this name be associated with the viewParams instead?
    private String name;
    @Override public String getName()
    {
        return name;
    }
    private MC4DViewGuts.ViewParams viewParams;
    @Override public MC4DViewGuts.ViewParams getViewParams()
    {
        return viewParams;
    }

    public MC4DSwingControlPanel(String name,
                                  final MC4DViewGuts.ViewParams viewParams,
                                  final MC4DViewGuts.ViewState viewState) // for "Frame Picture", kind of hacky, violates the idea that control panels are 1-to-1 with viewParams
    {
        this.name = name;
        this.viewParams = viewParams;

        this.setLayout(new java.awt.GridBagLayout());
        addSingleLabelRow(new BigBoldJLabel("Behavior"));
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
            "Stop Between Moves (not yet implemented)",
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
                 "un-ctrled mouse actions can both",
                 "start/stop the 3d rotation and do twists",
                 "(or 4d-rotate-to-center using middle mouse)",
                 "at the same time,",
                 "and ctrl-mouse actions will do twists without affecting",
                 "the 3d rotation.",
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
        addCheckboxRow(
            "Futt",
            viewParams.futtIfPossible,
            new String[] {
                 "Futt (fudge) when possible.",
                 "This means allow twists that are topologically possible",
                 "even if it requires morphing geometry.",
                 "See Oskar's Futtminx video:",
                 "    https://www.youtube.com/watch?v=9rbs5xxHdRg for more info.",
                 "",
                 "NOTE: Currently this is implemented in only a subset of puzzles:",
                 "3d trivalent puzzles and 4d tetravalent puzzles,",
                 "and, furthermore, cuts have to be shallow enough so that",
                 "cut sets of non-incident faces don't interact with each other.",
                 "",
                 "  Examples of 3d trivalent puzzles:",
                 "      - prisms (Futt allows turning the squares 90 degrees,",
                 "        which was previously impossible)",
                 "      - truncated icosahedron, which is the Futtminx",
                 "        (Futt allows turning the hexagons 60 degrees)",
                 "      - any truncated regular",
                 "        (Futt allows twisting the 2p-gons to all 2p positions instead of only p of them)",
                 "      - any omnitruncated regular",
                 "      - frucht and not-frucht",
                 "",
                 "  Examples of 4d tetravalent puzzles:",
                 "      - duoprisms {p}x{4} or {4}x{p}",
                 "        (Futt allows the cubes to be twisted more freely than previously)",
                 "          e.g. \"{3}x{4} 3\" (triangular-prism prism)",
                 "          e.g. \"{5}x{4} 3\" (pentagonal-prism prism)",
                 "      - any (truncated 3d regular) x (1d) hyperprism",
                 "        (Futt allows twisting the 2p-gonal prisms to all 2p positions instead of only p of them)",
                 "          e.g. \"(1)3(1)3(0)x{} 3(4)\" (truncated-tetrahedron prism)",
                 "          e.g. \"(1)4(1)3(0)x{} 3(4)\" (truncated-cube prism)",
                 "          e.g. \"(1)3(1)4(0)x{} 3\" (truncated-octahedron prism)",
                 "          e.g. \"(1)5(1)3(0)x{} 3(4)\" (truncated-dodecahedron prism)",
                 "          e.g. \"(1)3(1)5(0)x{} 3\" truncated-icosahedron prism)",
                 "      - any (omnitruncated 3d regular) x (1d) hyperprism",
                 "        (Futt allows twisting the 2p-gonal prisms to all 2p positions instead of only p of them)",
                 "          e.g. \"(1)3(1)3(1)x{} 3\"  (omnitruncated tetrahedron prism) (same as truncated-octahedron prism)",
                 "          e.g. \"(1)4(1)3(1)x{} 3\"  (omnitruncated cube/octahedron prism)",
                 "          e.g. \"(1)5(1)3(1)x{} 3\"  (omnitruncated icosa/dodecahedron prism)",
                 "      - any omnitruncated 4d regular",
                 "          e.g. \"(1)3(1)3(1)3(1) 3\"  (omnitruncated simplex)",
                 "          e.g. \"(1)4(1)3(1)3(1) 3\"  (omnitruncated hypercube)",
                 "          e.g. \"(1)3(1)4(1)3(1) 3\"  (omnitruncated 24-cell, good luck with that)",
                 "          e.g. \"(1)5(1)3(1)3(1) 3\"  (omnitruncated 120-cell, good luck with that)",
                 "      - (truncated regular {p,q,3}'s are tetravalent, but I don't think Futt provides any extra moves)",
                 "          e.g. \"(1)3(1)3(0)3(0) 3(5)\"  (truncated simplex) (no extra moves, I don't think)",
                 "          e.g. \"(1)4(1)3(0)3(0) 3(5)\"  (truncated hypercube) (no extra moves, I don't think)",
                 "          e.g. \"(1)3(1)4(0)3(0) 3\"  (truncated 24-cell) (no extra moves, I don't think)",
                 "          e.g. \"(1)5(1)3(0)3(0) 3(5)\"  (truncated 120-cell, good luck with that)",
                 "      - (bitruncateds are tetravalent too, but I don't think Futt provides any extra moves)",
                 "      - (runcinateds?)",
                 "      - (cantitruncateds?)",
                 "      - (runcitruncateds?)",
                 "      - frucht or not-frucht prism",
                 "          e.g. \"frucht*{} 3(4)\"  (frucht prism)",
                 "          e.g. \"notfrucht*{} 3(4)\"  (not-frucht prism)",
                 "",
            });
        addSingleComponentRow(new CanvasOfSize(1,1){{setBackground(java.awt.Color.black);}}); // Totally lame separator
        addSingleLabelRow(new BigBoldJLabel("Appearance"));
        addFloatSliderRow(
            "4d Face Shrink",
            viewParams.faceShrink4d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 4d",
                "(before the 4d->3d projection).",
                "",
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
                "",
                "Shrinking before the projection causes the apparent final 3d shape",
                "of the sticker to become less distorted (more cube-like),",
                "but more poorly fitting with its 3d neighbors.",
            });
        addLabelAndResetButtonRow(
            "4d Rotation",
            viewParams.viewMat4d,
            new String[] {
                "Middle-click (or Alt-click) on a hyperface to rotate that hyperface to the center.",
                "",
                "You can rotate an arbitrary element (hyperface, 2d face, edge, or vertex)",
                "to the center, by holding down the Ctrl key while middle-clicking (or Alt-clicking).",
            });
        addFloatSliderRow(
            "4d Eye Distance",
            viewParams.eyeW,
            new String[] {
                "Specifies the distance from the eye to the center of the puzzle in 4d,",
                "where 1 is the circumradius of the puzzle (with 4d shrinks applied).",
                "",
                "Considerations/tradeoffs:",
                "    - If you make the value too large, then fewer faces will be shown",
                "      in particular, it will make you unable to see all faces simultaneously",
                "      in some of the vertex-first, edge-first, and 2d-polygon-first views",
                "      of the standard puzzle.",
                "    - If you make the value too small, the smart depth sort of stickers will fail,",
                "      and the rendering will fall back to a lower-quality z-sort.  See the indicator",
                "      in the lower right of the picture, which should normally say \"no cycles\".",
                "    - If you make the value less than 1, expect bad rendering,",
                "      since part of the puzzle will be behind the eye,",
                "      which the rendering engine can't handle.",
            });
        addFloatSliderRow(
            "3d Face Shrink",
            viewParams.faceShrink3d,
            new String[] {
                "Specifies how much each face should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).",
                "",
                "Shrinking after the projection",
                "causes the face to retain its 3d shape as it shrinks.",
            });
        addFloatSliderRow(
            "3d Sticker Shrink",
            viewParams.stickerShrink3d,
            new String[] {
                "Specifies how much each sticker should be shrunk towards its center in 3d",
                "(after the 4d->3d projection).",
                "",
                "Shrinking after the projection",
                "causes the sticker to retain its 3d shape as it shrinks.",
            });
        addLabelAndResetButtonRow(
            "3d Rotation",
            viewParams.viewMat3d,
            new String[] {
                "Rotate the puzzle in 3d by dragging with the mouse.",
                "Let go while dragging to set the puzzle spinning.",
                "",
                "See also the \"Require Ctrl to 3d Rotate\" and \"Restrict Roll\" options,",
                "which modify the dragging behavior.",
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
                "Normally this option is set to 0, which causes stickers",
                "to shrink towards their centers.",
                "Setting it to 1 causes stickers to shrink towards",
                "the face boundaries instead (so if the 4d and 3d face shrinks are 1,",
                "this will cause all the stickers on a given cubie to be contiguous).",
                "Setting it to a value between 0 and 1 will result",
                "in shrinking towards some point in between.",
            });

        if (true)
        {
            add(new CanvasOfSize(20,10), // indent
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
            add(new JButton("Contiguous cubies") {
                    private Listenable.Listener listener; // need to keep a strong ref to the listener for as long as I'm alive
                    private void updateShownValue()
                    {
                        setEnabled(viewParams.faceShrink4d.get() != 1.f
                                || viewParams.faceShrink3d.get() != 1.f
                                || viewParams.stickersShrinkTowardsFaceBoundaries.get() != 1.f);
                    }
                    {
                        addActionListener(new ActionListener() {
                            @Override public void actionPerformed(ActionEvent e)
                            {
                                //System.out.println("Contiguous cubies button was bonked!");
                                viewParams.faceShrink4d.set(1.f);
                                viewParams.faceShrink3d.set(1.f);
                                viewParams.stickersShrinkTowardsFaceBoundaries.set(1.f);
                            }
                        });
                        listener = new Listenable.Listener() {
                            @Override public void valueChanged()
                            {
                                //System.out.println("One of the 3 float values changed");
                                updateShownValue();
                            }
                        };
                        viewParams.faceShrink4d.addListener(listener);
                        viewParams.faceShrink3d.addListener(listener);
                        viewParams.stickersShrinkTowardsFaceBoundaries.addListener(listener);
                        updateShownValue();
                    }
                },
                new java.awt.GridBagConstraints(){{gridy = nRows; anchor = WEST;}});
            super.add(new JLabel(""), new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = 3; fill = HORIZONTAL; weightx = 1.;}}); // just stretchable space
            add(new HelpButton("Contiguous cubies",
                               new String[] {
                                   "Pressing the Contiguous Cubies button",
                                   "is the same as setting 4d Face Shrink, 3d Face Shrink,",
                                   "and \"Stickers shrink to face boundaries\" all to 1.",
                                   "With these settings, all the stickers of a given cubie",
                                   "will appear to be contiguous",
                                   "",
                                   "This button is enabled only when not already contiguous.",
                                }),
                new java.awt.GridBagConstraints(){{gridy = nRows;}});
            nRows++;
        } // contiguous cubies button row

        if (false) // XXX just get rid of this, I think
        {
            add(new CanvasOfSize(20,10), // indent
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
            add(new JCheckBox("Contiguous cubies") {
                    private Listenable.Listener listener; // need to keep a strong ref to the listener for as long as I'm alive
                    private void updateShownValue()
                    {
                        setSelected(viewParams.faceShrink4d.get() == 1.f
                                 && viewParams.faceShrink3d.get() == 1.f
                                 && viewParams.stickersShrinkTowardsFaceBoundaries.get() == 1.f);
                    }
                    {
                        addItemListener(new ItemListener() {
                            @Override public void itemStateChanged(ItemEvent e)
                            {
                                if (e.getStateChange() == ItemEvent.SELECTED)
                                {
                                    viewParams.faceShrink4d.set(1.f);
                                    viewParams.faceShrink3d.set(1.f);
                                    viewParams.stickersShrinkTowardsFaceBoundaries.set(1.f);
                                }
                                else
                                {
                                    // If the Contiguous cubies is on
                                    // and the user turns it off again,
                                    // it turns back on because cubies
                                    // are still contiguous.
                                    // The is a bit obnoxious... maybe that's why
                                    // it should be a button instead of a checkbox
                                    updateShownValue();
                                }
                            }
                        });
                        listener = new Listenable.Listener() {
                            @Override public void valueChanged()
                            {
                                //System.out.println("One of the 3 float values changed");
                                updateShownValue();
                            }
                        };
                        viewParams.faceShrink4d.addListener(listener);
                        viewParams.faceShrink3d.addListener(listener);
                        viewParams.stickersShrinkTowardsFaceBoundaries.addListener(listener);
                        updateShownValue();
                    }
                },
                new java.awt.GridBagConstraints(){{gridy = nRows; anchor = WEST;}});
            super.add(new JLabel(""), new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = 3; fill = HORIZONTAL; weightx = 1.;}}); // just stretchable space
            add(new HelpButton("Contiguous cubies",
                               new String[] {
                                   "Checking the Contiguous Cubies checkbox",
                                   "is the same as setting 4d Face Shrink, 3d Face Shrink,",
                                   "and \"Stickers shrink to face boundaries\" all to 1.",
                                   "",
                                   "You can't uncheck it directly,",
                                   "but it will uncheck itself when you set",
                                   "any of those three parameters to a value",
                                   "other than 1.",
                                }),
                new java.awt.GridBagConstraints(){{gridy = nRows;}});
            nRows++;
        } // contiguous cubies checkbox row

        if (true)
        {
            add(new CanvasOfSize(20,10), // indent
                     new java.awt.GridBagConstraints(){{gridy = nRows;}});
            add(new JButton("Frame Picture") {{
                    addActionListener(new ActionListener() {
                        @Override public void actionPerformed(ActionEvent e)
                        {
                            float oldScale = viewParams.viewScale2d.get();
                            if (oldScale <= 0.f)
                            {
                                viewParams.viewScale2d.resetToDefault();
                                // they'll have to hit the button again
                                // after it paints with the sane value.
                                // serves them right.
                                return;
                            }
                            // Figure out a bounding box for what's painted
                            GenericPipelineUtils.Frame frame = viewState.untwistedFrame;
                            float verts[][] = frame.verts; // XXX does this maybe include extras?
                            if (verts == null)
                            {
                                viewParams.viewScale2d.resetToDefault();
                                return;
                            }
                            float bbox[/*2*/][] = com.donhatchsw.util.VecMath.bbox(verts);
                            System.out.println("nVerts = "+verts.length);
                            System.out.println("bbox = "+com.donhatchsw.util.VecMath.toString(bbox));
                            float windowWidth = 502.f; // XXX
                            float windowHeight = 485.f; // XXX
                            float oldPercentage = Math.max(
                                (bbox[1][0]-bbox[0][0])/windowWidth,
                                (bbox[1][1]-bbox[0][1])/windowHeight);
                            System.out.println("oldPercentage = "+oldPercentage);
                            if (oldPercentage == 0.f)
                            {
                                viewParams.viewScale2d.resetToDefault();
                                return;
                            }
                            float rescaleNeeded = .9f / oldPercentage;
                            // XXX I think there's a bug that makes the effect of scale get squared... so rescale it by only the square root of what we should REALLY rescale it by
                            rescaleNeeded = (float)Math.sqrt(rescaleNeeded);
                            float newScale = oldScale * rescaleNeeded;
                            viewParams.viewScale2d.set(newScale);
                        }
                    });
                }},
                new java.awt.GridBagConstraints(){{gridy = nRows; anchor = WEST;}});
            super.add(new JLabel(""), new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = 3; fill = HORIZONTAL; weightx = 1.;}}); // just stretchable space
            add(new HelpButton("Frame Picture",
                               new String[] {
                                   "Pressing the Frame Picture button",
                                   "changes the 2d scale if necessary",
                                   "so that the picture will take up",
                                   "90% of the viewing window in one",
                                   "of the two directions (width or height)",
                                   "and at most that in the other direction.",
                                }),
                new java.awt.GridBagConstraints(){{gridy = nRows;}});
            nRows++;
        } // frame picture button row



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
        add3CheckboxesRow(
            "Antialias (smooth edges): ",
            "never", "when still", "always",
            viewParams.antialias,
            new String[] {
                "Whether to anti-alias (smooth edges),",
                "if your computer's graphics hardware supports it.",
            });
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk face outlines (not yet implemented)",
            viewParams.nonShrunkFaceOutlineColor,
            viewParams.drawNonShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw shrunk face outlines (not yet implemented)",
            viewParams.shrunkFaceOutlineColor,
            viewParams.drawShrunkFaceOutlines,
            null); // no help string
        addColorSwatchAndCheckboxRow(
            "Draw non-shrunk sticker outlines (not yet implemented)",
            viewParams.nonShrunkStickerOutlineColor,
            viewParams.drawNonShrunkStickerOutlines,
            null); // no help string
        addCheckboxRow(
            "Draw shrunk sticker surfaces",
            viewParams.drawShrunkStickerSurfaces,
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
            add(new JLabel("Face Colors:"),
                new java.awt.GridBagConstraints(){{gridy = nRows; anchor = WEST; gridwidth = 4;}});
            add(new ResetButton("Reset to default", viewParams.faceColors),
                new java.awt.GridBagConstraints(){{gridy = nRows;}});
            nRows++;
        }
        {
            final int nFaces = 120; // XXX !?
            final int nFacesPerRow = 32;
            final int iFace[] = {0}; // really just an int, but need to be able to modify it in inner class
            final int indents[] = {0,8,4,12,2,10,6,14,1,9,5,13,3,11,7,15};
            final int swatchWidth = 16;
            final int swatchHeight = 16;
            for (int iRow = 0; iRow * nFacesPerRow < nFaces; ++iRow)
            {
                final int indent = indents[iRow%indents.length]*swatchWidth/indents.length;
                final int nFacesThisRow = Math.min(nFaces - iRow*nFacesPerRow, nFacesPerRow);

                this.add(new CanvasOfSize(20,swatchHeight), // overall additional indent
                         new java.awt.GridBagConstraints(){{gridy = nRows;}});
                JRow row = new JRow() {{

                    add(new CanvasOfSize(indent,swatchHeight));

                    for (int i = 0; i < nFacesThisRow; ++i)
                    {
                        Listenable.Color colorListenable = viewParams.faceColors[iFace[0]];
                        add(new ColorSwatch(colorListenable, swatchWidth,swatchHeight));
                        iFace[0]++;
                    }
                }};
                add(row,
                    new java.awt.GridBagConstraints(){{gridy = nRows; gridwidth = REMAINDER; anchor = WEST;}});
                nRows++;
            }
        }

        if (true) {
            addSingleComponentRow(new CanvasOfSize(1,1){{setBackground(java.awt.Color.black);}}); // Totally lame separator
            addSingleLabelRow(new BigBoldJLabel("Experimental"));
            addCheckboxRow(
                "Use newest depth sort algorithm (ctrl-alt-P to toggle viz)",
                viewParams.topsortUsesBoldNewWay,
                new String[] {
                    "If this option is checked,",
                    "the depth sorting willl use an experimental new bleeding edge algorithm.",
                });
        }

        addSingleComponentRow(new CanvasOfSize(1,1){{setBackground(java.awt.Color.black);}}); // Totally lame separator
        addSingleButtonRow(new ResetButton(
            "Reset All To Defaults",
            Listenable.allListenablesInObject(viewParams)));

        if (false)
            randomlyColorize(this);

        SetFontAll(this, new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
    } // MC4DSwingControlPanel ctor



    // TODO: publish this somewhere more legit
        // used by dumpComponentHierarchy
        private static String classNameAncestors(Class<?> classs)
        {
            String text = classs.getName();

            if (text.indexOf("java.lang.") == 0)
                text = text.substring(10);
            if (text.indexOf("java.awt.") == 0)
                text = text.substring(9);
            if (text.indexOf("javax.swing.J") == 0)
                text = text.substring(12);

            //if (text.indexOf('$') != -1)
            if (classs.getSuperclass() != null)
                text += " > " + classNameAncestors(classs.getSuperclass());
            return text;
        } // classNameAncestors
        // used by dumpComponentHierarchy
        private static String xcolorstring(java.awt.Color color) {
          if (color == null) return "null";
          int argb = color.getRGB();
          if (((argb >> 24)&0xff) == 0xff) {
            return String.format("#%06x", argb&0xffffff);
          } else {
            return String.format("#%08x", argb);
          }
        }
        // used by dumpComponentHierarchy
        private static String colored(java.awt.Color fg, java.awt.Color bg, String text) {
          if (fg == null || bg == null) return "";
          int fg_rgb = fg.getRGB();
          int bg_rgb = bg.getRGB();
          String answer = String.format("\033[38;2;%d;%d;%dm\033[48;2;%d;%d;%dm%s\033[0m",
                ((fg_rgb>>16)&0xff),
                ((fg_rgb>>8)&0xff),
                ((fg_rgb>>0)&0xff),
                ((bg_rgb>>16)&0xff),
                ((bg_rgb>>8)&0xff),
                ((bg_rgb>>0)&0xff),
                text);
          return answer;
        }
    public static void dumpComponentHierarchy(java.awt.Component component, int depth, int iChildInParent, int nChildrenInParent)
    {
	for (int i = 0; i < depth; ++i) System.out.print(" ");

	System.out.print(iChildInParent+"/"+nChildrenInParent+" ");
	//System.out.print(component+" ");  // interesting but too much
	System.out.print(classNameAncestors(component.getClass()));
	System.out.print("  (fg="+xcolorstring(component.getForeground())+" bg="+xcolorstring(component.getBackground())+")");
	System.out.print("  (name="+component.getName()+")");
	System.out.print("  (db="+component.isDoubleBuffered()+")");
	System.out.print(" (op="+component.isOpaque()+")");
	System.out.print("  "+colored(component.getForeground(), component.getBackground(), " HELLO "));
	System.out.println();

	if (component instanceof java.awt.Container)
	{
	    java.awt.Container C = (java.awt.Container)component;
	    if (true) {
	      for (int i = 0; i < depth; ++i) System.out.print(" ");
	      System.out.print("    "+C.getLayout()+(C.getLayout()==null?"":" ("+classNameAncestors(C.getLayout().getClass())+")"));
	      System.out.println();
	    }
	    int n = C.getComponentCount();
	    for (int iChild = 0; iChild < n; ++iChild)
	    {
		dumpComponentHierarchy(C.getComponent(iChild), depth+1, iChild, n);
	    }
	}
    } // dumpComponentHierarchy

    // for debugging XXX should probably be in com.donhatchsw.awt somewhere, the layout stuff has it too.  also the printComponent stuff, maybe
    public static void randomlyColorize(java.awt.Component c)
    {
        c.setBackground(new java.awt.Color((float)Math.random(),
                                           (float)Math.random(),
                                           (float)Math.random()));
        c.setForeground(new java.awt.Color((float)Math.random(),
                                           (float)Math.random(),
                                           (float)Math.random()));
        if (c instanceof java.awt.Container)
        {
            java.awt.Container C = (java.awt.Container)c;
            int n = C.getComponentCount();
            for (int i = 0; i < n; ++i)
                randomlyColorize(C.getComponent(i));
        }
    } // randomlyColorize




    /** A little test/example program. */
    public static void main(String args[])
    {
        // Only one set of params, share it among all the panels
        MC4DViewGuts.ViewParams viewParams = new MC4DViewGuts.ViewParams();
        MC4DViewGuts.ViewState viewState = new MC4DViewGuts.ViewState();
        for (int i = 0; i < 2; ++i)
        {
            final JFrame frame = new JFrame("MC4DSwingControlPanel Test");
            {
                com.donhatchsw.awt.MainWindowCount.increment();
                frame.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosing(WindowEvent we)
                    {
                        frame.dispose();
                    }
                    @Override public void windowClosed(WindowEvent we)
                    {
                        if (com.donhatchsw.awt.MainWindowCount.howMany() == 1)
                            System.out.println("Ciao!!");
                        else
                            System.out.println("ciao!");
                        com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();
                    }
                });
            }

            frame.add(new MC4DSwingControlPanel("Settings", viewParams, viewState));
            frame.pack();
            frame.setVisible(true);  // available in java 1.5, replaces deprecated show()
        }
        // release the main token
        com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();
    } // main

} // MC4DSwingControlPanel
