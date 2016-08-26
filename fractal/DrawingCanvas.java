////////////////////////////////////////////////////////////////////////////////
// DrawingCanvas Class /////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The DrawingCanvas class is responsible for painting the image.
// It also handles the mouse events and the zoom rectangle graphics.

package fractal;

import java.awt.*;
import java.awt.event.*;

class DrawingCanvas extends Canvas
                    implements MouseListener, MouseMotionListener
{
  private Fractal        fractal;

  // The state required for the zoom rectangle graphics:
  private Rectangle      lastZoom;
  private Rectangle      zoom;
  private boolean        hasZoom;
  private boolean        dragInterrupted;
  private boolean        mouseDisabled;
  private boolean        mouseOverCanvas;
  private int            mouseDown; // There's more than one mouse button.

  // The initial mouse press point (corner) for a zoom rectangle.
  private int            x1;
  private int            y1;

  // The very first call to paint() has special (initialization) behavior.
  private boolean        initialScreen = true;

  // Used by the layout manager to determine how big to make the canvas:
  private Dimension      preferredSize = null;

  protected DrawingCanvas( Fractal fractal )
  {
    this.fractal = fractal;
    zoom = new Rectangle();
    lastZoom = new Rectangle();
    dragInterrupted = false;
    hasZoom = false;
    initialScreen = true;
    preferredSize = null;
    mouseDisabled = true;
    mouseOverCanvas = true;
    mouseDown = 0;
    x1 = 0;
    y1 = 0;
  }

  protected void disableMouse( boolean disableMouse )
  {
    mouseDisabled = disableMouse;
  }

  public Dimension getMaximumSize()
  {
    // Called by the AWT.
    return getPreferredSize();
  }

  public Dimension getMinimumSize()
  {
    // Called by the AWT.
    return getPreferredSize();
  }

  public Dimension getPreferredSize()
  {
    // Called by the AWT.  There is no getPreferredSize(). (Swing fixes this.)
    // Overriding getPreferredSize() is the only way to set the size.
    if( preferredSize == null )
    {
      preferredSize = new Dimension( fractal.getImageWidth(),
                                     fractal.getImageHeight() );
    }
    return preferredSize;
  }

  protected Rectangle getZoom()
  {
    return zoom;
  }

  protected boolean hasZoom()
  {
     return hasZoom;
  }

  protected void initializeEventListeners()
  {
    // Listen for the return key, which should always Draw.
    // The return key is a KeyEvent for Canvases.
    addKeyListener( new ReturnKeyHandler() );
  }

  protected void interruptZoomDrag()
  {
    // This is needed because the user can be in the middle of drawing a zoom
    // rectangle exactly when a new fractal gets painted, asynchronously.
    if( mouseDown == 0 ) // There is more than one mouse button.
    {
      return;
    }

    dragInterrupted = true;
    // Note: the mouse is still down, so the mouse listener code must
    // now behave as if the mouse were just pressed, to began tracking
    // a new zoom rect, which might be over a new drawing...
  }

  public void paint( Graphics g )
  {
    // Display the canvas' current graphic.  Called by the AWT.
    synchronized( fractal )
    {
      try
      {
        if( initialScreen == true )
        {
          fractal.firstPaint();
          initialScreen = false;
        }
        Drawing current = fractal.getCurrentDrawing();
        Image currentImage = current.getImage();
        if( currentImage != null )
        {
          g.drawImage( currentImage, 0, 0, null );
        }
      }
      catch( OutOfMemoryError oom )
      {
        fractal.outOfMemory( true );
      }
      catch( Throwable t )
      {
        System.out.println( "Fractal ERROR !!! (paint) ... " + t );
      }
    }
  }

  protected void redraw( Drawing d )
  {
    if( d.hasZoom() )
    {
      hasZoom = true;
      Rectangle bounds = d.getZoom();
      lastZoom.setBounds( bounds );
      zoom.setBounds( bounds );
    }
    else
    {
      hasZoom = false;
    }

    // The HelpDrawing screen does not allow mouse events.
    // Mouse events also get disabled when the current drawing is Mandelbrot
    // and the Draw Julia Set checkbox is checked.
    mouseDisabled = ( d instanceof HelpDrawing );

    repaint(); // Schedules a call to paint() in another Thread.
  }

  public void update( Graphics g )
  {
    // This helps prevent flickering.  Called by the AWT.
    // Override the default Component.update()
    paint( g );
  }

  //////////////////////////
  // MOUSE handling methods:
  //////////////////////////

  private void drawXORRectangle( Graphics g, Rectangle rect )
  {
    // This is used only by the mouse event handling methods.
    g.setXORMode( Color.white );

    int x = rect.getBounds().x;
    int y = rect.getBounds().y;
    int width = rect.getBounds().width;
    int height = rect.getBounds().height;

    g.drawRect( x, y, width, height );
    g.setPaintMode();
  }

  private Rectangle makeRectangle( int x1, int x2, int y1, int y2 )
  {
    // This is used only by the mouse event handling methods.
    // Determine the upper left x,y plus the width,height.
    int x_ul = 0;
    int y_ul = 0;
    int width = 0;
    int height = 0;

    if( x1 > x2 )
    {
      x_ul = x2;
      width = x1 - x2;
    }
    else
    {
      x_ul = x1;
      width = x2 - x1;
    }
    if( y1 > y2 )
    {
      y_ul = y2;
      height = y1 - y2;
    }
    else
    {
      y_ul = y1;
      height = y2 - y1;
    }
    return new Rectangle( x_ul, y_ul, width, height );
  }

  public void mouseClicked( MouseEvent e )
  {
    // Called by AWT for all registered MouseListener objects.
    // On all platforms (?) this event is preceded by press and release events.
    synchronized( fractal )
    {
      if( mouseOverCanvas )
      {
        fractal.updateJuliaPoint( e.getX(), e.getY() );
      }
    }
  }

  public void mouseDragged( MouseEvent e )
  {
    // Called by AWT for all registered MouseMotionListener objects.
    Graphics imageGraphics = null;
    Graphics canvasGraphics = null;

    synchronized( fractal )
    {
      try
      {
        if( mouseOverCanvas )
        {
          fractal.updateJuliaPoint( e.getX(), e.getY() );
        }

        if( ! mouseOverCanvas || mouseDisabled )
        {
          return;
        }

        // Update the Canvas image to show the drag rectangle.
        Image currentImage = fractal.getCurrentDrawing().getImage();
        imageGraphics = currentImage.getGraphics();
        canvasGraphics = this.getGraphics();

        if( dragInterrupted )
        {
          // The drag has been interrupted.
          // Begin a new zoom rectangle now.
          // x1,y1 are the first corner of the new zoom rectangle.
          x1 = e.getX();
          y1 = e.getY();
          dragInterrupted = false;
        }

        if( hasZoom )
        {
          lastZoom.setBounds( zoom );
          hasZoom = false;

          // UnDraw the previous rectangle using XOR mode
          drawXORRectangle( imageGraphics, lastZoom );
        }

        int xCurrent = e.getX();
        int yCurrent = e.getY();

        hasZoom = true;
        zoom.setBounds( makeRectangle( x1, xCurrent, y1, yCurrent ));
        drawXORRectangle( imageGraphics, zoom );
        canvasGraphics.drawImage( currentImage, 0, 0, null );
      }
      catch( OutOfMemoryError oom )
      {
        fractal.outOfMemory( true );
      }
      catch( Throwable t )
      {
        System.out.println( "Fractal ERROR !!! (mouse drag) ... " + t );
      }
      finally
      {
        if( canvasGraphics != null )
        {
          canvasGraphics.dispose(); // garbage.
        }
        if( imageGraphics != null )
        {
          imageGraphics.dispose(); // garbage.
        }
      }
    }
  }

  public void mouseEntered( MouseEvent e )
  {
    // Called by AWT for all registered MouseListener objects.
    mouseOverCanvas = true;
  }

  public void mouseExited( MouseEvent e )
  {
    // Called by AWT for all registered MouseListener objects.
    mouseOverCanvas = false;
  }

  public void mouseMoved( MouseEvent e )
  {
    // Called by AWT for all registered MouseMotionListener objects.
    synchronized( fractal )
    {
      if( mouseOverCanvas )
      {
        fractal.showPoint( e.getX(), e.getY() );
      }
    }
  }

  public void mousePressed( MouseEvent e )
  {
    // Called by AWT for all registered MouseListener objects.
    Graphics imageGraphics = null;
    Graphics canvasGraphics = null;

    synchronized( fractal )
    {
      try
      {
        mouseDown++;
        fractal.setStatus( " " );
        fractal.setStatus2( " " );

        if( mouseDown > 1 )
        {
          return; // Another mouse button is already down.
        }

        dragInterrupted = false;

        if( mouseDisabled )
        {
          return;
        }

        if( hasZoom )
        {
          // UnDraw the previous rectangle using XOR mode.
          // This will revert the graphics back to the original
          // colors before the rectangle was ever drawn.
          hasZoom = false;
          Image currentImage = fractal.getCurrentDrawing().getImage();
          imageGraphics = currentImage.getGraphics();
          canvasGraphics = this.getGraphics();
          drawXORRectangle( imageGraphics, zoom );
          canvasGraphics.drawImage( currentImage, 0, 0, null );
        }

        if( ! mouseOverCanvas )
        {
          return;
        }

        // x1,y1 are the first corner of the new zoom rectangle.
        x1 = e.getX();
        y1 = e.getY();
      }
      catch( OutOfMemoryError oom )
      {
        fractal.outOfMemory( true );
      }
      catch( Throwable t )
      {
        System.out.println( "Fractal ERROR !!! (mouse press) ... " + t );
      }
      finally
      {
        if( canvasGraphics != null )
        {
          canvasGraphics.dispose(); // garbage.
        }
        if( imageGraphics != null )
        {
          imageGraphics.dispose(); // garbage.
        }
      }
    }
  }

  public void mouseReleased( MouseEvent e )
  {
    // Called by AWT for all registered MouseListener objects.
    Graphics imageGraphics = null;
    Graphics canvasGraphics = null;

    synchronized( fractal )
    {
      try
      {
        mouseDown--;
        if( mouseDown < 0 )
        {
          mouseDown = 0; // In case the mouse was down upon construction.
        }
        fractal.setStatus( " " );
        fractal.setStatus2( " " );

        if( mouseOverCanvas )
        {
          fractal.updateJuliaPoint( e.getX(), e.getY() );
        }

        if( mouseDisabled )
        {
          dragInterrupted = false;
          return;
        }

        if( dragInterrupted )
        {
          dragInterrupted = false;
          hasZoom = false;
          return;
        }

        // Get the Graphics objects.
        Image currentImage = fractal.getCurrentDrawing().getImage();
        imageGraphics = currentImage.getGraphics();
        canvasGraphics = this.getGraphics();

        if( hasZoom )
        {
          lastZoom.setBounds( zoom );

          // UnDraw the previous rectangle using XOR mode
          drawXORRectangle( imageGraphics, lastZoom );
        }
        else
        {
          // There is no zoom defined, yet.
          if( mouseOverCanvas )
          {
            // x2,y2 are the coordinates of a corner of the rectangle.
            int x2 = e.getX();
            int y2 = e.getY();

            zoom.setBounds( makeRectangle( x1, x2, y1, y2 ) );
          }
          else
          {
            return; // No zoom.
          }
        }

        if( zoom.getBounds().width < 3 || zoom.getBounds().height  < 3 )
        {
          fractal.setStatus( " Warning: the zoom rectangle is too small." );
          hasZoom = false;
        }
        else
        {
          hasZoom = true;
          fractal.setStatus( " " );
          drawXORRectangle( imageGraphics, zoom );
        }
        canvasGraphics.drawImage( currentImage, 0, 0, null );
      }
      catch( OutOfMemoryError oom )
      {
        fractal.outOfMemory( true );
      }
      catch( Throwable t )
      {
        System.out.println( "Fractal ERROR !!! (mouse release) ... " + t );
      }
      finally
      {
        if( canvasGraphics != null )
        {
          canvasGraphics.dispose(); // garbage.
        }
        if( imageGraphics != null )
        {
          imageGraphics.dispose(); // garbage.
        }
      }
    }
  }

  class ReturnKeyHandler extends KeyAdapter
  {
    // This Inner Class is used to listen for the return key being pressed
    // when this Canvas has the focus.  Doesn't work on some JVMs.
    public void keyTyped( KeyEvent e )
    {
      char theKey = e.getKeyChar();
      if( theKey == '\r' ) // return and/or enter
      {
        fractal.doDraw();
      }
    }
  }
}