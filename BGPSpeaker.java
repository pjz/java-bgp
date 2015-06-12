import java.util.*;
import java.net.*;
import java.io.*;


class BGPSpeaker {

  String bind_to = "192.207.126.10";  /* What IP address to bind to (locally) */
  int DEFAULT_PORT = 179; /* Standard port */
  public int ASNumber = 65534;   /* our AS Number */
  public int BGP_Session_Timeout = 30; /* 0 or >=3 ; 4 mins (240s) per RFC 1771 */

  public static boolean debug = true;

  Hashtable connections = new Hashtable();
  boolean should_run = true;
  long local_BGPID; /* our BGPID */

  Vector sessions = new Vector();

  public BGPSpeaker() {
    constructor(DEFAULT_PORT);
  }

  public BGPSpeaker(int port) {
    constructor(port);
  }

  void constructor(int port) {
    byte localaddr[];
    InetAddress us;
    try {
      us = InetAddress.getByName(bind_to);
      System.out.println("Binding to port "+Integer.toString(port)+"...");
      localaddr = us.getAddress();	
      local_BGPID = util.longFromBytes((byte)0, (byte)0, (byte)0, (byte)0, localaddr[0], localaddr[1], localaddr[2], localaddr[3]);
//    } catch (IOException e) {
//      System.err.println("Trouble binding to port "+Integer.toString(port)+": "+e.toString());
    } catch (UnknownHostException e) {
      System.err.println("Unknown host "+bind_to+"...");
    }
  }


  /* This will keep the other side from thinking we're dead.
     Also, since we only listen, this is the only thread that 
     writes anything to the socket */
  class KeepAliveThread extends Thread {
    OutputStream out;
    long holdtime;
    
    KeepAliveThread(OutputStream who, long delay) {
      out = who;
      holdtime = delay;
	}
    
    public void run() {
      BGPHeader keepalive = new BGPHeader();
      keepalive.type = BGPHeader.TYPE_KEEPALIVE;
      while (true) {
	try {
	  this.sleep(holdtime * 1000);	
	  keepalive.writeTo(out);
	} catch (InterruptedException e) { 
	} catch (IOException e) {
	  System.out.println("keepalive thread error: "+e.toString());
	  System.exit(1);
	}
	if (debug) System.out.println("keepalive sent.");
      }
    }
  }

  public void connectTo(String remotehost, int port) throws UnknownHostException, IOException {
    Socket s = new Socket(remotehost, port);
    //BGPSession session = new BGPSession(s, this);
    //sessions.addElement(session);
    //session.start();
    handleConnection(s);
  }

  public void connectTo(String remotehost) throws UnknownHostException, IOException {
    connectTo(remotehost, DEFAULT_PORT);
  }

  public void handleConnection(Socket s) {
    OutputStream out = null;
    DataInputStream in = null;
    long holdtime = BGP_Session_Timeout;

    try {
      /* set up the socket i/o */
      out = s.getOutputStream();
      in = new DataInputStream(s.getInputStream());

      /* STATE: ACTIVE (per BGP draft) */

      /* make an OPEN packet */
      BGPOpenPacket open = new BGPOpenPacket();
      open.AS = ASNumber; 
      open.version = 4; /* BGP version */
      open.BGPID = local_BGPID;
      open.holdtime = BGP_Session_Timeout; 

      /* send the OPEN packet */
      open.writeTo(out);
      out.flush();

      /* STATE: OPENSENT */

      /* wait for an OPEN packet */
      open = new BGPOpenPacket(in);
      if (debug) System.out.println("received OPEN");

      /* got it, check the holdtime */
      if (open.holdtime < holdtime) {
	holdtime = open.holdtime;
      }

      /* keep track of this connection (index by BGPID) */
      if (connections.get(new Long(open.BGPID)) != null) {
	/* duplicate connection; no need for it, so close it */
	if (debug) System.out.println("Duplicate connection for BGPID "+Long.toString(open.BGPID)+", dropping.");
	BGPNotificationPacket cease = new BGPNotificationPacket();
        cease.error = BGPNotificationPacket.CEASE;
	cease.writeTo(out);
	s.close();
	return;
      } else {
	if (debug) System.out.println("Connection not a dup; proceeding.");
      }

      BGPStatus status = new BGPStatus(open.BGPID);

      connections.put(InetNetwork.toString(open.BGPID), status);

      /* ack with a KEEPALIVE */
      BGPHeader keepalive = new BGPHeader();
      keepalive.type = BGPHeader.TYPE_KEEPALIVE;
      keepalive.writeTo(out);
      if (debug) System.out.println("Keepalive sent");

      /* STATE: OPENCONFIRM */

      keepalive = new BGPHeader(in);
      if (keepalive.type == BGPHeader.TYPE_KEEPALIVE) { 
	/* rock on */
	if (debug) System.out.println("received OPENCONFIRM (keepalive)");
      } else if (keepalive.type == BGPHeader.TYPE_NOTIFICATION) { 
	/* error probably */
	System.out.println("received NOTIFICATION instead of OPEN confirm");
	BGPNotificationPacket error = new BGPNotificationPacket(keepalive, in);
	System.out.println(error.toString());
      } else {
	System.err.println("Error in FSM.  Unexpected response to OPEN.");
      }

      /* STATE: ESTABLISHED */
	  
      if (holdtime > 0) {
	/* 0 holdtime means don't send keepalives, 
	   else space them < holdtime apart */
	if (holdtime < 3) { /* never have a keepalive of < 3s */
	  holdtime = 3;
	}
	new KeepAliveThread(out, holdtime*9/10).start();
      }

      boolean should_run = true;
      BGPHeader header;
      while (should_run) {
	if (debug) System.out.println("Waiting for header...");
	header = new BGPHeader(in);
	if (debug) System.out.print("...Got header of type: ");
	if (header.type == BGPHeader.TYPE_KEEPALIVE) {
	  if (debug) System.out.println("Keepalive");
	  /* reset the hold timer. well, but nevermind */
	} else if (header.type == BGPHeader.TYPE_UPDATE) {
	  // System.out.print("Update " + Long.toString(status.updates)+" : ");
	  status.updates++;
	  
	  /* handle updates */
	  BGPUpdatePacket update = new BGPUpdatePacket(header, in);
	  // System.out.print(" w: "+Integer.toString(update.withdrawls.size()));
	  for (Enumeration w = update.withdrawls.elements(); w.hasMoreElements();) {
	    InetNetwork net = (InetNetwork) w.nextElement();
	    // System.out.println("Withdraw: " +net.toString());
	    status.addWithdrawl(net);
	  }
	  // System.out.println(" a: "+Integer.toString(update.nlrinfo.size()));
	  for (Enumeration a = update.nlrinfo.elements(); a.hasMoreElements();) {
	    InetNetwork net = (InetNetwork) a.nextElement();
	    // System.out.println("Announce: "+net.toString());
	    status.addAnnounce(net);
	  }
	} else if (header.type == BGPHeader.TYPE_NOTIFICATION) {
	  System.out.println("Notification");
	  should_run = false;
	} 
      }
    } catch (IOException e) {
      System.err.println("Error: "+e.toString());
    }
  }

  
  ///////////////////////////////////////////////////  Main

  public static void main(String args[]) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    BGPSpeaker b = new BGPSpeaker();

    try {
      b.connectTo("192.168.1.254"); /* connect the speaker to someplace */
    } catch (UnknownHostException e) {
      System.out.println("dunno who stout is.");
    } catch (IOException e) {
      System.out.println("IOError: "+e.toString());
    }
    

  }


}
