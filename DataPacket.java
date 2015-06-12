
abstract class DataPacket {

  public DataPacketHeader(DataInputStream in) throws IOException {
    byte packetbuffer[] = new byte[length()];
    in.readFully(packetbuffer, 0, packetbuffer.length);
    fromBytes(packetbuffer);
  }
  
  /* convert the packet into bytes, in the array b, starting at position <offset> */
  abstract public void toBytes(byte b[], int offset) ; 

  public void toBytes(byte b[]) {
    toBytes(b, 0);
  }

  /* set the packet's value based on the bytes in b starting at position <offset> */
  abstract public void fromBytes(byte b[], int offset) ;
  
  public void fromBytes(byte b[]) {
    fromBytes(b, 0);
  }

  /* get the length of the packet, not including the header */
  abstract public int length();

}

abstract class DataPacket extends DataPacketHeader {

  abstract public void setPacketLength(int l); /* set the overall packet length */

  abstract public int packetLength() ; /* returns the overall packet length */

}


abstract class HeadedDataPacket extends DataPacket {

  HeadedDataPacketHeader header;

  public HeadedDataPacket(DataInputStream in) throws IOException {
    header = new HeadedDataPacketHeader(in);
    byte packetbuffer[header.packetLength() - header.length()];
    in.readFully(packetbuffer, 0, packetbuffer.length);
    fromBytes(packetbuffer);
  }

  public HeadedDataPacket(HeadedDataPacketHeader h, DataInputStream in) throws IOException {
    header = h;
    byte packetbuffer[header.packetLength() - header.length()];
    in.readFully(packetbuffer, 0, packetbuffer.length);
    fromBytes(packetbuffer);
  }

  public void writeTo(OutputStream out) {
    byte outbuffer[] = new byte[length()];
    header.setPacketLength(header.length() + outbuffer.length);
    header.writeTo(out);
    toBytes(outbuffer);
    out.write(outbuffer);
  }
}


