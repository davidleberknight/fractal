////////////////////////////////////////////////////////////////////////////////
// JuliaDrawing Class //////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The JuliaDrawing class extends the (Mandelbrot) Drawing class to add an
// additional parameter: the complex juliaPoint.

package fractal;

import java.awt.*;
import fractal.utils.*;

class JuliaDrawing extends Drawing
{
  private ComplexPoint juliaPoint;

  protected JuliaDrawing( ComplexRectangle rect, int iterations, Image image,
    Rectangle zoom, String colors, ComplexPoint juliaPoint )
  {
    super( rect, iterations, image, zoom, colors );
    this.juliaPoint = juliaPoint;
  }

  protected void dump()
  {
    super.dump();
    System.out.println( "Julia Point = " + Fractal.pointAsString( juliaPoint ));
  }

  protected String getConsoleOutputString()
  {
    return new String( "Julia Set Drawing:" );
  }

  protected ComplexPoint getJuliaPoint()
  {
    return juliaPoint;
  }
}
