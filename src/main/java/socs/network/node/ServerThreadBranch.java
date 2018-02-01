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
			SOSPFPacket listenMessage = null;
			//router received a hello
			listenMessage = (SOSPFPacket) new ObjectInputStream(socket.getInputStream()).readObject();
			if (listenMessage.sospfType == 0) {
				String neighborIP = listenMessage.neighborID;
				boolean initialHello = true;

				//to check if message received from router is the first time
				for (int i = 0; i < router.ports.length; ++i) {
					if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(neighborIP)) {
						initialHello = false;
						break;
					}
				}

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
		catch (IOException e) {
		}
		catch (ClassNotFoundException c) {
        }
	}
}
