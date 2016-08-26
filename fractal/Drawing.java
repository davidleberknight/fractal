////////////////////////////////////////////////////////////////////////////////
// Drawing Class ///////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// Drawing is a simple class to hold Drawing data. There are three kinds of
// Drawings: Mandelbrot, Julia, and Help.  This class is not abstract, but
// rather, holds the data to define a Mandelbrot Drawing.

package fractal;

import java.awt.*;
import fractal.utils.*;

class Drawing
{
  protected int[][]           colorNumbers; // Used by the FastColorsCalculator.
  protected String            color;
  protected ComplexRectangle  complexRect;
  protected Image             image;
  protected int               maxIterations;
  protected Rectangle         zoom;

  protected Drawing( ComplexRectangle complexRect, int maxIterations,
    Image image, Rectangle zoom, String color )
  {
    this.complexRect = complexRect;
    this.maxIterations = maxIterations;
    this.image = image;
    this.zoom = zoom;
    this.color = color;
    colorNumbers = null;
  }

  protected void dump()
  {
    System.out.println( getConsoleOutputString() );
    System.out.println( "Max Iterations = " + maxIterations );
    System.out.println( "Color scheme = " + color );
    System.out.println( "Real Min = " +
      Fractal.doubleAsString( complexRect.getRMin() ));
    System.out.println( "Real Max = " +
      Fractal.doubleAsString( complexRect.getRMax() ));
    System.out.println( "Imaginary Min = " +
      Fractal.doubleAsString( complexRect.getIMin() ));
    System.out.println( "Imaginary Max = " +
      Fractal.doubleAsString( complexRect.getIMax() ));
  }

  public void finalize()
  {
    // Called automatically by the system's garbage collector.
    // This helps free up resources quickly when a drawing gets deleted.
    image.flush();
  }

  protected int[][] getColorNumbers()
  {
    return colorNumbers;
  }

  protected String getColor()
  {
    return color;
  }

  protected ComplexRectangle getComplexRect()
  {
    return complexRect;
  }

  protected String getConsoleOutputString()
  {
    return new String( "Mandelbrot Set Drawing:" );
  }

  protected Image getImage()
  {
    return image;
  }

  protected int getMaxIterations()
  {
    return maxIterations;
  }

  protected Rectangle getZoom()
  {
    return zoom;
  }

  protected boolean hasZoom()
  {
    return ( zoom != null );
  }

  protected void setComplexRect( ComplexRectangle cr )
  {
    complexRect = cr;
  }

  protected void setImage( Image i )
  {
    image = i;
  }

  protected void setColorNumbers( int[][] colors )
  {
    colorNumbers = colors;
  }

  protected void setMaxIterations( int mi )
  {
    maxIterations = mi;
  }

  protected void setZoom( Rectangle z )
  {
    zoom = z;
  }
}
