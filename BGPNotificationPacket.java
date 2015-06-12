
import java.io.*;

class BGPNotificationPacket {

  public BGPHeader header;

  public int error;
  public static int MESSAGE_HEADER_ERROR = 1;
  public static int OPEN_MESSAGE_ERROR = 2;
  public static int UPDATE_MESSAGE_ERROR = 3;
  public static int HOLD_TIMER_EXPIRED = 4;
  public static int FINITE_STATE_MACHINE_ERROR = 5;
  public static int CEASE = 6;

  public int subcode;
  public byte[] data;

  public BGPNotificationPacket() {
    header = new BGPHeader();
    header.length = 21;
  }

  public BGPNotificationPacket(DataInputStream in) throws IOException {
    header = new BGPHeader(in);
    if (header.type != BGPHeader.TYPE_NOTIFICATION) {
      throw new IOException("Expected packet type NOTIFICATION, received type "+header.type);
    }
    int bodylength = header.packetlength - header.length;
    byte packetbuffer[] = new byte[bodylength];
    in.readFully(packetbuffer, 0, bodylength);
    fromBytes(packetbuffer, 0);
  }

  public BGPNotificationPacket(BGPHeader h, DataInputStream in) throws IOException {
    header = h;
    int bodylength = header.packetlength - header.length;
    byte packetbuffer[] = new byte[bodylength];
    in.readFully(packetbuffer, 0, bodylength);
    fromBytes(packetbuffer, 0);
  }

  public String toString() {
    String Errors[] = { "", 
			"Message Header Error",
			"OPEN Message Error",
			"UPDATE Message Error",
			"Hold Timer Expired",
			"Finite State Machine Error",
			"Cease"
    };
    String MessageHeaderErrors[] = {"", 
				"Connection Not Synchronized.",
				"Bad Message Length.",
				"Bad Message Type."
    };
    String OPENMessageErrors[] = {"",
				  "Unsupported Version Number.",
				  "Bad Peer AS.",
				  "Bad BGP Identifier.",
				  "Unsupported Optional Parameter",
				  "Authentication Failure.",
				  "Unacceptable Hold Time."
    };
    String UPDATEMessageErrors[] = { "",
				     "Malformed Attribute List",
				     "Unrecognized Well-known Attribute",
				     "Missing Well-known Attribute",
				     "Attribute Flags Error",
				     "Attribute Length Error",
				     "Invalid ORIGIN Attribute",
				     "AS Routing Loop",
				     "Invalid NEXT_HOP Attribute",
				     "Optional Attribute Error",
				     "Invalid Network Field",
				     "Malformed AS_PATH"
    };

    String toreturn = "NOTIFICATION: "+Integer.toString(error)+"."+
      Integer.toString(subcode)+": ";

    if (error == MESSAGE_HEADER_ERROR) {
      toreturn = toreturn + Errors[error];
      if (subcode > 0 && subcode < MessageHeaderErrors.length) {
	toreturn = toreturn +": "+MessageHeaderErrors[subcode];
      }
    } else if (error == OPEN_MESSAGE_ERROR) {
      toreturn = toreturn + Errors[error];
      if (subcode > 0 && subcode < OPENMessageErrors.length) {
	toreturn = toreturn +": "+OPENMessageErrors[subcode];
      }
    } else if (error == UPDATE_MESSAGE_ERROR) {
      toreturn = toreturn + Errors[error];
      if (subcode > 0 && subcode < UPDATEMessageErrors.length) {
	toreturn = toreturn + ": " + UPDATEMessageErrors[subcode];
      }
    } else if (error == HOLD_TIMER_EXPIRED) {
      toreturn = toreturn + Errors[error];
    } else if (error == FINITE_STATE_MACHINE_ERROR) {
      toreturn = toreturn + Errors[error];
    } else if (error == CEASE) {
      toreturn = toreturn + Errors[error];
    } else {
      toreturn = toreturn + "Unknown";
    }
    /* need to parse the data field into the error message eventually */
    return toreturn;
  }

  public int length() {
    int toreturn = 2; /* static data length */
    if (data != null) {
      toreturn = toreturn + data.length;
    }
    return toreturn;
  }

  public void writeTo(OutputStream out) throws IOException {
   int l = length();
   header.packetlength = header.length + l;
   header.writeTo(out);
   byte outbuffer[] = new byte[l];
   toBytes(outbuffer, 0);
   System.out.println("Sending "+toString());
   out.write(outbuffer);
  }

  public void fromBytes(byte b[], int offset) {
    int datalen = header.packetlength - 21;
    data = new byte[datalen];
    error = b[offset];
    subcode= b[offset+1];
    System.arraycopy(b, offset+2, data, 0, datalen);
  }

  public void toBytes(byte b[], int offset) {
    b[offset] = (byte) error;
    b[offset+1] = (byte) subcode;
    if (data != null) {
      System.arraycopy(data, 0, b, offset+2, data.length);
    }
  }

}
    
