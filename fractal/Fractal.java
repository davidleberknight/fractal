////////////////////////////////////////////////////////////////////////////////
// Fractal : an Applet and an Application for drawing Fractals /////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
// Last Modified: February 28th, 2001 ( version 2.9 )
//
// Fractal was designed primarily as an example for CSCI 4448 -
// Object-Oriented Programming and Design, at the University of Colorado.
//
// The code is developed using the Java JDK version 1.1.6.
// so it will not work on some of the oldest Internet browsers.
//
// The Applet may be viewed at the following URL:
// http://www.softwarefederation.com/fractal.html
//
// Example HTML for applet:
// < applet
//   CODE ="fractal.Fractal.class"
//   WIDTH = 800
//   HEIGHT = 600 >
// < PARAM NAME = "ImageWidth" VALUE = 470 >
// < PARAM NAME = "ImageHeight" VALUE = 470 >
// < PARAM NAME = "NumColors" VALUE = 512 >
// < /applet >
//
// Note: for the Applet version, there are two size parameters that must be set
// in harmony with each other: the entire Applet's height and width, and the
// DrawingCanvas' ImageHeight and ImageWidth. The -w and -h for the Application
// version's command line (below) set the size of the DrawingCanvas image.
//
// Example Command Line for Application:
// java fractal.Fractal -w 800 -h 800 -c 1024
//
// The Fractal class acts as the executive / controller for the entire Fractal
// program.  It is an example of the "Mediator" Design Pattern.  This code may
// also be thought of in terms of the "Model View Controller" Design Pattern,
// where class Fractal is the Controller, the Calculators and Drawings are the
// Model, and the ControlPanel and DrawingCanvas make up the View.
//
// The Fractal program is multi-threaded: There are Java AWT Threads which call
// into the code to paint(), and notify us of user events, such as mouse
// movement and button clicks. Refer to ControlPanel and DrawingCanvas for more
// on detecting user events. There are Threads originating from the applet
// browser, such as init() and stop(). Finally, there is a separate Thread to
// calculate each new Fractal.  Care must be taken whenever two Threads try to
// access the same object(s) at the same time; this code uses a single "monitor"
// object, the single instance of Fractal, to control Thread synchronization.
//
// Note: a few minor features in the UI do not work across all browsers & JVMs.
// For example, on some browsers typing 'Enter' *always* calls doDraw()...
// There is one other known defect: it is possible to zoom in so deeply that
// arithmetic precision is required beyond that provided by Java's primative
// type, double, resulting in a loss of image resolution.  Replacing references
// to double with java.math.BigDecimal would fix this, but such a simple
// approach would not be viable because number-crunching using BigDecimals is
// orders of magnitude slower than using doubles. It would be possible to use a
// DeepZoomCalculator "strategy" that would use BigDecimal arithmetic only as
// required (using primative doubles for "shallow zooms"), but the performance
// would still be unacceptably slow for deep zooming (I have coded up a
// prototype).  The ultimate solution would be to get a *fast* BigDecimal
// component from a component vendor, and use it instead of java.math.BigDecimal
// for the DeepZoomCalculator strategy.  Stay tuned...

package fractal;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;
import java.math.*;
import java.text.*;
import java.util.*;
import fractal.utils.*;

public class Fractal extends Applet
{
  // These defaults can be overridden from the command line or HTML parameters:
  private static int             imageHeight = 470; // default
  private static int             imageWidth = 470; // default
  private static int             numColors = 512; // colors per colormap

  // If this code is run as an Application, main() will set this to true.
  private static boolean         isApplication = false;

  // Color:
  private Hashtable              colorTable;
  private final String           COLORS_BLACK_AND_WHITE = "black & white";
  private final String           COLORS_BLUE_ICE        = "blue ice";
  private final String           COLORS_FUNKY           = "funky";
  private final String           COLORS_PASTEL          = "pastel";
  private final String           COLORS_PSYCHEDELIC     = "psychedelic";
  private final String           COLORS_PURPLE_HAZE     = "purple haze";
  private final String           COLORS_RADICAL         = "radical";
  private final String           COLORS_RAINBOW         = "rainbow";
  private final String           COLORS_RAINBOWS        = "rainbows";
  private final String           COLORS_SCINTILLATION   = "scintillation";
  private final String           COLORS_WARPED          = "warped";
  private final String           COLORS_WILD            = "wild";
  private final String           COLORS_ZEBRA           = "zebra";

  // These stacks contain Drawing objects:
  private Stack                  nextStack;
  private Stack                  previousStack;

  // Graphical things (the "View"):
  private ControlPanel           controlPanel;
  private DrawingCanvas          drawingCanvas;

  // The initial views into the sets:
  private final int              INITIAL_ITERATIONS = 33;
  private final ComplexRectangle INITIAL_RECT =
                                 new ComplexRectangle( -2.5, 1.5, -2.0, 2.0 );
  private final ComplexRectangle INITIAL_JULIA_RECT =
                                 new ComplexRectangle( -2.0, 2.0, -2.0, 2.0 );
  // Misc...
  private FractalCalculator      calculator;
  private Drawing                currentDrawing;
  private boolean                drawingNow = false;
  public  final static Font      FONT = new Font( "TimesRoman", Font.BOLD, 16 );
  private HelpDrawing            helpDrawing;
  private boolean                outOfMemory = false;
  private int                    previousIterations = 1;
  private static double          zoomFactor;

////////////////////////////////////////////////////////////////////////////////

  public Fractal()
  {
    // All initialization done in init().
  }

  protected void adjustZoomFactor( ComplexRectangle cr )
  {
    zoomFactor = INITIAL_RECT.getWidth() / cr.getWidth();
  }

  protected synchronized void calculatorCallback( boolean success,
                                                  Drawing newDrawing )
  {
    // Called by the Thread that calculates a new fractal.
    try
    {
      if( success )
      {
        // We need the zoom rect in whatever state it is in right now,
        // even if the mouse is still being dragged.
        drawingCanvas.interruptZoomDrag();
        setCurrentZoom();

        previousStack.push( currentDrawing ); // Current becomes previous.
        setCurrentDrawing( newDrawing );
        drawingCanvas.redraw( newDrawing );
      }
      else
      {
        if( outOfMemory )
        {
          setStatus2( "Out of Memory! " );
        }
        else
        {
          setStatus2( "Stopped. " );
        }
      }
    }
    catch( OutOfMemoryError oom )
    {
      outOfMemory( true );
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (drawing done) ... " + t );
    }
    finally
    {
      controlPanel.setParameterChangeFlag( false );
      drawingNow = false;
      controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious());
      calculator = null; // garbage
      System.gc();
    }
  }

  public synchronized void destroy()
  {
    // May be called by the Applet's browser or by an application Frame's
    // windowClosing event.
    doStop();  // Stop any calculations in progress.
    System.out.println( "Fractal destroyed." );
  }

  private boolean detectDeepZoom( Drawing d )
  {
    // "Deep Zoom" occurs when the precision provided by the Java type double
    // runs out of resolution.  The use of BigDecimal is required to fix this.
    ComplexRectangle cr = d.getComplexRect();
    double deltaDiv2 = cr.getWidth() / ( (double)( imageWidth ) * 2.0 );
    String min = "" + ( cr.getRMin() );
    String minPlus = "" + ( cr.getRMin() + deltaDiv2 );

    if( Double.valueOf( min ).doubleValue() ==
        Double.valueOf( minPlus ).doubleValue() )
    {
      String deepZoom = "Deep Zoom...  Drawing resolution will be degraded ;-(";
      setStatus( deepZoom );
      System.out.println( "" );
      System.out.println( "Fractal: " + deepZoom );
      return true;
    }
    return false;
  }

  protected synchronized void doDelete()
  {
    try
    {
      if( ! hasNext() && ! hasPrevious() )
      {
        // We shouldn't get here (delete should have been disabled).
        return; // Don't delete the last Drawing.
      }

      drawingCanvas.interruptZoomDrag();
      Drawing newCurrentDrawing = null;

      if( hasNext() )
      {
        newCurrentDrawing = ( Drawing ) nextStack.pop();
      }
      else
      {
        newCurrentDrawing = ( Drawing ) previousStack.pop();
      }
      setCurrentDrawing( newCurrentDrawing );
      setColor( newCurrentDrawing.getColor() );
      drawingCanvas.redraw( newCurrentDrawing );

      outOfMemory( false );
      setStatus2( "Deleted." );
      System.out.println( " " );
      System.out.println( "Drawing deleted." );
    }
    catch( OutOfMemoryError oom )
    {
      outOfMemory( true );
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (delete) ... " + t );
    }
    finally
    {
      setStatus( " " );
      controlPanel.setParameterChangeFlag( false );
      controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious());
      System.gc();
    }
  }

  protected synchronized void doDraw()
  {
    try
    {
      setStatus( " " );
      setStatus2( " " );
      if( drawingNow )
      {
        // Shouldn't be able to have this condition, but check anyway.
        return;
      }
      if( outOfMemory )
      {
        outOfMemory( true );
        return;
      }

      ComplexRectangle newRect = new ComplexRectangle();
      IntWrapper maxIterations = new IntWrapper( 1 );
      ComplexPoint juliaPoint = new ComplexPoint();
      StringBuffer color = new StringBuffer();
      // Must use StringBuffer for color because String is immutable.

      if( getNewParameters( newRect, maxIterations, juliaPoint, color ) )
      {
        // Go for it!
        makeNewFractal( newRect, maxIterations, juliaPoint, color.toString() );
      }
    }
    catch( OutOfMemoryError oom )
    {
      outOfMemory( true );
      calculatorCallback( false, null );
    }
    catch( Throwable t )
    {
      calculatorCallback( false, null );
      System.out.println( "Fractal ERROR !!! (draw) ... " + t );
    }
  }

  protected synchronized void doHelp()
  {
    if( currentDrawing instanceof HelpDrawing )
    {
      return;
    }

    try
    {
      if( helpDrawing == null )
  	  {
        Image image = drawingCanvas.createImage( imageWidth, imageHeight );
	      helpDrawing = new HelpDrawing( getWelcome(), INITIAL_RECT,
                                       INITIAL_ITERATIONS, image,
                                       COLORS_RAINBOW );
  	  }
      if( currentDrawing != null )
      {
        // Update the currentDrawing with the current zoom.
        drawingCanvas.interruptZoomDrag();
        setCurrentZoom();
        nextStack.push( currentDrawing );
        helpDrawing.dump();
        setStatus( " " );
        setStatus2( " " );
      }

      setCurrentDrawing( helpDrawing );
      drawingCanvas.redraw( helpDrawing );
    }
    catch( OutOfMemoryError oom )
    {
      outOfMemory( true );
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (help) ... " + t );
    }
    finally
    {
      // Save memory and make a nicer interface. Only keep one Help screen.
      removePreviousHelpScreen();
      controlPanel.setParameterChangeFlag( false );
      controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious() );
    }
  }

  protected synchronized void doNext()
  {
    doNextPrevious( nextStack, previousStack );
  }

  private void doNextPrevious( Stack fromStack, Stack toStack )
  {
    try
    {
      setStatus( "" );
      setStatus2( "" );
      drawingCanvas.interruptZoomDrag();
      setCurrentZoom();
      toStack.push( currentDrawing );
      Drawing newCurrentDrawing = ( Drawing ) fromStack.pop();
      setCurrentDrawing( newCurrentDrawing );
      setColor( newCurrentDrawing.getColor() );

      if( fromStack.empty() )
      {
         transferFocus();
      }
      drawingCanvas.redraw( newCurrentDrawing );
    }
    catch( OutOfMemoryError oom )
    {
      outOfMemory( true );
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (next previous) ... " + t );
    }
    finally
    {
      controlPanel.setParameterChangeFlag( false );
      controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious());
    }
  }

  protected synchronized void doPrevious()
  {
    doNextPrevious( previousStack, nextStack );
  }

  protected synchronized void doStop()
  {
    if( calculator != null )
    {
      calculator.stop();
    }
  }

  public static String doubleAsString( double d )
  {
    // This global method encapsulates a trick to prevent undesirable rounding
    // effects from being noticable to the user.
    // The scale is determined by the zoom factor of the current drawing.
    int scale = getBigDecimalScale();
    BigDecimal big = new BigDecimal( d );
    return big.setScale( scale, BigDecimal.ROUND_HALF_UP ).toString();
  }

  protected void expandRectToFitImage( ComplexRectangle complexRect )
  {
    // The complex rectangle must be scaled to fit the pixel image view.
    // Method: compare the width/height ratios of the two rectangles.
    double imageWHRatio = 1.0;
    double complexWHRatio = 1.0;
    double iMin = complexRect.getIMin();
    double iMax = complexRect.getIMax();
    double rMin = complexRect.getRMin();
    double rMax = complexRect.getRMax();
    double complexWidth = rMax - rMin;
    double complexHeight = iMax - iMin;

    if( ( imageWidth != 0 ) && ( imageHeight != 0 ) )
    {
      imageWHRatio = ((double)imageWidth / (double)imageHeight);
    }
    else return;

    if( ( complexWidth != 0 ) && ( complexHeight != 0 ) )
    {
      complexWHRatio = complexWidth / complexHeight;
    }
    else return;

    if( imageWHRatio == complexWHRatio ) return;

    if( imageWHRatio < complexWHRatio )
    {
      // Expand vertically
      double newHeight = complexWidth / imageWHRatio;
      double heightDifference = Math.abs( newHeight - complexHeight );
      iMin = iMin - heightDifference / 2;
      iMax = iMax + heightDifference / 2;
    }
    else
    {
      // Expand horizontally
      double newWidth = complexHeight * imageWHRatio;
      double widthDifference = Math.abs( newWidth - complexWidth );
      rMin = rMin - widthDifference / 2;
      rMax = rMax + widthDifference / 2;
    }
    complexRect.set( rMin, rMax, iMin, iMax );
  }

  protected void firstPaint()
  {
    // Called from the very first call to DrawingCanvas.paint().
    // Only now can we assume that everything we need is properly initialized.
    String welcome = getWelcome();
    setStatus( welcome );
    System.out.println( " " );
    System.out.println( welcome );
    System.out.println( "Image width = " + imageWidth );
    System.out.println( "Image height = " + imageHeight );
    System.out.println( "" + numColors + " different colors per color map." );

    doHelp(); // Show the Help screen immediately.
    doDraw(); // Begin calculating the initial Mandelbrot set.
  }

  private void getAppletParameters()
  {
    try
    {
      String imageWidthString = getParameter( "ImageWidth" ).trim();
      String imageHeightString = getParameter( "ImageHeight" ).trim();
      String numColorsString = getParameter( "NumColors" ).trim();

      imageWidth = Integer.valueOf( imageWidthString ).intValue();
      imageHeight = Integer.valueOf( imageHeightString ).intValue();
      numColors = Integer.valueOf( numColorsString ).intValue();
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal Warning: (html) ... " + t );
    }
  }

  private static void getApplicationParameters( String[] args )
  {
    try
    {
      int argNum = 0;

      while( argNum < args.length )
      {
        if( args[ argNum ].equals( "-w" ))
        {
          argNum++;
          imageWidth = Integer.valueOf( args[ argNum++ ].trim() ).intValue();
        }
        else if( args[ argNum ].equals( "-h" ))
        {
          argNum++;
          imageHeight = Integer.valueOf( args[ argNum++ ].trim() ).intValue();
        }
        else if( args[ argNum ].equals( "-c" ))
        {
          argNum++;
          numColors = Integer.valueOf( args[ argNum++ ].trim() ).intValue();
        }
        else
        {
          throw new Exception();
        }
      }
    }
    catch( Throwable t )
    {
      String usageString = "Fractal Usage: java fractal.Fractal [-w width] " +
        "[-h height] [-c numColors]";
      System.out.println( usageString );
    }
  }

  private static int getBigDecimalScale()
  {
    // Determine how many decimal places of resolution are required (min = 4).
    // This quantity is directly related to the base 10 log of the zoomFactor.
    // Note: Math.log(x) uses base E (2.71828...) not base 10, so in order to
    // compute a base 10 logarithm, use: Math.log( x ) / Math.log( 10 ).
    double zoom = getZoomFactor();
    if( zoom < 1.0 )
    {
      zoom = 1.0; // forces log( zoom ) >= 0
    }
    int scale = (int)( Math.log( zoom ) / 2.3 ); // Math.log( 10 ) = 2.3
    return scale + 4;
  }

  protected String getColor()
  {
    StringBuffer color = new StringBuffer();
    if( ! controlPanel.getColor( color ) )
    {
      return COLORS_RAINBOWS; // Should never happen.
    }
    return color.toString();
  }

  private ComplexPoint getComplexPoint( int x, int y )
  {
    ComplexRectangle currentRect = getCurrentRect();

    // Delta is the numerical range covered per pixel.
    double delta = currentRect.getWidth() / ( (double)imageWidth );
    double r = currentRect.getRMin() + ( x * delta );
    double i = currentRect.getIMin() + ((((double)imageHeight ) - y ) * delta );
    return new ComplexPoint( r, i );
  }

  protected Color[] getCurrentColorMap()
  {
    return (Color[]) colorTable.get( getColor() );
  }

  protected Drawing getCurrentDrawing()
  {
    return currentDrawing;
  }

  protected ComplexRectangle getCurrentRect()
  {
    return currentDrawing.getComplexRect();
  }

  protected int getImageHeight()
  {
    return imageHeight;
  }

  protected int getImageWidth()
  {
    return imageWidth;
  }

  private void getMouseZoomRect( ComplexRectangle newRect )
  {
    // We need the zoom rect in whatever state it is in right now,
    // even if the mouse is still being dragged (pressed down).
    drawingCanvas.interruptZoomDrag();

    // Map from pixel coordinates to the imaginary plane.
    Rectangle zoom = drawingCanvas.getZoom();

    int zoomXMin = zoom.getBounds().x;
    int zoomXMax = zoomXMin + zoom.getBounds().width;
    int zoomYMin = zoom.getBounds().y;
    int zoomYMax = zoomYMin + zoom.getBounds().height;

    ComplexPoint p1 = getComplexPoint( zoomXMin, zoomYMin );
    ComplexPoint p2 = getComplexPoint( zoomXMax, zoomYMax );

    newRect.set( p1, p2 );
  }

  private boolean getNewParameters( ComplexRectangle newRect,
                                    IntWrapper maxIterations,
                                    ComplexPoint juliaPoint,
                                    StringBuffer color )
  {
    // Get the new zoom coordinates from the TextFields only if
    // the user has not defined a zoom rectangle with the mouse.
    if( drawingCanvas.hasZoom() )
    {
      getMouseZoomRect( newRect );
    }
    else
    {
      if( ! getTextZoomRect( newRect ) )
      {
        return false;
      }
    }
    expandRectToFitImage( newRect );

    if( ! controlPanel.getIterations( maxIterations ) )
    {
      maxIterations.setValue( previousIterations );
      controlPanel.updateIterations( previousIterations );
      return false;
    }
    if( ! controlPanel.getColor( color ) )
    {
      return false;
    }
    if( controlPanel.isJulia() )
    {
      return controlPanel.getJuliaPoint( juliaPoint );
    }
    return true;
  }

  private boolean getTextZoomRect( ComplexRectangle newRect )
  {
    return controlPanel.getComplexRect( newRect );
  }

  protected String getWelcome()
  {
    String welcome = null;
    if( isApplication )
    {
      welcome = "Welcome to the Fractal Application !!!";
    }
    else
    {
      welcome = "Welcome to the Fractal Applet !!!";
    }
    return welcome;
  }

  protected static double getZoomFactor()
  {
    return zoomFactor;
  }

  private int guessNewMaxIterations( boolean isJulia )
  {
    // The higher the zoom factor, the more iterations that are needed to see
    // the detail. Guess at a number to produce a cool looking fractal:
    double zoom = getZoomFactor();
    if( zoom < 1.0 )
    {
      zoom = 1.0; // forces logZoom >= 0
    }
    double logZoom = Math.log( zoom );
    double magnitude = ( logZoom / 2.3 ) - 2.0; // just a guess.
    if( magnitude < 1.0 )
    {
      magnitude = 1.0;
    }
    double iterations = ( (double) INITIAL_ITERATIONS ) *
                        ( magnitude * logZoom + 1.0 );
    if( isJulia ) iterations *= 2.0; // Julia sets tend to need more iterations.
    return (int) iterations;
  }

  private boolean hasNewParameters()
  {
    if( drawingCanvas.hasZoom() )
    {
      return true;
    }
    return controlPanel.hasNewParameters();
  }

  protected boolean hasNext()
  {
    return ( ! nextStack.isEmpty() );
  }

  protected boolean hasPrevious()
  {
    return ( ! previousStack.isEmpty() );
  }

  public synchronized void init()
  {
    // This method gets automatically called by the Applet's browser.
    // Or it gets invoked by main() in the Application version.
    // Do most initialization right here, right now;
    // but defer some initialization to firstPaint()...
    try
    {
      if( ! isApplication )
      {
        getAppletParameters();
      }

      initializeNextPrevStacks();
      initializeGraphics();
      initializeColors();
      initializeEventListeners();

      // Configure the initial view of the Mandelbrot Set.
      ComplexRectangle currentRect = INITIAL_RECT;
      expandRectToFitImage( currentRect );

      controlPanel.updateComplexRect( currentRect );
      controlPanel.updateIterations( INITIAL_ITERATIONS );

      // All is constructed, ready to be displayed...
      repaint(); // Schedules a call to paint() in another Thread.
    }
    catch( Throwable t )
    {
       System.out.println( "Fractal ERROR !!! (init) ... " + t );
    }
    // Now we just sit around and wait for events (and calls to paint)...
    // The event-listener objects are on the job.
  }

  private void initializeColors()
  {
    colorTable = new Hashtable();

    int red = 255;
    int green = 255;
    int blue = 255;

    float hue = (float) 1.0;
    float saturation = (float) 1.0;
    float brightness = (float) 1.0;

    // COLORS_BLACK_AND_WHITE:
    Color[] colorMap = new Color[ numColors ];
    for( int colorNum = numColors -1; colorNum >= 0; colorNum-- )
    {
      colorMap[ colorNum ] = Color.white;
    }
    colorTable.put( COLORS_BLACK_AND_WHITE, colorMap );
    controlPanel.addColor( COLORS_BLACK_AND_WHITE );

    // COLORS_BLUE_ICE:
    blue = 255;
    colorMap = new Color[ numColors ];
    for( int colorNum = numColors -1; colorNum >= 0; colorNum-- )
    {
      red = (int)((255*(float) colorNum / (float) numColors)) % 255;
      green = (int)((255*(float) colorNum / (float) numColors)) % 255;
      colorMap[ colorNum ] = new Color( red, green, blue );
    }
    colorTable.put( COLORS_BLUE_ICE, colorMap );
    controlPanel.addColor( COLORS_BLUE_ICE );

    // COLORS_FUNKY:
    colorMap = new Color[ numColors ];
    for( int colorNum = numColors -1; colorNum >= 0; colorNum-- )
    {
      red = (int)((1024*(float) colorNum / (float) numColors)) % 255;
      green = (int)((512*(float) colorNum / (float) numColors)) % 255;
      blue = (int)((256*(float) colorNum / (float) numColors)) % 255;
      colorMap[ numColors - colorNum -1 ] = new Color( red, green, blue );
    }
    colorTable.put( COLORS_FUNKY, colorMap );
    controlPanel.addColor( COLORS_FUNKY );

    // COLORS_PASTEL
    brightness = (float) 1.0;
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float)(colorNum*4) / (float)numColors) % (float)numColors;
      saturation = ((float)(colorNum*2) / (float)numColors) % (float)numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_PASTEL, colorMap );
    controlPanel.addColor( COLORS_PASTEL );

    // COLORS_PSYCHEDELIC:
    saturation = (float) 1.0;
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float) (colorNum * 5) / (float) numColors) % numColors;
      brightness = ((float) (colorNum * 20) / (float) numColors) % numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_PSYCHEDELIC, colorMap );
    controlPanel.addColor( COLORS_PSYCHEDELIC );

    // COLORS_PURPLE_HAZE:
    red = 255;
    blue = 255;
    colorMap = new Color[ numColors ];
    for( int colorNum = numColors -1; colorNum >= 0; colorNum-- )
    {
      green = (int)((255*(float) colorNum / (float) numColors)) % 255;
      colorMap[ numColors - colorNum -1 ] = new Color( red, green, blue );
    }
    colorTable.put( COLORS_PURPLE_HAZE, colorMap );
    controlPanel.addColor( COLORS_PURPLE_HAZE );

    // COLORS_RADICAL:
    saturation = (float) 1.0;
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float) (colorNum * 7) / (float) numColors) % numColors;
      brightness = ((float) (colorNum * 49) / (float) numColors) % numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_RADICAL, colorMap );
    controlPanel.addColor( COLORS_RADICAL );

    // COLORS_RAINBOW:
    saturation = (float) 1.0;
    brightness = (float) 1.0;
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = (float) colorNum / (float) numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_RAINBOW, colorMap );
    controlPanel.addColor( COLORS_RAINBOW );
    setColor( COLORS_RAINBOW );  // default

    // COLORS_RAINBOWS:
    saturation = (float) 1.0;
    brightness = (float) 1.0;
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float) (colorNum * 5) / (float) numColors) % numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_RAINBOWS, colorMap );
    controlPanel.addColor( COLORS_RAINBOWS );

    // COLORS_SCINTILLATION
    brightness = (float) 1.0;
    saturation = (float) 1.0;
    colorMap = new Color[ numColors ];
     for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float)(colorNum*2) / (float)numColors) % (float)numColors;
      brightness = ((float)(colorNum*5) / (float)numColors) % (float)numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_SCINTILLATION, colorMap );
    controlPanel.addColor( COLORS_SCINTILLATION );

    // COLORS_WARPED:
    colorMap = new Color[ numColors ];
    for( int colorNum = numColors -1; colorNum >= 0; colorNum-- )
    {
      red = (int)((1024*(float) colorNum / (float) numColors)) % 255;
      green = (int)((256*(float) colorNum / (float) numColors)) % 255;
      blue = (int)((512*(float) colorNum / (float) numColors)) % 255;
      colorMap[ numColors - colorNum -1 ] = new Color( red, green, blue );
    }
    colorTable.put( COLORS_WARPED, colorMap );
    controlPanel.addColor( COLORS_WARPED );

    // COLORS_WILD:
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      hue = ((float)(colorNum*1) / (float)numColors) % (float)numColors;
      saturation = ((float)(colorNum*2) / (float)numColors) % (float)numColors;
      brightness = ((float)(colorNum*4) / (float)numColors) % (float)numColors;
      colorMap[ colorNum ] = new Color(
        Color.HSBtoRGB( hue, saturation, brightness ) );
    }
    colorTable.put( COLORS_WILD, colorMap );
    controlPanel.addColor( COLORS_WILD );

    // COLORS_ZEBRA:
    colorMap = new Color[ numColors ];
    for( int colorNum = 0; colorNum < numColors; colorNum++ )
    {
      if( colorNum % 2 == 0 )
      {
        colorMap[ colorNum ] = Color.white;;
      }
      else
      {
        colorMap[ colorNum ] = Color.black;;
      }
    }
    colorTable.put( COLORS_ZEBRA, colorMap );
    controlPanel.addColor( COLORS_ZEBRA );
  }

  private void initializeEventListeners()
  {
    // The drawingCanvas listens for mouse events.
    drawingCanvas.addMouseListener( drawingCanvas );
    drawingCanvas.addMouseMotionListener( drawingCanvas );
    controlPanel.initializeEventListeners();
    drawingCanvas.initializeEventListeners();
  }

  private void initializeGraphics()
  {
    // BorderLayout is the default layout strategy, but set it anyway.
    setLayout( new BorderLayout() );

    // DrawingCanvas holds the image, and controls the zoom rectangle graphics.
    drawingCanvas = new DrawingCanvas( this );

    // ControlPanel contains all graphics, and listens for control events.
    controlPanel = new ControlPanel( this, drawingCanvas );
    controlPanel.initializeGraphics();

    // This Applet is-a-kind-of Panel which is-a-kind-of Container.
    add( controlPanel, "Center" );
  }

  private void initializeNextPrevStacks()
  {
    // Only instances of Drawing are contained in these Stacks.
    previousStack = new Stack();
    nextStack = new Stack();
  }

  protected void juliaClicked( boolean isJulia )
  {
    // The user has clicked the Julia checkbox.
    boolean disableMouse = ( currentDrawing instanceof HelpDrawing ) ||
            ( isJulia && ! ( currentDrawing instanceof JuliaDrawing ) );
    drawingCanvas.disableMouse( disableMouse );
  }

  public static void main( String[] args )
  {
    // This is the entry point for the application version.
    isApplication = true;
    try
    {
      getApplicationParameters( args );

      // The application's top-level Container will be a Frame.
      Frame frame = new Frame( "Fractal Explorer" );

      // Put the Applet (which is-a-kind-of-Panel) inside the Frame...
      // Needs to be declared final for the annonomous inner class below.
      final Applet applet = new Fractal();

      frame.setLayout( new BorderLayout() );
      frame.add( applet, BorderLayout.CENTER );

      // Finish initializing the Frame...
      frame.setLocation( 0, 0 );

      // Required, to be able to click on the little x to kill the Frame.
      frame.addWindowListener(
        // Note the unusual syntax for an "annonomous inner class":
        new WindowAdapter()
        {
          public void windowClosing( WindowEvent event )
          {
            // The little x on the Frame has been clicked.
            System.out.println( event );
            applet.destroy();
            event.getWindow().dispose(); // garbage
            System.gc();
            System.exit( 0 );
          } } );

      // Initialize the Applet Component.
      // Note: Applet.init() gets called automatically for the Applet version.
      applet.init();

      // Minimize the size of the Frame, respecting the preferred sizes of
      // the various Components, if possible.
      frame.pack();

      // Bring the Frame to the screen.
      // Initiate the AWT event handling infrastructure (in another Thread).
      frame.show();
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (main) ... " + t );
    }
  }

  private FractalCalculator makeNewCalculator( Drawing d )
  {
    // Determine which Calculator to use for the new Drawing.
    // Example of the "Strategy" Design Pattern.
    FractalCalculator fc = null;

    if( useFastColorsCalculator() &&
        ! ( getCurrentDrawing() instanceof HelpDrawing ) )
    {
      fc = new FastColorsCalculator( this, d );
    }
    else if( d instanceof JuliaDrawing )
    {
      fc = new JuliaCalculator( this, d );
    }
    else
    {
      fc = new MandelbrotCalculator( this, d );
    }
    return fc;
  }

  private Drawing makeNewDrawing( ComplexRectangle newRect,
                                  IntWrapper maxIterations,
                                  ComplexPoint juliaPoint, String color )
  {
    Drawing drawing = null;
    Image image = drawingCanvas.createImage( imageWidth, imageHeight );

    if( controlPanel.isJulia() && ! ( currentDrawing instanceof JuliaDrawing ) )
    {
      // We're making a brand new Julia set at a new Julia Point.
      drawing = new JuliaDrawing( INITIAL_JULIA_RECT, INITIAL_ITERATIONS, image,
                                  null, color, juliaPoint );
      expandRectToFitImage( drawing.getComplexRect() );
      previousIterations = INITIAL_ITERATIONS;
    }
    else if( controlPanel.isJulia() )
    {
      drawing = new JuliaDrawing( newRect, maxIterations.getValue(), image,
                                  null, color, juliaPoint );
    }
    else
    {
      drawing = new Drawing( newRect, maxIterations.getValue(), image,
                             null, color );
    }
    controlPanel.updateDrawing( drawing );
    return drawing;
  }

  private void makeNewFractal( ComplexRectangle newRect,
                               IntWrapper maxIterations,
                               ComplexPoint juliaPoint, String color )
  {
    drawingNow = true; // We can only calculate one fractal at a time.

    controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious() );

    Drawing newDrawing = makeNewDrawing( newRect, maxIterations,
                                         juliaPoint, color );
    calculator = makeNewCalculator( newDrawing );

    maybeGuessMaxIterations( newDrawing );
    detectDeepZoom( newDrawing );

    // Spin off a new Thread to do the calculations.
    // Else, the rest of the UI would not work in parallel.
    // The Thread will call calculatorCallback() when done.
    // Various methods have to be 'synchronized' for this to work.
    Thread t = new Thread( calculator );
    t.setPriority( Thread.NORM_PRIORITY );
    t.setName( "Fractal Calculator" );
    t.start(); // Invokes calculator.run() in another thread.

    // The deeper we zoom, the longer the strings in the TextFields...
    recalculateLayout();
  }

  private void maybeGuessMaxIterations( Drawing d )
  {
    int maxIterations = d.getMaxIterations();
    // If the user did not change the number of iterations, make a guess...
    if( previousIterations == maxIterations && ! useFastColorsCalculator() )
    {
      maxIterations = guessNewMaxIterations( d instanceof JuliaDrawing );
      if( previousIterations != maxIterations )
      {
        // We have changed the num iterations.
        d.setMaxIterations( maxIterations );
        controlPanel.updateIterations( maxIterations );
        controlPanel.setParameterChangeFlag( true );
      }
    }
    previousIterations = maxIterations;
  }

  protected void outOfMemory( boolean oom )
  {
    if( oom )
    {
      String oomString = "Out of memory!  Please delete some drawings. ";
      setStatus( oomString );
      setStatus2( "Out of memory!" );
      System.out.println( " " );
      System.out.println( oomString );
    }
    outOfMemory = oom;
    controlPanel.setState( drawingNow, oom, hasNext(), hasPrevious() );
    System.gc();
  }

  protected static String pointAsString( ComplexPoint cp )
  {
    // Create a String representation of a complex point: "a + bi"
    String point = null;
    double cr = cp.getReal();
    double ci = cp.getImaginary();

    if( ci < 0.0 )
    {
      ci = Math.abs( ci );
      point = new String( doubleAsString( cr ) + " - " +
                          doubleAsString( ci ) + "i" );
    }
    else
    {
      point = new String( doubleAsString( cr ) + " + " +
                          doubleAsString( ci ) + "i" );
    }
    return point;
  }

  protected synchronized void recalculateLayout()
  {
    // Tell the outermost container to invalidate the current layout.
    validate();
    if( isApplication )
    {
      ((Frame)getParent()).validate();
      ((Frame)getParent()).pack();
    }
    repaint();
  }

  private void removePreviousHelpScreen()
  {
    // Java's Enumeration interface exemplifies the "Iterator" Design Pattern.
    // There should only ever be, at the most, one previous Help screen.
    Enumeration e = nextStack.elements();
    while( e.hasMoreElements() )
    {
      Drawing d = (Drawing) e.nextElement();
      if( d instanceof HelpDrawing )
      {
        nextStack.removeElement( d );
        return;
      }
    }
    e = previousStack.elements();
    while( e.hasMoreElements() )
    {
      Drawing d = (Drawing) e.nextElement();
      if( d instanceof HelpDrawing )
      {
        previousStack.removeElement( d );
        return;
      }
    }
  }

  protected synchronized void setColor( String color )
  {
    controlPanel.updateColor( color );
  }

  private void setCurrentDrawing( Drawing d )
  {
    currentDrawing = d;
    previousIterations = d.getMaxIterations();
    controlPanel.updateDrawing( d );
    controlPanel.setState( drawingNow, outOfMemory, hasNext(), hasPrevious() );
  }

  private void setCurrentZoom()
  {
    boolean hasZoom = drawingCanvas.hasZoom();

    if( hasZoom )
    {
      // Copy the Rectangle object since each Drawing needs its own zoom.
      currentDrawing.setZoom( new Rectangle( drawingCanvas.getZoom() ) );
    }
    else
    {
      currentDrawing.setZoom( null );
    }
  }

  protected void setStatus( String s )
  {
    controlPanel.setStatus( s );
  }

  protected void setStatus2( String s )
  {
    controlPanel.setStatus2( s );
  }

  protected void showPoint( int x, int y )
  {
    Drawing d = getCurrentDrawing();
    if( d == null || d instanceof HelpDrawing )
    {
      return;
    }
    if( x < 0 || x > imageWidth || y < 0 || y > imageHeight )
    {
      return;
    }
    setStatus( "Point: " + pointAsString( getComplexPoint( x, y ) ) );
  }

  public synchronized void stop()
  {
    // May be called by the Applet's browser.
    // Behave as if the user just pressed the "Stop" button.
    doStop();
    System.out.println( "Fractal stopped." );
  }

  protected void updateJuliaPoint( int x, int y )
  {
    showPoint( x, y );

    // Only update the Julia Set point from a mouse click
    // if the current drawing shows the Mandelbrot Set.
    if( currentDrawing instanceof JuliaDrawing ||
        currentDrawing instanceof HelpDrawing )
    {
      return;
    }
    if( x < 0 || x > imageWidth ||  y < 0 || y > imageHeight )
    {
      return;
    }
    controlPanel.updateJuliaPoint( getComplexPoint( x, y ) );
  }

  private boolean useFastColorsCalculator()
  {
    // Can we use the FastColorsCalculator performance optimization?
    // If the user has not changed any of the drawing parameters (except color),
    // then we can simply remap the colors instead of recalculating the fractal.
    if( ! hasNewParameters() )
    {
      if( controlPanel.isJulia() )
      {
        if( currentDrawing instanceof JuliaDrawing )
        {
          return true;
        }
        return false;
      }
      return true;
    }
    return false;
  }
}