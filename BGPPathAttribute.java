
class BGPPathAttribute {

  public boolean optional;
  public boolean transitive;
  public boolean partial;

  /* Note that while we correctly identify the known types, we don't make
     handling them particularly easy. TYPE_AS_PATH, for instance, implies 
     a Vector of AS path segments */  

  public int type;
  public static int TYPE_ORIGIN = 1;
  public static int TYPE_AS_PATH = 2;
  public static int TYPE_NEXT_HOP = 3;
  public static int TYPE_MULTI_EXIT_DISC = 4;
  public static int TYPE_LOCAL_PREF = 5;
  public static int TYPE_ATOMIC_AGGREGATE = 6;
  public static int TYPE_AGGREGATORT = 7;

  public int length; // data.length + flag byte + type byte + length byte(s)
  public byte data[];

  public void fromBytes(byte b[], int offset) {
    optional =   ((b[offset] & 0x80) != 0);
    transitive = ((b[offset] & 0x40) != 0);
    partial =    ((b[offset] & 0x20) != 0);
    type = b[offset+1];
    if ((b[offset] & 0x10) != 0) { // extended length bit set
      length = util.intFromBytes((byte)0, (byte)0, b[offset+2], b[offset+3]);
      data = new byte[length];
      System.arraycopy(b, offset+4, data, 0, length);
      length = length + 4; 
    } else {                       // extended length bit NOT set
      length = util.intFromBytes((byte)0, (byte)0, (byte)0, b[offset+2]);
      data = new byte[length];
      System.arraycopy(b, offset+3, data, 0, length);
      length = length + 3; 
    }
  }

  public void toBytes (byte b[], int offset) {
    byte flags = 0;
    if (optional) {
      flags = (byte) (flags | 0x80);
    } 
    if (transitive) {
      flags = (byte) (flags | 0x40);
    }	
    if (partial) {
      flags = (byte) (flags | 0x20);
    }
    if (length > 255) {
      flags = (byte) (flags | 0x10);
    }
    b[offset+1] = (byte) type;
    if (length > 255) {
      b[offset+2] = util.byteOfInt(length, 1);
      b[offset+3] = util.byteOfInt(length, 0);
      System.arraycopy(data, 0, b, offset+4, length);
    } else {
      b[offset+2] = (byte) length;
      System.arraycopy(data, 0, b, offset+3, length);
    }
  }

}


    





