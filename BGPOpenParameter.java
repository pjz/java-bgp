


import java.net.*;
import java.io.*;



  class BGPOpenParameter extends Object {
    public int type;
    public static int TYPE_AUTH = 1;
    public byte[] value;

    public int length() {
      return 2 + value.length;
    }

    public void toBytes(byte b[], int offset) {
      b[offset] = (byte) type;
      b[offset +1] = (byte) value.length;
      System.arraycopy(value, 0, b, offset+2, value.length);
    }

    public void fromBytes(byte b[], int offset) {
      type = b[offset];
      int valuelen = b[offset+1];
      value = new byte[valuelen];
      System.arraycopy(b, offset+2, value, 0, valuelen);
    }

  }








