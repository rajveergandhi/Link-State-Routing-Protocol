package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;

public class ServerThreadBranch implements Runnable{
    private Thread t;
	public Socket socket;
	public Router router;
	
	public ServerThreadBranch(Socket sock, Router rout){
        socket = sock;
        router = rout;
	}

	public void run() {
		try {
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			SOSPFPacket packet = (SOSPFPacket) inStream.readObject();
			if (packet.sospfType == 0) {

                System.out.println("received HELLO from " + packet.neighborID + ";");

                // create a new RouterDescription instance for the router being synced, set status to INIT
                RouterDescription r2 = new RouterDescription();
                r2.processIPAddress = packet.srcProcessIP;
                r2.processPortNumber = packet.srcProcessPort;
                r2.simulatedIPAddress = packet.neighborID;
                r2.status = RouterStatus.INIT;

                System.out.println("set " + r2.simulatedIPAddress + " state to INIT;");

                // create a new packet
                SOSPFPacket sPacket = new SOSPFPacket();
                sPacket.srcProcessIP = router.rd.processIPAddress;
                sPacket.srcProcessPort = router.rd.processPortNumber;
                sPacket.srcIP = router.rd.simulatedIPAddress;
                sPacket.dstIP = packet.neighborID;
                sPacket.sospfType = 0; // HELLO
                sPacket.routerID = router.rd.simulatedIPAddress;
                sPacket.neighborID = router.rd.simulatedIPAddress;

                // reply with HELLO for the sync
                ObjectOutputStream outToServer = new ObjectOutputStream(socket.getOutputStream());
                outToServer.writeObject(sPacket);
                
                // receive the second HELLO
                packet = (SOSPFPacket) inStream.readObject();
                if(packet.sospfType == 0) {
                    System.out.println("received HELLO from " + packet.neighborID + ";");

                    // set status to TWO_WAY
                    r2.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + r2.simulatedIPAddress + " state to TWO_WAY;");

                    // finally, add the synced router to the ports[] array for this router
                    for (int i = 0; i < router.ports.length; ++i) {
                        if (router.ports[i] == null) {
                            router.ports[i] = new Link(router.rd, r2);
                            break;
                        }
                    }
                }
            }
            socket.close();
        }
		catch (IOException e) {
            e.printStackTrace();
		}
		catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
	}
	public void start() {
	    if(t==null) {
	        t = new Thread(this);
	        t.start();
        }
    }
}
