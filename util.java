
class util {

  /* container class for miscellaneous util methods */


  public static final void dumpArray(byte b[], int start, int end) {
    for (int i = 1; i < start % 16; i++) {
      System.out.print("    ");
    }
    int i = start;
    while (i <= end) {
      if (i % 8 == 0) System.out.print(" ");
      if (i % 16 == 0) System.out.println("");
      System.out.print(Byte.toString(b[i])+" ");
      i++;
    }
    System.out.println("");
  }


  /* java should really make this a tad easier */
  public static final int intFromBytes(byte b1,  byte b2, byte b3, byte b4) {
    int i1, i2, i3, i4;
    i1 = b1; i2 = b2; i3 = b3; i4 = b4;
    return (((i1 << 24) & 0xff000000) |
	    ((i2 << 16) & 0x00ff0000) |
	    ((i3 << 8) & 0x0000ff00) |
	    (i4 & 0x000000ff)); 
  }	

  public static final long longFromBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
    long l1, l2, l3, l4, l5, l6, l7, l8;
    l1 = b1; l2 = b2; l3 = b3; l4 = b4; l5 = b5; l6 = b6; l7 = b7; l8 = b8;
    return (((l1 << 56) & 0xff00000000000000L) |
	    ((l2 << 48) & 0x00ff000000000000L) |
	    ((l3 << 40) & 0x0000ff0000000000L) |
	    ((l4 << 32) & 0x000000ff00000000L) |
	    ((l5 << 24) & 0x00000000ff000000L) |
	    ((l6 << 16) & 0x0000000000ff0000L) |
	    ((l7 <<  8) & 0x000000000000ff00L) |
	    (l8 &         0x00000000000000ffL));
    
    //    l1 = intFromBytes(b8, b7, b6, b5); if (l1 < 0) { l1 = l1 + 65536 ; }
    //l2 = intFromBytes(b4, b3, b2, b1); if (l2 < 0) { l2 = l2 + 65536 ; }
    //return (long) ((l1 << 32) | l2);
  }
    
  /* get the which'th byte of i, which should be [0..3] with 0 being LSB */
  public static final byte byteOfInt(int i, int which) {
    int shift = which * 8;
    return (byte) ((i & (0x000000FF << shift)) >> shift);
  }

  public static final byte byteOfLong(long i, int which) {
    int shift = which * 8;
    return (byte) (( i & (0x00000000000000FF <<shift)) >> shift);
  }

  public static final int intOfLong(long i, int whichbyte) {
    int shift = whichbyte * 8;
    return (int) (( i & (0x00000000000000FF << shift)) >> shift);
  }

  /* test routine */
  public static void main(String argv[]) {

    System.out.println("0xFFFFFFFF = " +
       Integer.toString(intFromBytes((byte) 0xFF, (byte) 0, (byte) 0, (byte) 0)) +
		       " (should be -2^31)");
    System.out.println("0x00000100 = "+ intFromBytes((byte)0, (byte)0, (byte)1, (byte)0));

    System.out.println(Integer.toString(byteOfInt(intFromBytes((byte)0xFF,(byte)0xFF,(byte)0xFF, (byte)0xFF),3)));
    System.out.println(Byte.toString((byte)0xFF));

  }

}





