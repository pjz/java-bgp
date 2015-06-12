
import java.util.*;

class BGPStatus extends Object {

  long BGPID; /* init'd by the constructor */

  Date enteredcurrentstate;
  public long updates = 0;
  public long withdrawls = 0;
  public long announces = 0;
  public Hashtable routes = new Hashtable();

  public BGPStatus(long newBGPID) {
    BGPID = newBGPID;
    resetUptime();
  }

  public long BGPID() {
    return BGPID;
  }

  public long uptime() {
    return ((new Date()).getTime() - enteredcurrentstate.getTime());
  }

  public void resetUptime() {
    enteredcurrentstate = new Date();
  }

  public void addWithdrawl(InetNetwork withdrawl) {
    withdrawls++;
    if (routes.get(withdrawl.toString()) != null) {
      routes.remove(withdrawl.toString());
    }
  }

  public void addAnnounce(InetNetwork announce) {
    announces++;
    if (routes.get(announce.toString()) == null) {
      routes.put(announce.toString(), announce);
    } 
      
  }
}






