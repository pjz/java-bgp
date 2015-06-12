
00-README for java-BGP
----------------------

Who?   Paul ('PJ') Jimenez by the graces of his 
         then-current employer, Smallworks, Inc.

What?  A cleanroom RFC-xxxx based BGP implementation in Java.  Open source.
       Artistic License.
 
When?  Now.  Well, okay, awhile ago, but being released now. (October 1, 1999)

Where? http://github.com/pjz/java-bgp

Why?   Originally to just be a listener to allow userspace to get the advantages
         of listening to the RBL.  Probably useful for those folk interested in
         experimenting with routing protocols as well.

How?   Blood, sweat and tears.  And Dr. Pepper.

Included in this archive:
------------------------
BGPHeader.java             BGP packet header decoding/encoding/access
BGPListener.java           The main program that actually uses the 
                             rest to listen to a BGP feed
BGPNotificationPacket.java BGP notify packet decoding/encoding/access
BGPOpenPacket.java         BGP open packet decoding/encoding/access
BGPOpenParameter.java      BGP open packet parameter decoding/encoding/access
BGPPathAttribute.java      BGP path attr decoding/encoding/access
BGPStatus.java             BGP session status keeping structure
BGPUpdatePacket.java       BGP update packet decoding/encoding/access
DataPacket.java            BGP update packet decoding/encoding/access
InetNetwork.java           Class useful for encoding/decoding/viewing CIDR nets
util.java                  Useful static method holder.  Mostly binary<->int
                             encoding/decoding routines

Known bugs:
-----------

None.   Successfully kept a single session up for multiple months.

Etc.:
-----

Questions, comments, rants, raves, reviews and bags of small unmarked
bills to pj@place.org.  No warranty expresses or implied.  Use at your
own risk.  Try not to flap too much.
  

