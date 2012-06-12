/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmcs.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A text field for search/filter interfaces. The extra functionality includes
 * a placeholder string (when the user hasn't yet typed anything), and a button
 * to clear the currently-entered text.
 *
 * @origin Elliott Hughes
 *
 * @todo : add a menu of recent searches.
 * @todo : make recent searches persistent.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public class SearchField extends JTextField {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    protected static final Logger _logger = LoggerFactory.getLogger(SearchField.class.getName());
    /** disarm color */
    private static final Color DISARMED_GRAY = new Color(0.7f, 0.7f, 0.7f);
    /** Mac flag, true if running on a Mac OS X computer, false otherwise */
    private static final boolean MACOSX_RUNTIME = SystemUtils.IS_OS_MAC_OSX;
    /** debug flag to draw border area */
    private static final boolean DEBUG_AREA = false;

    /* members */
    /** Store whether notifications should be sent every time a key is pressed */
    private boolean _sendsNotificationForEachKeystroke = false;
    /** Store whether a text should be drawn when nothing else in text field */
    private boolean _showingPlaceholderText = false;
    /** Store the text displayed when nothing in */
    private final String _placeholderText;
    /** Store the previous entered text */
    private String _previousText = "";
    /** Store shape object representing the search button area */
    private Shape _searchButtonShape = null;
    /** Store whether the mouse is over the cancel cross */
    private boolean _armedCancelButton = false;
    /** Store optional shape object representing the cancel button area */
    private Shape _cancelButtonShape = null;
    /** Store whether the mouse is over the options button */
    private boolean _armedOptionsButton = false;
    /** Store optional shape object representing the options button area */
    private Shape _optionsButtonShape = null;
    /** Store the pop up men for options */
    private JPopupMenu _optionsPopupMenu = null;
    /** Store the rounded rectangle area of this search field */
    private Shape _roundedInnerArea = null;

    /**
     * Creates a new SearchField object with options.
     *
     * @param placeholderText the text displayed when nothing in.
     * @param options the pop up men for options, null if none.
     */
    public SearchField(final String placeholderText, JPopupMenu options) {
        super(8); // 8 characters wide by default

        _placeholderText = placeholderText;
        _optionsPopupMenu = options;

        addFocusListener(new PlaceholderText());
        initBorder();
        initKeyListener();
    }

    /**
     * Creates a new SearchField object.
     *
     * @param placeholderText the text displayed when nothing in.
     */
    public SearchField(final String placeholderText) {
        this(placeholderText, null);
    }

    /**
     * Creates a new SearchField object with a default "Search" place holder.
     */
    public SearchField() {
        this("Search", null);
    }

    /**
     * Draw the custom widget border.
     */
    private void initBorder() {
        // On Mac OS X, simply use the OS specific search textfield widget
        if (MACOSX_RUNTIME) {
            // http://developer.apple.com/mac/library/technotes/tn2007/tn2196.html#//apple_ref/doc/uid/DTS10004439
            putClientProperty("JTextField.variant", "search");
            putClientProperty("JTextField.FindAction",
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            postActionEvent();
                        }
                    });
            putClientProperty("JTextField.CancelAction",
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            handleCancelEdit();
                        }
                    });
            if (_optionsPopupMenu != null) {
                putClientProperty("JTextField.Search.FindPopup", _optionsPopupMenu);
            }

            return;
        }

        // Fallback for platforms other than Mac OS X

        // Add the border that draws the magnifying glass and the cancel cross:
        final int left = 30;
        final int right = 22;
        if (DEBUG_AREA) {
            setBorder(new CompoundBorder(BorderFactory.createMatteBorder(4, left, 4, right, Color.YELLOW), new ButtonBorder()));
        } else {
            setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(4, left, 4, right), new ButtonBorder()));
        }

        final MouseInputListener mouseInputListener = new ButtonBorderMouseListener();
        addMouseListener(mouseInputListener);
        addMouseMotionListener(mouseInputListener);

        // We must be non-opaque since we won't fill all pixels.
        // This will also stop the UI from filling our background.
        setOpaque(false);
    }

    /**
     * Draw the dedicated custom rounded text field.
     *
     * @param g2 the graphical context to draw in.
     */
    @Override
    protected void paintComponent(final Graphics g2) {
        // On anything but Mac OS X
        if (!MACOSX_RUNTIME) {
            final int width = getWidth();
            final int height = getHeight();

            final Graphics2D g2d = (Graphics2D) g2;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Color savedColor = g2d.getColor();

            // Paint a rounded rectangle in the background surrounded by a black line.
            final RoundRectangle2D outerArea = new RoundRectangle2D.Double(0d, 0d, width, height, height, height);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fill(outerArea);

            g2d.setColor(Color.GRAY);
            g2d.fillRoundRect(0, -1, width, height, height, height);

            // inner area (text field):
            final RoundRectangle2D innerArea = new RoundRectangle2D.Double(1d, 1d, width - 2d, height - 2d, height - 2d, height - 2d);
            g2d.setColor(getBackground());
            g2d.fill(innerArea);

            // define clip for the following line only:
            g2.setClip(outerArea);

            g2d.setColor(Color.GRAY);
            g2d.drawLine(0, 1, width, 1);

            // define clip as smaller rounded rectangle (GTK and nimbus LAF):
            _roundedInnerArea = new RoundRectangle2D.Double(3d, 3d, width - 6d, height - 6d, height - 6d, height - 6d);
            g2.setClip(_roundedInnerArea);

            // restore g2d state:
            g2d.setColor(savedColor);
        }

        // Now call the superclass behavior to paint the foreground.
        super.paintComponent(g2);
    }

    /**
     * Follow keystrokes to notify listeners.
     */
    private void initKeyListener() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    handleCancelEdit();
                } else if (_sendsNotificationForEachKeystroke) {
                    maybeNotify();
                }
            }
        });
    }

    /**
     * Reset SearchField content and notify listeners.
     */
    private void handleCancelEdit() {
        if (!_showingPlaceholderText && getText().length() != 0) {
            // Field is NOT empty
            setText("");
        }
        postActionEvent();
    }

    /**
     * Display Options.
     * @param me mouse event to define menu location
     */
    private void handleShowOptions(final MouseEvent me) {
        if (_optionsPopupMenu != null) {
            _optionsPopupMenu.validate();
            _optionsPopupMenu.show(this, me.getX() + 5, me.getY() + 10);
        }
    }

    /**
     * Sets the text of this <code>TextComponent</code>
     * to the specified text.
     *
     * This overrides the default behavior to tell the placeholder to use this new text value
     *
     * @param txt the new text to be set
     */
    @Override
    public void setText(final String txt) {
        super.setText(txt);

        if (!_placeholderText.equals(txt)) {
            _previousText = txt;
        }
    }

    /**
     * Returns the text contained in this <code>TextComponent</code>.
     *
     * If the text corresponds to the placeholder text then it returns "".
     *
     * @return the text, not the placeholder text
     */
    public final String getRealText() {
        final String txt = super.getText();

        if (_placeholderText.equals(txt)) {
            return "";
        }
        return txt;
    }

    /**
     * Trap notifications when showing place holder.
     */
    private void maybeNotify() {
        if (_showingPlaceholderText) {
            return;
        }

        postActionEvent();
    }

    /**
     * Store whether notifications should be sent for each key pressed.
     *
     * @param eachKeystroke true to notify any key pressed, false otherwise.
     */
    public void setSendsNotificationForEachKeystroke(final boolean eachKeystroke) {
        _sendsNotificationForEachKeystroke = eachKeystroke;
    }

    /**
     * Draws the cancel button (a gray circle with a white cross) and the magnifying glass icon ...
     */
    private final class ButtonBorder extends EmptyBorder {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;
        /** debug flag to draw shapes */
        private static final boolean DEBUG_SHAPES = false;

        /**
         * Constructor
         */
        ButtonBorder() {
            super(0, 0, 0, 0);
        }

        /**
         * Paint this border
         */
        @Override
        public void paintBorder(final Component c, final Graphics g2,
                final int x, final int y, final int width, final int height) {

            final SearchField field = (SearchField) c;
            final Graphics2D g2d = (Graphics2D) g2;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Color savedColor = g2d.getColor();
            if (DEBUG_SHAPES) {
                g2d.setColor(Color.BLUE);
                g2d.draw(new Rectangle(x, y, width, height));
            }
            final Color backgroundColor = field.getBackground();

            // left = x = 30

            // Draw magnifying glass lens:
            final int diskL = 10;
            final int diskX = x - diskL - 15;
            final int diskY = y + ((height - 1 - diskL) / 2);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(diskX, diskY, diskL, diskL);
            g2d.setColor(backgroundColor);
            g2d.fillOval(diskX + 2, diskY + 2, diskL - 4, diskL - 4);

            // Draw magnifying glass handle:
            final int downX = (diskX + diskL) - 3;
            final int downY = (diskY + diskL) - 3;
            final int upX = downX + 4;
            final int upY = downY + 4;
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawLine(downX, downY, upX, upY);
            g2d.drawLine(downX, downY, upX, upY);
            g2d.drawLine(downX + 1, downY, upX, upY);

            // draw the popup arrow if options are available
            if (_optionsPopupMenu != null) {
                // Draw shaded arrow
                g2d.setColor(_armedOptionsButton ? DISARMED_GRAY : Color.GRAY);

                final int size = 4;

                final int xOrigin = diskX + diskL + 3 + size;
                final int yOrigin = y + height / 2 + 1;
                final int[] xPoints = {xOrigin - size, xOrigin + size, xOrigin};
                final int[] yPoints = {yOrigin - size, yOrigin - size, yOrigin + size};
                g2d.fillPolygon(xPoints, yPoints, 3);

                // add 1 pixel margin:
                _searchButtonShape = null;
                _optionsButtonShape = new Rectangle(diskX - 1, diskY - 1, diskL + 2 + 2 * size + 2, diskL + 2);
                if (DEBUG_SHAPES) {
                    g2d.setColor(Color.RED);
                    g2d.draw(_optionsButtonShape);
                }
            } else {
                // add 1 pixel margin:
                _searchButtonShape = new Rectangle(diskX - 1, diskY - 1, diskL + 2, diskL + 2);
                if (DEBUG_SHAPES) {
                    g2d.setColor(Color.RED);
                    g2d.draw(_searchButtonShape);
                }
                _optionsButtonShape = null;
            }

            if (!_showingPlaceholderText && getText().length() != 0) {
                // if NOT empty, draw the cancel cross

                // right = x + width = 22

                // Draw shaded disk
                final int circleL = 14;
                final int circleX = (x + width) + (22 - 5) - circleL;
                final int circleY = y + ((height - circleL) / 2);

                _cancelButtonShape = new Ellipse2D.Double(circleX, circleY, circleL, circleL);
                g2d.setColor(_armedCancelButton ? Color.GRAY : DISARMED_GRAY);
                g2d.fill(_cancelButtonShape);

                if (DEBUG_SHAPES) {
                    g2d.setColor(Color.RED);
                    g2d.draw(_cancelButtonShape);
                }

                // Draw white cross
                final int lineL = circleL - 8;
                final int lineX = circleX + 4;
                final int lineY = circleY + 4;
                g2d.setColor(backgroundColor);
                g2d.drawLine(lineX, lineY, lineX + lineL, lineY + lineL);
                g2d.drawLine(lineX, lineY + lineL, lineX + lineL, lineY);

            } else {
                // reset area:
                _cancelButtonShape = null;
            }

            // restore g2d state:
            g2d.setColor(savedColor);
        }
    }

    /**
     * Handles a click on the cancel button by clearing the text and notifying
     * any ActionListeners.
     */
    private final class ButtonBorderMouseListener extends MouseInputAdapter {

        /**
         * Return true if the mouse is over the cancel or options button
         * @param me mouse event
         * @return true if any armed flag changed
         */
        private boolean isOverButtons(final MouseEvent me) {
            boolean changed = false;
            // If the button is down, we might be outside the component
            // without having had mouseExited invoked.
            if (!_roundedInnerArea.contains(me.getPoint())) {
                changed = _armedCancelButton || _armedOptionsButton;
                // reset:
                _armedCancelButton = false;
                _armedOptionsButton = false;
                setCursor(Cursor.getDefaultCursor());
                return changed;
            }

            // check if the mouse is over the search button:
            boolean armedSearchButton = false;
            if (_searchButtonShape != null) {
                armedSearchButton = _searchButtonShape.contains(me.getPoint());
            }

            // check if the mouse is over the cancel button:
            if (_cancelButtonShape != null) {
                final boolean armed = _cancelButtonShape.contains(me.getPoint());

                if (armed != _armedCancelButton) {
                    _armedCancelButton = armed;
                    changed = true;
                }
            }

            // check if the mouse is over the options button:
            if (_optionsButtonShape != null) {
                final boolean armed = _optionsButtonShape.contains(me.getPoint());

                if (armed != _armedOptionsButton) {
                    _armedOptionsButton = armed;
                    changed = true;
                }
            }

            setCursor((armedSearchButton || _armedCancelButton || _armedOptionsButton)
                    ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

            return changed;
        }

        /**
         * Handle mouse event i.e. test mouse over buttons (arm) and repaint if needed
         * @param me mouse event
         */
        private void handleMouseEvent(final MouseEvent me) {
            if (isOverButtons(me)) {
                repaint();
            }
        }

        @Override
        public void mouseMoved(final MouseEvent me) {
            handleMouseEvent(me);
        }

        @Override
        public void mouseDragged(final MouseEvent me) {
            handleMouseEvent(me);
        }

        @Override
        public void mouseEntered(final MouseEvent me) {
            handleMouseEvent(me);
        }

        @Override
        public void mouseExited(final MouseEvent me) {
            handleMouseEvent(me);
        }

        @Override
        public void mousePressed(final MouseEvent me) {
            handleMouseEvent(me);
        }

        @Override
        public void mouseReleased(final MouseEvent me) {
            isOverButtons(me);

            // enable actions only if the text field is enabled:
            if (SwingUtilities.isLeftMouseButton(me) && isEnabled()) {
                if (_armedCancelButton) {
                    handleCancelEdit();
                }
                if (_armedOptionsButton) {
                    handleShowOptions(me);
                }
            }
            repaint();
        }
    }

    /**
     * Replaces the entered text with a gray placeholder string when the
     * search field doesn't have the focus. The entered text returns when
     * we get the focus back.
     */
    private final class PlaceholderText implements FocusListener {

        /** color used when the field has the focus */
        private Color _previousColor;

        /**
         * Constructor
         */
        PlaceholderText() {
            // get initial text and colors:
            focusLost(null);
        }

        @Override
        public void focusGained(final FocusEvent fe) {
            setForeground(_previousColor);
            setText(_previousText);
            _showingPlaceholderText = false;
        }

        @Override
        public void focusLost(final FocusEvent fe) {
            _previousText = getRealText();
            _previousColor = getForeground();

            // if the field is empty :
            if (_previousText.length() == 0) {
                _showingPlaceholderText = true;
                setForeground(Color.GRAY);
                setText(_placeholderText);
            }
        }
    }

    /**
     * Main - for StarResolverWidget demonstration and test only.
     * @param args unused
     */
    public static void main(final String[] args) {

        final boolean testOptions = true;

        // GUI initialization
        final JFrame frame = new JFrame();
        frame.setTitle("SearchField Demo");

        // Force to exit when the frame closes :
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        final SearchField searchField;
        if (testOptions) {
            final JPopupMenu optionsMenu = new JPopupMenu();

            // Add title
            JMenuItem menuItem = new JMenuItem("Choose Search Option:");
            menuItem.setEnabled(false);
            optionsMenu.add(menuItem);

            // And populate the options:
            menuItem = new JMenuItem("Test option");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    System.out.println("e = " + e);
                }
            });

            optionsMenu.add(menuItem);

            searchField = new SearchField("placeHolder", optionsMenu);
        } else {
            searchField = new SearchField("placeHolder");
        }
        panel.add(searchField, BorderLayout.CENTER);

        frame.getContentPane().add(panel);

        frame.pack();
        frame.setVisible(true);
    }
}
/*___oOo___*/