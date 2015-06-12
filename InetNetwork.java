
class InetNetwork {

  public int prefixlength;
  public long prefix;

  public InetNetwork(long network, int masklen) {
    prefix = network;
    prefixlength = masklen;
    /* clear the low bits - probably not necessary */
    //    prefix = (prefix & 0x0FFFFFFFFFFFFFFFL) << (32-masklen);
  }

  public InetNetwork(byte o1, byte o2, byte o3, byte o4, int masklen) {
    prefix = util.longFromBytes((byte)0, (byte)0, (byte)0, (byte)0, o1, o2, o3, o4);
    prefixlength = masklen;
  }

  public static String toString(long network, int masklen) {
    InetNetwork n = new InetNetwork(network, masklen);
    return n.toString();
  }

  public static String toString(long network) {
    return toString(network, 32);
  }

  public String toString() {
    return 
      Long.toString((prefix & 0x00000000FF000000L) >> (long) 24)+"."+
      Long.toString((prefix & 0x0000000000FF0000L) >> (long) 16)+"."+
      Long.toString((prefix & 0x000000000000FF00L) >> (long) 8) +"."+
      Long.toString( prefix & 0x00000000000000FFL)+"/"+
      Integer.toString(prefixlength);
  }
  
}



