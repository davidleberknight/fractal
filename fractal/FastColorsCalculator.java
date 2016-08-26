////////////////////////////////////////////////////////////////////////////////
// FastColorsCalculator Class //////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The FastColorsCalculator is a performance optimization.  The colorNumbers
// from the original Drawing are kept around, so that they can be used to
// quickly create a new image, when only the color scheme has changed.

package fractal;

import java.awt.Color;

class FastColorsCalculator extends FractalCalculator
{
  protected FastColorsCalculator( Fractal fractal, Drawing newDrawing )
  {
    super( fractal, newDrawing );
  }

  protected Color getColor( int x, int y )
  {
    Color c = Color.black;
    int colorNum = colorNumbers[ x ][ y ];
    if( colorNum != -1 )  // By convention, -1 indicates black.
    {
      c = colorMap[ colorNum ];
    }
    return c;
  }

  protected String getConsoleOutputString()
  {
    return new String( "Remapping colors from previously calculated fractal." );
  }

  protected int[][] getColorNumbers()
  {
    return fractal.getCurrentDrawing().getColorNumbers();
  }

  protected boolean maybeYieldOrStop( int loopCounter )
  {
    if( stopRequested ) // Did the user press the Stop button?
    {
      return false;
    }
    return true; // Don't yield.  Go fast!
  }

  protected int testPoint( double zR, double zI, int maxIterations )
  {
    // The code will NEVER get here. Nevertheless, this method must be provided
    // because it is declared to be abstract in the base class.
    return -1;
  }
}