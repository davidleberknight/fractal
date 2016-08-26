////////////////////////////////////////////////////////////////////////////////
// ComplexRectangle Class //////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The ComplexRectangle class holds a rectangle of complex coordinates.

package fractal.utils;

import java.awt.Rectangle;

public class ComplexRectangle
{
  private double iMin; // imaginary
  private double iMax;
  private double rMin; // real
  private double rMax;

  public ComplexRectangle( double r1, double r2, double i1, double i2 )
  {
    set( r1, r2, i1, i2 );
  }

  public ComplexRectangle()
  {
    set( 0.0, 0.0, 0.0, 0.0 );
  }

  public ComplexRectangle( ComplexRectangle cr )
  {
    set( cr );
  }

  public double getIMin()
  {
    return iMin;
  }

  public double getIMax()
  {
    return iMax;
  }

  public double getRMin()
  {
    return rMin;
  }

  public double getRMax()
  {
    return rMax;
  }

  public double getHeight()
  {
    return iMax - iMin;
  }

  public double getWidth()
  {
    return rMax - rMin;
  }

  public void set( ComplexRectangle cr )
  {
    set( cr.getRMin(), cr.getRMax(), cr.getIMin(), cr.getIMax() );
  }

  public void set( ComplexPoint p1, ComplexPoint p2 )
  {
    set( p1.getReal(), p2.getReal(), p1.getImaginary(), p2.getImaginary() );
  }

  public void set( double r1, double r2, double i1, double i2 )
  {
    if( r1 > r2 )
    {
      rMin = r2;
      rMax = r1;
    }
    else
    {
      rMin = r1;
      rMax = r2;
    }
    if( i1 > i2 )
    {
      iMin = i2;
      iMax = i1;
    }
    else
    {
      iMin = i1;
      iMax = i2;
    }
  }
}