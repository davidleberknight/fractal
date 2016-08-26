////////////////////////////////////////////////////////////////////////////////
// FractalCalculator Class /////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// FractalCalculator is an abstract class generalizing all fractal calculators.
// This class uses the "Template Method" Design Pattern; the entire algorithm
// for creating a new fractal image is defined in calcFractal(), which uses
// getColor(), which in turn uses the abstract method testPoint(), which is
// implemented differently by MandelbrotCalculator and JuliaClaculator.
// The method getColor() is overriden by FastColorsCalculator.

package fractal;

import java.awt.*;
import fractal.utils.*;

abstract class FractalCalculator implements Runnable
{
  protected Color[]           colorMap;
  protected int[][]           colorNumbers;
  protected double            delta;
  protected Fractal           fractal;
  protected Image             image;
  protected double            iRangeMax;
  protected double            iRangeMin;
  protected int               maxIterations;
  protected Drawing           newDrawing;
  protected ComplexRectangle  newRect;
  protected int               numColors;
  protected int               imageHeight;
  protected int               imageWidth;
  protected double            rRangeMax;
  protected double            rRangeMin;
  protected boolean           stopRequested;

  protected FractalCalculator( Fractal fractal, Drawing newDrawing )
  {
    this.fractal    = fractal;
    this.newDrawing = newDrawing;
    image           = newDrawing.getImage();
    maxIterations   = newDrawing.getMaxIterations();
    newRect         = newDrawing.getComplexRect();
    imageWidth      = fractal.getImageWidth();
    imageHeight     = fractal.getImageHeight();
    colorMap        = fractal.getCurrentColorMap();
    numColors       = colorMap.length;
    rRangeMin       = newRect.getRMin();
    rRangeMax       = newRect.getRMax();
    iRangeMin       = newRect.getIMin();
    iRangeMax       = newRect.getIMax();
    delta           = (rRangeMax - rRangeMin) / (double) imageWidth;
    colorNumbers    = null; // set this up later.
    stopRequested   = false;
  }

  private boolean calcFractal()
  {
    // Assign a color to every pixel ( x , y ) in the Image, corresponding to
    // one point, z, in the imaginary plane ( zr, zi ).
    Graphics imageGraphics = null;
    try
    {
      colorNumbers = getColorNumbers();
      fractal.setStatus2( " 0% Complete." );

      // Get the Graphics object for the Image. This is necessary
      // for "double buffering" of the calculated image.
      imageGraphics = image.getGraphics();
      imageGraphics.setPaintMode();

      // For each pixel...
      int loopCounter = 0;
      for( int x = 0; x < imageWidth; x++ )
      {
        for( int y = 0; y < imageHeight; y++ )
        {
          Color c = getColor( x, y );

          imageGraphics.setColor( c );
          imageGraphics.drawLine( x, y, x, y );

          if( ! maybeYieldOrStop( ++loopCounter ) )
          {
            return false; // stop was requested.
          }
        }
        fractal.setStatus2( " " + 100 * x / imageWidth + "% Complete." );
      }
      newDrawing.setColorNumbers( colorNumbers );
      fractal.setStatus2( " 100% Complete." );
      return true;
    }
    catch( OutOfMemoryError oom )
    {
      fractal.outOfMemory( true );
      return false;
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (calc fractal) ... " + t );
      return false;
    }
    finally
    {
      if( imageGraphics != null )
      {
        imageGraphics.dispose(); // garbage.
      }
    }
  }

  protected Color getColor( int x, int y )
  {
    Color c = Color.black;
    colorNumbers[ x ][ y ] = -1; // -1 indicates black.
    double zR = rRangeMin + ((double) x ) * delta;
    double zI = iRangeMin + ((double)( imageHeight - y )) * delta;

    // Is the point inside the set?
    int numIterations = testPoint( zR, zI, maxIterations );
    
    if( numIterations != 0 )
    {
       // The point is outside the set. It gets a color based on the number
       // of iterations it took to know this.
       int colorNum = (int)((float) numColors * ( 1.0 -
                     (float) numIterations / (float) maxIterations ));
       colorNum = (colorNum == numColors) ? 0 : colorNum;

       // Save this information, to the slight detriment of this calculator's
       // speed, in order to greatly increase the performance for creating
       // future Drawings, when only the color scheme has been changed.
       colorNumbers[ x ][ y ] = colorNum;
       c = colorMap[ colorNum ];
    }
    return c;
  }

  protected int[][] getColorNumbers()
  {
    // Beware: out of memory!  Save the colorNumber data with the drawing
    // in order to be able to use the FastColorsCalculator later on.
    return new int[ imageWidth ][ imageHeight ];
  }

  protected String getConsoleOutputString()
  {
    return new String( "Calculating new fractal." );
  }

  protected boolean maybeYieldOrStop( int loopCounter )
  {
    // Improve the response time for the UI (to the slight detriment of this
    // Thread's speed). Suggest to the Virtual Machine's Thread scheduler that
    // it yield processing to other Threads.
    if( loopCounter % 2048 == 0 )
    {
      Thread.yield(); // Needed for UI performance on some browsers.

      if( stopRequested ) // Did the user press the Stop button?
      {
        return false;
      }
    }
    return true;
  }

  public void run()
  {
    // This is the entry point for the new Thread, called after
    // the parent Thread calls Thread.start().
    try
    {
      System.out.println( " " );
      System.out.println( "Starting new drawing: " );
      System.out.println( "Zoom factor: " + (long)fractal.getZoomFactor() );
      newDrawing.dump();
      System.out.println( getConsoleOutputString() );

      if( calcFractal() )
      {
        System.out.println( "Drawing completed." );
        fractal.calculatorCallback( true, newDrawing );
      }
      else
      {
        // Drawing stopped for some reason or another.
        System.out.println( "Drawing stopped." );
        fractal.calculatorCallback( false, null );
      }
    }
    catch( OutOfMemoryError oom )
    {
      fractal.outOfMemory( true );
      fractal.calculatorCallback( false, null );
    }
    catch( Throwable t )
    {
      System.out.println( "Fractal ERROR !!! (calculator) ... " + t );
      fractal.calculatorCallback( false, null );
    }
  }

  protected void stop()
  {
    stopRequested = true;
  }

  // Subclasses must implement this method:
  protected abstract int testPoint( double r, double i, int maxIterations );
}