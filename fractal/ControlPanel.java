////////////////////////////////////////////////////////////////////////////////
// ControlPanel Class //////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// In terms of the "Model View Controller" Design Pattern, this class
// (plus the DrawingCanvas) make up the View.  The ControlPanel contains
// all of the UI widgets and listens for control events.
//
// Note: the Java AWT uses the "Composite" Design Pattern to implement
// widget containment hierarchies, such as the one used here.
// The various layout managers exemplify the "Strategy" Design Pattern.
//
// Hierarchical layout of the widgets:
//
// ControlPanel: this
//   DrawingCanvas: drawingCanvas
//   Label: status
//   Panel: controlsPanel
//     Panel: controlPanels[ 0 ]
//       Button: drawButton
//     Panel: controlPanels[ 1 ]
//       Label: colorLabel
//       Choice: colorChoice
//     Panel: controlPanels[ 2 ]
//       Label: iterationsLabel
//       TextField: iterationsText
//     Panel: controlPanels[ 3 ]
//       Button: stopButton
//       Label: status2
//     Panel: controlPanels[ 4 ]
//       Button: deleteButton
//       Button: helpButton
//     Panel: controlPanels[ 5 ]
//       Button: prevButton
//       Button: nextButton
//     Panel: controlPanels[ 6 ]
//       Label: rMinLabel
//       TextField: rMinText
//     Panel: controlPanels[ 7 ]
//       Label: rMaxLabel
//       TextField: rMaxText
//     Panel: controlPanels[ 8 ]
//       Label: iMinLabel
//       TextField: iMinText
//     Panel: controlPanels[ 9 ]
//       Label: iMaxLabel
//       TextField: iMaxText
//     Panel: controlPanels[ 10 ]
//       Checkbox: drawJuliaCheckbox
//     Panel: controlPanels[ 11 ]
//       Label: juliaCRLabel
//       TextField: juliaCRText
//     Panel: controlPanels[ 12 ]
//       Label: juliaCILabel
//       TextField: juliaCIText

package fractal;

import java.awt.*;
import java.awt.event.*;
import fractal.utils.*;

class ControlPanel extends Panel
{
  private DrawingCanvas     drawingCanvas;
  private Fractal           fractal;
  private Insets            insets;
  private final int         NUM_ROWS = 13;
  private boolean           parameterChangeFlag; // See hasNewParameters()

  private GridLayout        controlsGrid;
  private GridLayout[]      controlGrids;
  private Panel             controlsPanel;
  private Panel[]           controlPanels;

  private Button            deleteButton;
  private Button            drawButton;
  private Button            helpButton;
  private Button            nextButton;
  private Button            prevButton;
  private Button            stopButton;

  private Checkbox          drawJuliaCheckbox;
  private Choice            colorChoice;

  private Label             colorLabel;
  private Label             iMaxLabel;
  private Label             iMinLabel;
  private Label             iterationsLabel;
  private Label             juliaCILabel;
  private Label             juliaCRLabel;
  private Label             rMaxLabel;
  private Label             rMinLabel;
  private Label             status;
  private Label             status2;

  private TextField         iMaxText;
  private TextField         iMinText;
  private TextField         iterationsText;
  private TextField         juliaCIText;
  private TextField         juliaCRText;
  private TextField         rMaxText;
  private TextField         rMinText;

  protected ControlPanel( Fractal fractal, DrawingCanvas drawingCanvas )
  {
    this.fractal = fractal;
    this.drawingCanvas = drawingCanvas;
    parameterChangeFlag = false;
    insets = null;
  }

  protected void addColor( String colorSchemeName )
  {
    colorChoice.addItem( colorSchemeName );
  }

  public boolean getColor( StringBuffer colorSchemeName )
  {
    try
    {
      String scheme = colorChoice.getSelectedItem();
      if( scheme == null )
      {
        return false;
      }
      colorSchemeName.setLength( 0 );
      colorSchemeName.append( scheme );
    }
    catch( Throwable t )
    {
      return false;
    }
    return true;
  }

  protected boolean getComplexRect( ComplexRectangle newRect )
  {
    double rMin;
    double rMax;
    double iMin;
    double iMax;

    // Get the user's new draw parameters from the text controls.
    // Convert them from Strings to numbers, if possible.
    try
    {
      rMin = Double.valueOf( rMinText.getText().trim() ).doubleValue();
      rMax = Double.valueOf( rMaxText.getText().trim() ).doubleValue();
      iMin = Double.valueOf( iMinText.getText().trim() ).doubleValue();
      iMax = Double.valueOf( iMaxText.getText().trim() ).doubleValue();

      if( rMin >= rMax )
      {
        setStatus( " Warning: The Real Max is less than the Min." );
        return false;
      }

      if( iMin >= iMax )
      {
        setStatus( " Warning: Imaginary Max is less than the Min." );
        return false;
      }
    }
    catch( Throwable t )
    {
      setStatus( " Warning: Could not convert all input to numbers." );
      return false;
    }

    newRect.set( rMin, rMax, iMin, iMax );
    return true;
  }

  public Insets getInsets()
  {
    // Called by the AWT.  There is no setInsets().  (Swing fixes this.)
    // Overriding getInsets() is the only way to set the inset values.
    if( insets == null )
    {
      insets = super.getInsets(); // Define a 5 pixel border.
      insets = new Insets( insets.top + 5, insets.left + 5,
                           insets.bottom + 5, insets.right + 5 );
    }
    return insets;
  }

  protected boolean getIterations( IntWrapper iterations )
  {
    // Get the maximum number of iterations, if possible.
    int mi = 0;
    try
    {
      mi = Integer.valueOf( iterationsText.getText().trim() ).intValue();
      if( mi < 1 )
      {
        setStatus( " Warning: there must be at least 1 iteration." );
        return false;
      }
    }
    catch( Throwable t )
    {
      setStatus( " Warning: Cannot convert iterations to a number." );
      return false;
    }

    iterations.setValue( mi );
    return true;
  }

  protected boolean getJuliaPoint( ComplexPoint c )
  {
    try
    {
      double juliaCR = Double.valueOf(
                       juliaCRText.getText().trim() ).doubleValue();
      double juliaCI = Double.valueOf(
                       juliaCIText.getText().trim() ).doubleValue();
      c.set( juliaCR, juliaCI );
    }
    catch( Throwable t )
    {
      setStatus( " Warning: Cannot get Julia point." );
      return false;
    }
    return true;
  }

  protected boolean hasNewParameters()
  {
    // Has the user made any changes to any of the drawing parameters?
    // Testing equality of doubles doesn't always work due to roundoff error,
    // so the code instead checks if anything has been modified at all.
    // Consistent use of BigDecimal rounding should allow equality tests
    // to work, but for now, this does the trick.
    return parameterChangeFlag;
  }

  protected void initializeEventListeners()
  {
    // Example of the "Observer" Design Pattern:
    // ControlPanel's inner classes "observe" the AWT widgets.

    // Detect the Draw ("Make New Fractal") button being pressed.
    DrawHandler drawHandler = new DrawHandler();
    drawButton.addActionListener( drawHandler );

    // Detect the "Stop" button being pressed.
    stopButton.addActionListener( new StopHandler() );

    // Detect the "Previous" button being pressed.
    prevButton.addActionListener( new PreviousHandler() );

    // Detect the "Next" button being pressed.
    nextButton.addActionListener( new NextHandler() );

    // Detect the "Delete" button being pressed.
    deleteButton.addActionListener( new DeleteHandler() );

    // Detect the "Help" button being pressed.
    helpButton.addActionListener( new HelpHandler() );

    // Listen for the return key, which will always Draw.
    ReturnKeyHandler returnKeyHandler = new ReturnKeyHandler();

    // The return key is a KeyEvent for Buttons
    drawButton.addKeyListener( returnKeyHandler );
    prevButton.addKeyListener( returnKeyHandler );
    helpButton.addKeyListener( returnKeyHandler );
    nextButton.addKeyListener( returnKeyHandler );
    deleteButton.addKeyListener( returnKeyHandler );

    // The return key is an ActionEvent for TextFields
    // Reuse drawHandler; we want to draw.
    iterationsText.addActionListener( drawHandler );
    rMinText.addActionListener( drawHandler );
    rMaxText.addActionListener( drawHandler );
    iMinText.addActionListener( drawHandler );
    iMaxText.addActionListener( drawHandler );
    juliaCRText.addActionListener( drawHandler );
    juliaCIText.addActionListener( drawHandler );

    // Listen for FocusEvents on the TextFields
    // in order to (de)select text for easier UI editing.
    FocusHandler focusHandler = new FocusHandler();
    iterationsText.addFocusListener( focusHandler );
    rMinText.addFocusListener( focusHandler );
    rMaxText.addFocusListener( focusHandler );
    iMinText.addFocusListener( focusHandler );
    iMaxText.addFocusListener( focusHandler );
    juliaCRText.addFocusListener( focusHandler );
    juliaCIText.addFocusListener( focusHandler );

    // The Julia checkbox:
    drawJuliaCheckbox.addItemListener( new JuliaHandler() );
    drawJuliaCheckbox.addKeyListener( returnKeyHandler );

    // Color:
    colorChoice.addKeyListener( returnKeyHandler );

    // See if the user changes any parameters by typing.
    TextChangeHandler changeHandler = new TextChangeHandler();
    iterationsText.addKeyListener( changeHandler );
    rMinText.addKeyListener( changeHandler );
    rMaxText.addKeyListener( changeHandler );
    iMinText.addKeyListener( changeHandler );
    iMaxText.addKeyListener( changeHandler );
    juliaCRText.addKeyListener( changeHandler );
    juliaCIText.addKeyListener( changeHandler );
  }

  protected void initializeGraphics()
  {
    // BorderLayout is the default layout strategy, but set it anyway:
    BorderLayout mainBorderLayout = new BorderLayout();
    mainBorderLayout.setHgap( 10 );
    mainBorderLayout.setVgap( 10 );
    setLayout( mainBorderLayout );

    // Add the top-level Components to "this", which is-a-kind-of Container.
    add( drawingCanvas, BorderLayout.CENTER );
    status = new Label( "" );
    status.setFont( Fractal.FONT );
    add( status, BorderLayout.SOUTH );
    controlsPanel = new Panel();
    add( controlsPanel, BorderLayout.WEST );

    // The controlsPanel has a grid of grids...
    controlsGrid = new GridLayout( NUM_ROWS, 1 );
    controlsGrid.setVgap( 10 );
    controlsGrid.setHgap( 10 );
    controlsPanel.setLayout( controlsGrid );

    controlPanels = new Panel[ NUM_ROWS ];
    controlGrids = new GridLayout[ NUM_ROWS ];

    // Initialize the Panel objects in the controlPanels array,
    // and associate them with their respective GridLayout objects.
    int rowNum = 0;
    for( rowNum = 0; rowNum < NUM_ROWS; rowNum++ )
    {
      controlGrids[ rowNum ] = new GridLayout( 1, 2 );

      controlPanels[ rowNum ] = new Panel( );
      controlPanels[ rowNum ].setLayout( controlGrids[ rowNum ] );

      controlGrids[ rowNum ].setHgap( 10 );
      controlGrids[ rowNum ].setVgap( 10 );

      controlsPanel.add( controlPanels[ rowNum ] );
    }

    // Draw Button
    rowNum = 0;
    drawButton = new Button( "Make New Fractal" );
    drawButton.setEnabled( false );
    drawButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( drawButton );

    // "Color:" || Choice
    rowNum++;
    colorLabel = new Label( " Colors:" );
    colorLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( colorLabel );
    colorChoice = new Choice();
    colorChoice.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( colorChoice );

    // "Iterations:" || TextField
    rowNum++;
    iterationsLabel = new Label( " Iterations:" );
    iterationsLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iterationsLabel );
    iterationsText = new TextField( "" );
    iterationsText.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iterationsText );

    // Stop Button || Status Label
    rowNum++;
    stopButton = new Button( "Stop" );
    stopButton.setEnabled( false );
    stopButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( stopButton );
    status2 = new Label( "" );
    status2.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( status2 );

    // Delete Button || Help Button
    rowNum++;
    deleteButton = new Button( "Delete" );
    deleteButton.setEnabled( false );
    deleteButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( deleteButton );
    helpButton = new Button( "Help" );
    helpButton.setEnabled( true );
    helpButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( helpButton );

    // Previous Button || Next Button
    rowNum++;
    prevButton = new Button( "Previous" );
    prevButton.setEnabled( false );
    prevButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( prevButton );
    nextButton = new Button( "Next" );
    nextButton.setEnabled( false );
    nextButton.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( nextButton );

    // "Min Real:" || TextField
    rowNum++;
    rMinLabel = new Label( " Min Real:" );
    rMinLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( rMinLabel );
    rMinText = new TextField( "" );
    rMinText.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( rMinText );

    // "Max Real:" || TextField
    rowNum++;
    rMaxLabel = new Label( " Max Real:" );
    rMaxLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( rMaxLabel );
    rMaxText = new TextField( "" );
    rMaxText.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( rMaxText );

    // "Min Imaginary:" || TextField
    rowNum++;
    iMinLabel = new Label( " Min Imaginary:" );
    iMinLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iMinLabel );
    iMinText = new TextField( "" );
    iMinText.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iMinText );

    // "Max Imaginary:    " || TextField
    // Note: pad the string with blanks to make the length = 19 so that the
    // (sometimes invisible) juliaCILabel is not the longest string.
    // This is required to prevent an annoying layout management nit.
    rowNum++;
    iMaxLabel = new Label( " Max Imaginary:    " );
    iMaxLabel.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iMaxLabel );
    iMaxText = new TextField( "" );
    iMaxText.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( iMaxText );

    // "Draw Julia Set:" Checkbox
    rowNum++;
    drawJuliaCheckbox = new Checkbox( "Draw Julia Set: ", false );
    drawJuliaCheckbox.setFont( Fractal.FONT );
    controlPanels[ rowNum ].add( drawJuliaCheckbox );

    // Julia Set Constants:
    // Don't make these visible until needed.
    juliaCRLabel = new Label( " Julia C Real:" );
    juliaCRLabel.setFont( Fractal.FONT );
    juliaCILabel = new Label( " Julia C Imaginary:" ); // String length = 19
    juliaCILabel.setFont( Fractal.FONT );
    juliaCRText = new TextField( "0.0" );
    juliaCRText.setFont( Fractal.FONT );
    juliaCIText = new TextField( "0.0" );
    juliaCIText.setFont( Fractal.FONT );
  }

  protected boolean isJulia()
  {
    return drawJuliaCheckbox.getState();
  }

  protected void setParameterChangeFlag( boolean b )
  {
    parameterChangeFlag = b;
  }

  protected void setState( boolean drawingNow, boolean outOfMemory,
                           boolean hasNext, boolean hasPrevious )
  {
    if( ! drawingNow && ! outOfMemory )
    {
      drawButton.setEnabled( true );
    }
    else
    {
      drawButton.setEnabled( false );
    }

    if( hasPrevious )
    {
      prevButton.setEnabled( true );
    }
    else
    {
      prevButton.setEnabled( false );
    }

    if( hasNext )
    {
      nextButton.setEnabled( true );
    }
    else
    {
      nextButton.setEnabled( false );
    }

    if( drawingNow )
    {
      stopButton.setEnabled( true );
      stopButton.requestFocus();
    }
    else
    {
      stopButton.setEnabled( false );
      iterationsText.requestFocus();
    }

    if( ! hasPrevious && ! hasNext )
    {
      deleteButton.setEnabled( false );
    }
    else
    {
      deleteButton.setEnabled( true );
      if( outOfMemory )
      {
        deleteButton.requestFocus();
      }
    }
  }

  protected void setStatus( String s )
  {
    // Always show something in the status label.
    if( s.length() == 0 )
    {
      s = fractal.getWelcome();
    }
    status.setText( s );
  }

  protected void setStatus2( String s )
  {
    status2.setText( s );
  }

  private void showJuliaControls( boolean isJulia )
  {
    // The Julia Point Labels & TextFields reside in the last two rows.
    // Sometimes these controls are invisible... So we need to be able to
    // dynamically add/remove the controls from the layout, and then force the
    // layout to be recalculated and redisplayed...
    try
    {
      controlPanels[ NUM_ROWS - 2 ].setVisible( false );
      controlPanels[ NUM_ROWS - 2 ].removeAll();

      controlPanels[ NUM_ROWS - 1 ].setVisible( false );
      controlPanels[ NUM_ROWS - 1 ].removeAll();

      if( isJulia )
      {
        int rowNum = NUM_ROWS - 2;
        controlPanels[ rowNum ].add( juliaCRLabel );
        controlPanels[ rowNum ].add( juliaCRText );
        controlPanels[ rowNum ].setVisible( true );

        rowNum = NUM_ROWS -1;
        controlPanels[ rowNum ].add( juliaCILabel );
        controlPanels[ rowNum ].add( juliaCIText );
        controlPanels[ rowNum ].setVisible( true );
      }

      validate();
      fractal.recalculateLayout(); // Recalculate the layout and repaint.
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (show julia controls) ... " + t );
    }
  }

  protected void updateColor( String colorSchemeName )
  {
    colorChoice.select( colorSchemeName );
  }

  protected void updateComplexRect( ComplexRectangle rect )
  {
    double rMin = rect.getRMin();
    double rMax = rect.getRMax();
    double iMin = rect.getIMin();
    double iMax = rect.getIMax();

    // Adjust the zoom factor to agree with these numbers.
    fractal.adjustZoomFactor( rect );

    rMinText.setText( "" + Fractal.doubleAsString( rMin ));
    rMaxText.setText( "" + Fractal.doubleAsString( rMax ));
    iMinText.setText( "" + Fractal.doubleAsString( iMin ));
    iMaxText.setText( "" + Fractal.doubleAsString( iMax ));
  }

  protected void updateDrawing( Drawing d )
  {
    updateColor( d.getColor() );
    updateComplexRect( d.getComplexRect() );
    updateIterations( d.getMaxIterations() );
    updateJulia( d );
  }

  protected void updateIterations( int mi )
  {
    iterationsText.setText( "" + mi );
  }

  private void updateJulia( Drawing d )
  {
    boolean isJulia = ( d instanceof JuliaDrawing );
    drawJuliaCheckbox.setState( isJulia );
    drawJuliaCheckbox.setEnabled( ! isJulia );

    if( isJulia )
    {
      JuliaDrawing jd = (JuliaDrawing) d;
      updateJuliaPoint( jd.getJuliaPoint() );
    }
    showJuliaControls( isJulia );
  }

  protected void updateJuliaPoint( ComplexPoint cp )
  {
    juliaCRText.setText( "" + Fractal.doubleAsString( cp.getReal() ) );
    juliaCIText.setText( "" + Fractal.doubleAsString( cp.getImaginary() ) );
  }

  /////////////////////////////////////////////////////////////////////
  // INNER CLASSES of class ControlPanel: /////////////////////////////
  /////////////////////////////////////////////////////////////////////

  class DeleteHandler implements ActionListener
  {
    public void actionPerformed( ActionEvent e )
    {
      // The user just clicked the Delete Button...
      fractal.doDelete();
    }
  }

  class DrawHandler implements ActionListener
  {
    // For the "Draw!" Action Events:
    // This is used for the ActionEvents which occur when
    // the the user clicks the Draw Button, and when
    // the return key is pressed while some other widget has the focus.
    public void actionPerformed( ActionEvent e )
    {
      fractal.doDraw();
    }
  }

  class FocusHandler implements FocusListener
  {
    // For TextField Focus Listener:
    // This doesn't work on some browsers...
    public void focusLost( FocusEvent fe )
    {
      TextField tf = (TextField) fe.getComponent();
      tf.select( 0, 0 );  // Unselect all.
    }

    public void focusGained( FocusEvent fe )
    {
      TextField tf = (TextField) fe.getComponent();
      tf.selectAll();
    }
  }

  class HelpHandler implements ActionListener
  {
    public void actionPerformed( ActionEvent e )
    {
      // The user just clicked the Help Button...
      fractal.doHelp();
    }
  }

  class JuliaHandler implements ItemListener
  {
    public void itemStateChanged( ItemEvent e )
    {
      // The user just (un)checked the Draw Julia checkbox...
      synchronized( fractal )
      {
        showJuliaControls( isJulia() );
        fractal.juliaClicked( isJulia() );
      }
    }
  }

  class NextHandler implements ActionListener
  {
    public void actionPerformed( ActionEvent e )
    {
      // The user just clicked the Next Button...
      fractal.doNext();
    }
  }

  class PreviousHandler implements ActionListener
  {
    public void actionPerformed( ActionEvent e )
    {
      // The user just clicked the Previous Button...
      fractal.doPrevious();
    }
  }

  class ReturnKeyHandler extends KeyAdapter
  {
    // For the "Draw!" Key Events:
    // This is used to listen for the return key being pressed
    // when a Button has the focus.  (The return key produces an
    // ActionEvent, not a KeyEvent when a TextField has the focus.)
    public void keyTyped( KeyEvent e )
    {
      char theKey = e.getKeyChar();
      if( theKey == '\r' ) // return / enter
      {
        fractal.doDraw();
      }
    }
  }

  class StopHandler implements ActionListener
  {
    public void actionPerformed( ActionEvent e )
    {
      // The user just clicked the Stop Button...
      fractal.doStop();
    }
  }

  class TextChangeHandler extends KeyAdapter
  {
    // For knowing if any control parameters have been updated by the user.
    // If not, then maybe we can quickly remap colors instead of
    // recalculating a new fractal from scratch...
    public void keyTyped( KeyEvent e )
    {
      char theKey = e.getKeyChar();
      if( theKey != '\t' )  // ignore tabs
      {
        parameterChangeFlag = true;
      }
    }
  }
}