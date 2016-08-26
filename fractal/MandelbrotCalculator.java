////////////////////////////////////////////////////////////////////////////////
// MandelbrotCalculator Class //////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// MandelbrotCalculators compute Mandelbrot Set images.

package fractal;

class MandelbrotCalculator extends FractalCalculator
{
  protected MandelbrotCalculator( Fractal fractal, Drawing newDrawing )
  {
    super( fractal, newDrawing );
  }

  protected int testPoint( double cR, double cI, int maxIterations )
  {
    // Is the given complex point, (cR, cI), in the Mandelbrot set?
    // Use the formula: z <= z*z + c, where z is initially equal to c.
    // If |z| >= 2, then the point is not in the set.
    // Return 0 if the point is in the set; else return the number of
    // iterations it took to decide that the point is not in the set.
    double zR = cR;
    double zI = cI;

    for( int i = 1; i <= maxIterations; i++ )
    {
       // To square a complex number: (a+bi)(a+bi) = a*a - b*b + 2abi
       double zROld = zR;
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