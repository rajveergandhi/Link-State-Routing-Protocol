package socs.network.node;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;

import socs.network.message.*;

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
                if (packet.sospfType == 0) {
                    System.out.println("received HELLO from " + packet.neighborID + ";");

                    // set status to TWO_WAY
                    r2.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + r2.simulatedIPAddress + " state to TWO_WAY;");

                    // finally, add the synced router to the ports[] array for this router
                    for (int i = 0; i < router.ports.length; ++i) {
                        if (router.ports[i] == null) {
                            router.ports[i] = new Link(router.rd, r2, packet.weight);
                            break;
                        }
                    }
                }
                socket.close();
            }
            else if (packet.sospfType == 1) {
                boolean sendPacket = false;
                boolean sendToNeighbor = false;
                boolean addLinkToLSA = false;
                int neighbor = 0;
                LSA neighbor_lsa = null;
                LinkDescription ld = null;
                LinkDescription neighbor_ld = null;
                int weight = 0;

                Iterator lsa_iterator = packet.lsaArray.iterator();
                while (lsa_iterator.hasNext()) {
                    LSA next_lsa = (LSA) lsa_iterator.next();

                    if (router.lsd._store.get(next_lsa.linkStateID) != null) {
                        LSA prev_lsa = router.lsd._store.get(next_lsa.linkStateID);
                        if (prev_lsa.lsaSeqNumber < next_lsa.lsaSeqNumber) {
                            router.lsd._store.remove(prev_lsa.linkStateID);
                            addLinkToLSA = true;
                        }
                    }
                    if ((router.lsd._store.get(next_lsa.linkStateID) == null) || addLinkToLSA ) {
                        sendPacket = true;
                        router.lsd._store.put(next_lsa.linkStateID, next_lsa);
                        for (int i = 0; i < router.ports.length; i++) {
                                // add new link to the lsa, which orginated at the server end
                            if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(next_lsa.linkStateID) && router.ports[i].router2.status == RouterStatus.TWO_WAY) {
                                for (LinkDescription l: next_lsa.links)
                                    if (l.linkID.equals(router.rd.simulatedIPAddress))
                                        ld = l;
                                if (ld != null) {
                                    sendToNeighbor = true;
                                    neighbor_lsa = next_lsa;
                                    neighbor = i;
                                }
                                break;
                            }
                        }
                    }
                }

                if (sendPacket) {
                    for (int i = 0; i < router.ports.length; i++) {
                        if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(packet.routerID) == false && router.ports[i].router2.status == RouterStatus.TWO_WAY) {
                            Socket client = new Socket(router.ports[i].router2.processIPAddress, router.ports[i].router2.processPortNumber);
                            SOSPFPacket sPacket = new SOSPFPacket();
                            sPacket.sospfType = 1;
                            sPacket.srcProcessIP = router.rd.processIPAddress;
                            sPacket.srcProcessPort = router.rd.processPortNumber;
                            sPacket.srcIP = router.rd.simulatedIPAddress;
                            sPacket.dstIP = router.ports[i].router2.simulatedIPAddress;
                            sPacket.routerID = router.rd.simulatedIPAddress;
                            sPacket.neighborID = router.rd.simulatedIPAddress;
                            sPacket.lsaArray = new Vector<LSA>();
                            for (LSA update_lsa: router.lsd._store.values()) {
                                sPacket.lsaArray.addElement(update_lsa);
                            }
                            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                            out.writeObject(sPacket);
                            client.close();
                        }
                    }
                }

                if (sendToNeighbor) {
                    LSA this_lsa = router.lsd._store.get(router.rd.simulatedIPAddress);
                    for (LinkDescription l: this_lsa.links) {
                        if (l.linkID.equals(router.rd.simulatedIPAddress)) {
                            ld = l;
                        }
                    }
                    if (ld == null) {
                        ld = new LinkDescription();

                        ld.linkID = neighbor_lsa.linkStateID;
                        ld.portNum = neighbor;
                        for (LinkDescription l: neighbor_lsa.links) {
                            if (l.linkID.equals(router.rd.simulatedIPAddress)) {
                                neighbor_ld = l;
                            }
                        }
                        weight = neighbor_ld.tosMetrics;
                        ld.tosMetrics = weight;
                        router.ports[neighbor].weight = weight;
                        LSA lsa = router.lsd._store.get(router.rd.simulatedIPAddress);
                        lsa.lsaSeqNumber++;
                        lsa.links.add(ld);
                    }
                    for (int i = 0; i < router.ports.length; i++) {
                        if (router.ports[i] != null && router.ports[i].router2.status == RouterStatus.TWO_WAY) {
                            Socket client = new Socket(router.ports[i].router2.processIPAddress, router.ports[i].router2.processPortNumber);
                            SOSPFPacket cPacket = new SOSPFPacket();
                            cPacket.sospfType = 1;
                            cPacket.srcProcessIP = router.rd.processIPAddress;
                            cPacket.srcProcessPort = router.rd.processPortNumber;
                            cPacket.srcIP = router.rd.simulatedIPAddress;
                            cPacket.dstIP = router.ports[i].router2.simulatedIPAddress;
                            cPacket.routerID = router.rd.simulatedIPAddress;
                            cPacket.neighborID = router.rd.simulatedIPAddress;
                            cPacket.lsaArray = new Vector<LSA>();
                            for (LSA update_lsa: router.lsd._store.values()) {
                                cPacket.lsaArray.addElement(update_lsa);
                            }
                            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                            out.writeObject(cPacket);
                            client.close();
                        }
                    }
                }
                socket.close();
            }
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
            System.out.print(">> ");
        }
    }
}
