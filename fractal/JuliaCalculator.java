////////////////////////////////////////////////////////////////////////////////
// JuliaCalculator Class ///////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// JuliaCalculators compute Julia Set images. For every complex number c,
// there is a different Julia Set.

package fractal;

class JuliaCalculator extends FractalCalculator
{
  private double  cR = 0.0; // Real
  private double  cI = 0.0; // Imaginary

  protected JuliaCalculator( Fractal fractal, Drawing newDrawing )
  {
    super( fractal, newDrawing );
    if( newDrawing instanceof JuliaDrawing )
    {
      cR = ((JuliaDrawing) newDrawing).getJuliaPoint().getReal();
      cI = ((JuliaDrawing) newDrawing).getJuliaPoint().getImaginary();
    }
    // else the point will be (0,0) and the calculator will generate a circle.
  }

  protected int testPoint( double zR, double zI, int maxIterations )
  {
    // Is the given complex point, (zR, zI), in the Julia set?
    // Use the formula: z <= z*z + c, where z is the point being tested,
    // and c is the Julia Set constant.
    // If |z| >= 2, then the point is not in the set.
    // Return 0 if the point is in the set; else return the number of
    // iterations it took to decide that the point is not in the set.
    for( int i = 1; i <= maxIterations; i++ )
    {
       double zROld = zR;
       // To square a complex number: (a+bi)(a+bi) = a*a - b*b + 2abi
       zR = zR * zR - zI * zI + cR;
       zI = 2 * zROld * zI + cI;
       // We know that if the distance from z to the origin is >= 2
       // then the point is out of the set.  To avoid a square root,
       // we'll instead check if the distance squared >= 4.
       double distSquared = zR * zR + zI * zI;
       if( distSquared >= 4 )
       {
          return i;
       }
    }
    return 0;
  }
}
