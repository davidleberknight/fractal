////////////////////////////////////////////////////////////////////////////////
// IntWrapper Class ////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
// Copyright 2000 - David Leberknight - Anyone may use this code for any reason
// at any time, provided that they give an appropriate reference to this source,
// and send David some email ( david@leberknight.com ).
//
// The class Integer is immutable; this isn't.
// The primative int is passed by value; this is passed by reference.

package fractal.utils;

public class IntWrapper
{
  private int theValue;

  public IntWrapper( int i )
  {
    theValue = i;
  }

  public void setValue( int i )
  {
    theValue = i;
  }

  public int getValue()
  {
    return theValue;
  }

  public boolean equals( int i )
  {
    if( theValue == i )
    {
      return true;
    }
    return false;
  }
}