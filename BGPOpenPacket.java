/* BGP OPEN packet */


import java.net.*;
import java.io.*;
import java.util.*;

class BGPOpenPacket {

  public BGPHeader header;
  public int version;
  public int AS;
  public int holdtime;
  public long BGPID;
  public Vector params;

  public static boolean debug = false;  

  public BGPOpenPacket() {
    params = new Vector(0);
    header = new BGPHeader();
    header.type = BGPHeader.TYPE_OPEN;
    header.packetlength = BGPHeader.length + length();
  }

  BGPOpenPacket(DataInputStream in) throws IOException {
    if (debug) System.out.println("OPEN: Waiting for header");
    header = new BGPHeader(in);
    if (header.type != BGPHeader.TYPE_OPEN) {
      throw new IOException("Expected packet type OPEN, received type "+header.type);
    }
    int bodylength = header.packetlength - header.length;
    byte packetbuffer[] = new byte[bodylength];
    if (debug) System.out.println("OPEN: Waiting for body");
    in.readFully(packetbuffer, 0, bodylength);
    if (debug) System.out.println("OPEN: received body");
    fromBytes(packetbuffer, 0);
  }
  
  
  BGPOpenPacket(BGPHeader h, DataInputStream in) throws IOException {
    header = h;
    int bodylength = header.packetlength - header.length;
    byte packetbuffer[] = new byte[bodylength];
    in.readFully(packetbuffer, 0, bodylength);
    fromBytes(packetbuffer, 0);
  }
  
  public void toBytes(byte b[], int offset) {
    b[offset] = (byte)version;
    b[offset+1] = util.byteOfInt(AS, 1);
    b[offset+2] = util.byteOfInt(AS, 0);
    b[offset+3] = util.byteOfInt(holdtime, 1);
    b[offset+4] = util.byteOfInt(holdtime, 0);
    b[offset+5] = util.byteOfLong(BGPID, 3);
    b[offset+6] = util.byteOfLong(BGPID, 2);
    b[offset+7] = util.byteOfLong(BGPID, 1);
    b[offset+8] = util.byteOfLong(BGPID, 0);
    int pcount = 0;
    int poffset = offset+10;
    for (Enumeration p = params.elements(); p.hasMoreElements(); ) {
      BGPOpenParameter param = (BGPOpenParameter)p.nextElement();
      param.toBytes(b, poffset);
      poffset = poffset+param.length();
      pcount++;
    }
    b[offset+9] = (byte) pcount;
  }    
  
  public void writeTo(OutputStream out) throws IOException {

    if (false) {
      /* write out the header */
      int l = length();
      header.packetlength = header.length + l;
      header.writeTo(out);

      /* write out this packet */
      byte outbuffer[] = new byte[l];
      toBytes(outbuffer, 0);
      if (debug) System.out.println("Sending "+toString());
      out.write(outbuffer);
      out.flush();
      if (debug) System.out.println("Sent "+toString());
    } else {
      int l = length();
      header.packetlength = header.length + l;
      byte outbuffer[] = new byte[header.packetlength];
      /* write out the header */
      header.toBytes(outbuffer,0);

      /* write out this packet */
      toBytes(outbuffer, header.length);
      if (debug) System.out.println("Sending "+toString());
      out.write(outbuffer);
      out.flush();
      if (debug) System.out.println("Sent "+toString());
    }
  }

  public String toString() {
    String toreturn = "OPEN: ";
    toreturn = toreturn + "version: "+Integer.toString(version);
    toreturn = toreturn + " AS: "+Integer.toString(AS);	
    toreturn = toreturn + " holdtime: "+Integer.toString(holdtime);
    toreturn = toreturn + " BGPID: "+Long.toString(BGPID);
    toreturn = toreturn + "(" + InetNetwork.toString(BGPID) + ")";
    return toreturn;
  }


  int length() {
    int l = 10 ;   /* OPEN static data */
    /* length of all the params */
    for(Enumeration p = params.elements(); p.hasMoreElements(); ) {
      l = l + ((BGPOpenParameter)(p.nextElement())).length();
    }
    return l;
  }
  
  public void fromBytes(byte b[], int offset ) throws IOException {
    /* get the packet */
    version = b[offset];
    AS = util.intFromBytes((byte) 0,(byte) 0, b[offset+1], b[offset+2]);
    holdtime = util.intFromBytes((byte) 0, (byte) 0, b[offset+3], b[offset+4]);
    BGPID = util.intFromBytes(b[offset+5], b[offset+6], b[offset+7], b[offset+8]);
    int paramcount = b[offset+9];
    params = new Vector(paramcount);
    int poffset = offset + 10;
    for (int i = 1; i < paramcount; i++) {
      BGPOpenParameter param = new BGPOpenParameter();
      param.fromBytes(b, poffset);
      params.addElement(param);
      poffset = poffset + param.value.length + 2;
    }
  }
}


