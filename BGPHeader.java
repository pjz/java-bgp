

import java.net.*;
import java.io.*;



class BGPHeader extends Object implements Cloneable {

  public byte marker[];
  public int packetlength;
  public static int length = 19; /* static header size */
  public static int MIN_PACKETLENGTH = length; /* smallest packet is just a header */
  public static int MAX_PACKETLENGTH = 4096;
  public byte type;   
  public static byte TYPE_OPEN = 1;
  public static byte TYPE_UPDATE = 2;
  public static byte TYPE_NOTIFICATION = 3;
  public static byte TYPE_KEEPALIVE = 4;
  
  public static boolean debug = false;
  
  BGPHeader() {
    marker = new byte[16];
    /* init to all 1's */
    for (int i = 0; i < marker.length; i++) {
      marker[i] = (byte) 0xff;
    }
    packetlength = length;
  }
  
  BGPHeader(DataInputStream in) throws IOException {
    byte headerbuffer[] = new byte[length];
    marker = new byte[16];
    if (debug) System.out.println("HEADER: waiting for data ("+Integer.toString(length)+" bytes)");
          in.readFully(headerbuffer, 0, length);
    //int n = in.read(headerbuffer, 0, length); // test to see if we're getting *anything*
    //System.out.println("HEADER: read "+Integer.toString(n)+" bytes.");
    
    if (debug) System.out.println("HEADER: received data");
    fromBytes(headerbuffer, 0);
    if (debug) System.out.println(this.toString());
  }
  
  public void writeTo(OutputStream out) throws IOException {
    byte outbuffer[] = new byte[length];
    toBytes(outbuffer, 0);
    if (debug) System.out.println("Sending "+toString());
    out.write(outbuffer);
    out.flush();
  }
  
  public String toString() {
    return "HEADER: length: "+Integer.toString(packetlength)+" type: "+Byte.toString(type);
  }
  
  public void toBytes(byte headbuffer[], int offset) {
    System.arraycopy(marker, 0, headbuffer, offset, 16);
    headbuffer[offset+16] = util.byteOfInt(packetlength, 1);
    headbuffer[offset+17] = util.byteOfInt(packetlength, 0);
    headbuffer[offset+18] = type;
  }
  
  public void fromBytes(byte b[], int offset) throws IOException {
    System.arraycopy(b, offset, marker, 0, 16);
    packetlength = util.intFromBytes((byte) 0, (byte) 0, b[offset+16], b[offset+17]);
    type = b[offset+18];
  }	

} /* end class BGPHeader */




