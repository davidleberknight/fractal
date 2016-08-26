////////////////////////////////////////////////////////////////////////////////
// ComplexPoint Class //////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The ComplexPoint class holds an imaginary (complex) point.

package fractal.utils;

public class ComplexPoint
{
  private double real;
  private double imaginary;

  public ComplexPoint( double real, double imaginary )
  {
    this.real = real;
    this.imaginary = imaginary;
  }

  public ComplexPoint()
  {
    real = 0.0;
    imaginary = 0.0;
  }

  public double getImaginary()
  {
    return imaginary;
  }

  public double getReal()
  {
    return real;
  }
  
  public void set( ComplexPoint cp )
  {
    real = cp.getReal();
    imaginary = cp.getImaginary();
  }

  public void set( double cr, double ci )
  {
    real = cr;
    imaginary = ci;
  }
}