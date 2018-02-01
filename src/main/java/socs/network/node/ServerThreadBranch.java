package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.DataOutputStream;

import socs.network.message.SOSPFPacket;

public class ServerThreadBranch extends Thread{
	public Socket socket;
	public Router router;
	
	public ServerThreadBranch(Socket sock, Router rout){
        socket = sock;
        router = rout;
	}

	public void run() {
		try {
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			//SOSPFPacket packet = (SOSPFPacket) new ObjectInputStream(socket.getInputStream()).readObject();
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
                //SOSPFPacket packet2 = (SOSPFPacket) new ObjectInputStream(socket.getInputStream()).readObject();
                packet = (SOSPFPacket) inStream.readObject();
                //packet = socket.getInputStream().readObject();
                if(packet.sospfType == 0) {
                    System.out.println("received HELLO from " + packet.neighborID + ";");

                    // set status to TWO_WAY
                    r2.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + r2.simulatedIPAddress + " state to TWO_WAY;");

                    // finally, add the synced router to the ports[] array for this router
                    for (int i = 0; i < router.ports.length; ++i) {
                        if (router.ports[i] != null) {
                            router.ports[i] = new Link(router.rd, r2);
                            break;
                        }
                    }
                }
            }
            socket.close();
        }
/*
                    for (int i = 0; i<4; i++) {
                        if(router.ports[i] == null) {
                            router.ports[i] = new Link(router.rd, neighborRouter);
                            break;
                        }
                    }
                }


                //received message for a second time. set status of router2 as TWO_WAY
                if(secondMessage.sospfType == 0) {
                    System.out.println("received Hello from " + secondMessage.neighborID);
                    neighborRouter.status = RouterStatus.TWO_WAY;
                    System.out.println("set " + neighborRouter.simulatedIPAddress + " state to TWO_WAY.\n");

                    //add router to link array
                    for (int i = 0; i<4; i++) {
                        if(router.ports[i] == null) {
                            router.ports[i] = new Link(router.rd, neighborRouter);
                            break;
                        }
                    }
                }


                ports[i] = new Link(rd, r2);

                // check if this router is already in the ports[] array
				for (int i = 0; i < router.ports.length; ++i) {
					if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(message.neighborID)) {
						break;
					}
				}
                if 
				String neighborIP = listenMessage.neighborID;
				boolean initialHello = true;

				//to check if message received from router is the first time

				//if message received from router is the first time
				if (initialHello) {
					System.out.println("received Hello from " + listenMessage.neighborID);
					RouterDescription neighborRouter = new RouterDescription();
					neighborRouter.processIPAddress = listenMessage.srcProcessIP;
					neighborRouter.processPortNumber = listenMessage.srcProcessPort;
					neighborRouter.simulatedIPAddress = neighborIP;
					//set router description of router2 as INIT
					neighborRouter.status = RouterStatus.INIT;
					System.out.println("Set " + neighborIP + " state to INIT.\n");

					//send hello message back to router
					SOSPFPacket dispatchMessage = new SOSPFPacket();
					dispatchMessage.sospfType = 0;
					dispatchMessage.srcProcessIP = router.rd.processIPAddress;
					dispatchMessage.srcProcessPort = router.rd.processPortNumber;
					dispatchMessage.srcIP = router.rd.simulatedIPAddress;
					dispatchMessage.dstIP = neighborIP;
					dispatchMessage.routerID = router.rd.simulatedIPAddress;
					dispatchMessage.neighborID = router.rd.simulatedIPAddress;

					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(dispatchMessage);

					//prepare to get a second hello back
					SOSPFPacket secondMessage = (SOSPFPacket) new ObjectInputStream(socket.getInputStream()).readObject();
                    //received message for a second time. set status of router2 as TWO_WAY
					if(secondMessage.sospfType == 0) {
						System.out.println("received Hello from " + secondMessage.neighborID);
						neighborRouter.status = RouterStatus.TWO_WAY;
						System.out.println("set " + neighborRouter.simulatedIPAddress + " state to TWO_WAY.\n");

						//add router to link array
						for (int i = 0; i<4; i++) {
							if(router.ports[i] == null) {
								router.ports[i] = new Link(router.rd, neighborRouter);
								break;
							}
						}
					}

				}
			}
		}
*/
		catch (IOException e) {
            e.printStackTrace();
		}
		catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
	}
}
