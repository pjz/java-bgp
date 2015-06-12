

import java.util.*;
import java.net.*;
import java.io.*;


class BGPListener {

  String bind_to = "192.168.1.2"; /* what IP to bind to locally */
  int DEFAULT_PORT = 179; /* Standard port */
  public int ASNumber = 65534;   /* our AS Number */
  public int BGP_Session_Timeout = 240; /* 0 or >=3 ; 4 mins (240s) per RFC 1771 */

  Hashtable connections = new Hashtable();

  boolean debug = false;  
  boolean should_run = true;
  long local_BGPID; /* our BGPID */
  ServerSocket serverport;
  Listener listener;

  Vector sessions = new Vector();

  public BGPListener() {
    constructor(DEFAULT_PORT);
  }

  public BGPListener(int port) {
    constructor(port);
  }

  void constructor(int port) {
    byte localaddr[];
    InetAddress us;
    try {
      us = InetAddress.getByName(bind_to);
      System.out.println("Now listening on port "+Integer.toString(port)+"...");
      serverport = new ServerSocket(port);
      localaddr = us.getAddress();	
      
      local_BGPID = util.longFromBytes((byte)0, (byte)0, (byte)0, (byte)0, 
                                       localaddr[0], localaddr[1], localaddr[2], localaddr[3]);
    } catch (IOException e) {
      System.err.println("Trouble binding to port "+Integer.toString(port)+": "+e.toString());
      System.exit(1);
    }
  }


  /* internal listening threadclass */
  class Listener extends Thread {
    BGPListener bgpl;
    ServerSocket server;

    public Listener(ServerSocket s, BGPListener b) {
      bgpl = b;
      server = s;
    }

    public void run() {
      Socket socket;
      BGPSession s;
      try {
	while (true) {
	  socket = server.accept();
	  s = new BGPSession(socket, bgpl);
	  s.start();
	  bgpl.sessions.addElement(s);
	}
      } catch (IOException e) {
	System.err.println("IO Error: "+e.toString());
      }
    }
  }

  /* internal threadclass to handle connections */ 
  class BGPSession extends Thread {
    Socket sock;
    BGPListener bgpl;
    
    public BGPSession(Socket s, BGPListener b) {
      sock = s;
      bgpl = b;
    }
    
    public void run() {
      bgpl.handleConnection(sock);
    }
  }

  public void startListening() {
    /* spawn a new listening thread so we can get on with our life */
    listener = new Listener(serverport, this);
    listener.start();
  }

  public void stopListening() {
    /* kill all child threads */
    for(Enumeration sessionlist = sessions.elements(); sessionlist.hasMoreElements();) {
      BGPSession session = (BGPSession)sessionlist.nextElement();
      session.stop();
    }
    listener.stop();
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
      }
    }
  }

  public void connectTo(String remotehost, int port) throws UnknownHostException, IOException {
    Socket s = new Socket(remotehost, port);
    BGPSession session = new BGPSession(s, this);
    sessions.addElement(session);
    session.start();
  }

  public void connectTo(String remotehost) throws UnknownHostException, IOException {
    connectTo(remotehost, DEFAULT_PORT);
  }

  public void handleConnection(Socket s) {
    OutputStream out = null;
    DataInputStream in = null;
    long holdtime = BGP_Session_Timeout;
    KeepAliveThread kt = null;

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
      System.out.println("received OPEN");

      /* got it, check the holdtime */
      if (open.holdtime < holdtime) {
	holdtime = open.holdtime;
      }

      /* keep track of this connection (index by BGPID) */
      if (connections.get(new Long(open.BGPID)) != null) {
	/* duplicate connection; no need for it, so close it */
	System.out.println("Duplicate connection for BGPID "+Long.toString(open.BGPID)+", dropping.");
	BGPNotificationPacket cease = new BGPNotificationPacket();
        cease.error = BGPNotificationPacket.CEASE;
	cease.writeTo(out);
	s.close();
	return;
      } else {
	System.out.println("Connection not a dup; proceeding.");
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
	// System.out.println("received OPENCONFIRM (keepalive)");
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
	kt = new KeepAliveThread(out, holdtime*9/10);
	kt.start();
      }

      boolean should_run = true;
      BGPHeader header;
      while (should_run) {
	// System.out.println("Waiting for header...");
	header = new BGPHeader(in);
	// System.out.print("...Got header of type: ");
	if (header.type == BGPHeader.TYPE_KEEPALIVE) {
	  // System.out.println("Keepalive");
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
    if (kt != null) kt.stop();
  }

  public static void main(String args[]) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    BGPListener b = new BGPListener();

    b.startListening();

    String command = "";
    while (!command.equalsIgnoreCase("quit")) {
      System.out.print(">");
      try {
	command = in.readLine();
      } catch (IOException e) {
	System.out.println("Invalid input");
      }
      if (command.startsWith("connect")) {
	int i, j;
	String arg;
	BGPStatus status;
	for(i = 7; i < command.length() && Character.isWhitespace(command.charAt(i)) ; i++);
	if (i < command.length()) { /* there's another word, it's probably a connection */
	  for(j = i; j < command.length() && !Character.isWhitespace(command.charAt(j)); j++);
	  arg = command.substring(i, j);
	  try {
	    b.connectTo(arg);
	  } catch (UnknownHostException e) {
	    System.out.println("Unknown host: "+arg);
	  } catch (IOException e) {
	    System.out.println("Problem connecting to "+arg+": "+e.toString());
	  }
	}
      } else if (command.startsWith("status")) {
	int i, j;
	String arg;
	BGPStatus status;
        // see if there's another word
	for(i = 6; i < command.length() && Character.isWhitespace(command.charAt(i)) ; i++);
	if (i < command.length()) { /* there's another word, it's probably a connection */
          // get the 2nd word
	  for(j = i; j < command.length() && !Character.isWhitespace(command.charAt(j)); j++);
	  arg = command.substring(i, j);
          // the 2nd word is a connection, display its routes
	  if ((status = (BGPStatus)b.connections.get(arg)) != null) {
	    for(Enumeration e = status.routes.keys(); e.hasMoreElements();) {	
	      String key = (String)e.nextElement();
	      System.out.println(" "+key);
	    }
	  } else {
	    System.out.println("Invalid argument: "+arg);
	  }
	} else { // no 2nd word, display list of connections
          if (b.connections.size() < 1) {
            System.out.println("No connections to list.");
	  } else {
	    for(Enumeration e = b.connections.elements(); e.hasMoreElements();) {	
	      status = (BGPStatus) e.nextElement();
	      System.out.print(InetNetwork.toString(status.BGPID));
	      System.out.print(" up for "+Long.toString(status.uptime() / 1000L)+"s");
	      System.out.print(" U: "+Long.toString(status.updates));
	      System.out.print(" A: "+Long.toString(status.announces));
	      System.out.print(" W: "+Long.toString(status.withdrawls));
	      System.out.print(" R: "+Integer.toString(status.routes.size()));
	      System.out.println("");
	    }
	  }
	}
      } else if (command.equals("?") || command.equalsIgnoreCase("help")) {
	System.out.println("connect <host> - initiate a BGP connection on the default BGP port");
	System.out.println("status - show status");
	System.out.println("status <connection> - show routes for that connection");
	System.out.println("quit - exit, closing everything down.");
	System.out.println("help or ? - this message");
      }
    }
      
    b.stopListening();
  }


}
