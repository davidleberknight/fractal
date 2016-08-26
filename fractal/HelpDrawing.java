////////////////////////////////////////////////////////////////////////////////
// HelpDrawing Class ///////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
// The (pseudo-Singleton) HelpDrawing doesn't allow zoom rectangle graphics.

package fractal;

import java.awt.*;
import fractal.utils.*;

class HelpDrawing extends Drawing
{
  private String welcome = null;

  protected HelpDrawing( String bemVindo, ComplexRectangle cr, int mi,
                         Image image, String colors )
  {
    super( cr, mi, image, null, colors );

    welcome = bemVindo;

    Graphics g = image.getGraphics( );
    g.setPaintMode( );

    int lineNum = 20;
    int delta = 25;
    int delta2 = 20;

    g.setFont( new Font( "TimesRoman", Font.BOLD, 18 ) );
    g.drawString( welcome, 1, lineNum );
    g.drawString( "Written by David Leberknight.", 1, lineNum += delta );
    g.drawString( "Last update: February 28th, 2001 ( version 2.9 ).",
                   1, lineNum += delta );
    lineNum += delta2;
    g.drawString( "Use the mouse to draw a rectangle for zooming.",
                  1, lineNum += delta );
    g.drawString( "The more you zoom, the more iterations you'll need.",
                  1, lineNum += delta );
    g.drawString( "More iterations make the image more detailed,",
                  1, lineNum += delta );
    g.drawString( "but it takes longer to compute.", 1, lineNum += delta );
    g.drawString( "Also, the number of iterations affects the color.",
                  1, lineNum += delta );
    g.drawString( "You may change the default number of iterations.",
                  1, lineNum += delta );
    lineNum += delta2;
    g.drawString( "To draw a Julia Set, check the Draw Julia Set checkbox,",
                  1, lineNum += delta );
    g.drawString( "and then select a Julia Set point by clicking the mouse",
                  1, lineNum += delta );
    g.drawString( "over a point in a Mandelbrot Set image.",
                  1, lineNum += delta );
    g.drawString( "Different points create different Julia Sets.",
                  1, lineNum += delta );
    g.drawString( "You may also use the keyboard to type image parameters.",
                  1, lineNum += delta );
    lineNum += delta2;
    g.drawString( "Try using different color mappings too.",
                  1, lineNum += delta );
    g.drawString( "Have fun exploring the wonderful world of fractals !!!",
                  1, lineNum += delta );
    g.dispose(); // garbage
  }

  protected void dump()
  {
    System.out.println( " " );
    System.out.println( "Help has been requested..." );
  }
}