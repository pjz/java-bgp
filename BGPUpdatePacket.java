

import java.util.*;
import java.io.*;

class BGPUpdatePacket {

  public BGPHeader header;

  public Vector withdrawls;
  public Vector pathattrs;
  public Vector nlrinfo;

  public BGPUpdatePacket(BGPHeader h, DataInputStream in) throws IOException {
    header = h;
    int bodylength = header.packetlength - header.length;
    byte packetbuffer[] = new byte[bodylength];
    in.readFully(packetbuffer, 0, bodylength);
    fromBytes(packetbuffer, 0);
  }
  
  public void fromBytes(byte b[], int offset) {
    withdrawls = new Vector();
    nlrinfo = new Vector();
    pathattrs = new Vector();

    /* withdrawls */
    int toffset = offset + 2;
    int withdrawllen = util.intFromBytes((byte) 0, (byte) 0, b[offset], b[offset+1]);
    while (toffset < offset + withdrawllen) {
      int prefixlen = (b[toffset] + 7) / 8;
      byte prefix[] = new byte[4];
      System.arraycopy(b, toffset+1, prefix, 0, prefixlen);
      withdrawls.addElement(new InetNetwork(prefix[0], prefix[1], prefix[2], prefix[3], b[toffset]));
      toffset = toffset + prefixlen + 1;
    }             

    /* path attributes */
    int pathattrlen = util.intFromBytes((byte) 0, (byte) 0, b[toffset], b[toffset+1]);
    toffset = toffset + 2; /* past the pathattrlen */
    int nlroffset = toffset + pathattrlen; /* where the NRL info is */
    BGPPathAttribute pathattr = null;
    while (toffset < nlroffset) {
      pathattr = new BGPPathAttribute();
      pathattr.fromBytes(b, toffset);
      pathattrs.addElement(pathattr);
      toffset = toffset + pathattr.length;
    }

    if (toffset != nlroffset) {
      System.err.println("WARNING: INCONSISTENCY IN PACKET DATA");
      toffset = nlroffset;
    }

    /* Network Layer Reachability (NLR) info */

    while( toffset < offset + header.packetlength - header.length) {        
      int prefixlen = (b[toffset] + 7) / 8;
      if (prefixlen > 4) {
        System.err.println("ERROR: INVALID PREFIX LENGTH ("+Byte.toString(b[toffset])+")");
        util.dumpArray(b,0,b.length-1);
        System.exit(1);
        b[toffset] = 32;
      }
      byte prefix[] = new byte[4];
      System.arraycopy(b, toffset+1, prefix, 0, prefixlen);
      nlrinfo.addElement(new InetNetwork(prefix[0], prefix[1], prefix[2], prefix[3], b[toffset]));
      toffset = toffset + prefixlen + 1;
    }
  }

  public void toBytes(byte b[], int offset) {

    /* write out withdrawls */
    int toffset = offset+2;
    for(Enumeration w = withdrawls.elements(); w.hasMoreElements();) {
      InetNetwork net = (InetNetwork) w.nextElement();
      b[toffset] = (byte) net.prefixlength;
      for (int i = 0; i < (b[toffset] / 8); i++) {
        b[toffset+1+i] = util.byteOfLong(net.prefix, 3-i);
      }
      toffset = toffset + 1 + (b[toffset] / 8);
    }
    b[offset] = util.byteOfInt(toffset - offset - 2, 1);
    b[offset+1] = util.byteOfInt(toffset - offset - 2, 0);

    /* write out the path attributes */
    int pathattroff = toffset;
    for(Enumeration p = pathattrs.elements(); p.hasMoreElements();) {
      BGPPathAttribute pathattr = (BGPPathAttribute) p.nextElement();
      pathattr.toBytes(b, toffset);
      toffset = toffset + pathattr.length;
    }
    b[pathattroff] = util.byteOfInt(toffset - pathattroff - 2, 1);
    b[pathattroff+1] = util.byteOfInt(toffset - pathattroff - 2, 0);

    /* write out the NLR info */
    for(Enumeration n = nlrinfo.elements(); n.hasMoreElements();) {
      InetNetwork net = (InetNetwork) n.nextElement();  
      b[toffset] = (byte) net.prefixlength;
      for (int i = 0; i < (b[toffset] / 8); i++) {
        b[toffset+1+i] = util.byteOfLong(net.prefix, 3-i);
      }
      toffset = toffset + 1 + (b[toffset] / 8);
    } 
  }

}


