package socs.network.node;
import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.*;
import java.net.*;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;


public class Router {

  protected LinkStateDatabase lsd;
  public RouterDescription rd = new RouterDescription();
  public ServerThread serverThread;
  public ServerSocket serverSocket;

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) throws IOException {

    // get simulated IP and port number from config
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.port");

    //hardcoded to localhost since in this simulation, all router instances are run on the same machine
    rd.processIPAddress = "127.0.0.1";

    // initialize link state database
    lsd = new LinkStateDatabase(rd);

    // initialize main server socket and main server thread, and start() it
    serverSocket = new ServerSocket(rd.processPortNumber);
    serverThread = new ServerThread(serverSocket, this); 
    serverThread.start();

  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
      if (destinationIP.equals(rd.simulatedIPAddress)) {
          //Distance 0
      }
      else {
          //shortest path
      }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {

      for (int i = 0; i < ports.length; ++i) {
          if (ports[i] != null) {
              if (ports[i].router1.simulatedIPAddress.equals(simulatedIP)) {
                  // router1 cannot be this router instance
                  System.err.println("Error: router1's simulated IP is the same as the current router");
                  return;
              }
              else if (ports[i].router2.simulatedIPAddress.equals(simulatedIP)) {
                  // router2 is already attached to this router instance
                  System.err.println("Error: The given router is already attached to this router");
                  return;
              }
          }
          else { // there is an available entry in the ports array

              // create a new RouterDescription instance for the router being attached at the other end of the Link
              RouterDescription r2 = new RouterDescription();
              r2.processIPAddress = processIP;
              r2.processPortNumber = processPort;
              r2.simulatedIPAddress = simulatedIP;
              r2.status = RouterStatus.INIT; // initialize to INIT

              ports[i] = new Link(rd, r2, weight);
              System.out.println("Successfully attached link");
              return;
          }
      }

      // ports array is full
      System.err.println("Error: no free ports available for this router");
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() throws ClassNotFoundException, UnknownHostException, IOException {

    for (int i = 0; i < ports.length; ++i) {

        // broadcast to all routers that have not already been synced (that are not already TWO_WAY)
        if (ports[i] != null && ports[i].router2.status != RouterStatus.TWO_WAY) {

            // create and initialize the packet for broadcast
            SOSPFPacket cPacket = new SOSPFPacket();
            cPacket.srcProcessIP = rd.processIPAddress;
            cPacket.srcProcessPort = rd.processPortNumber;
            cPacket.srcIP = rd.simulatedIPAddress;
            cPacket.dstIP = ports[i].router2.simulatedIPAddress;
            cPacket.sospfType = 0; // HELLO
            cPacket.routerID = rd.simulatedIPAddress;
            cPacket.neighborID = rd.simulatedIPAddress;
            cPacket.weight = ports[i].weight;
            // send packet to the server
            Socket client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
            ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
            outToServer.writeObject(cPacket);

            // wait for reply from the router we just initiated sync with
            ObjectInputStream inFromServer = new ObjectInputStream(client.getInputStream());
            SOSPFPacket packet = (SOSPFPacket) inFromServer.readObject();
            if(packet.sospfType == 0) {

                // set status to TWO_WAY
                System.out.println("received HELLO from " + packet.neighborID + ";");
                ports[i].router2.status = RouterStatus.TWO_WAY;
                System.out.println("set " + ports[i].router2.simulatedIPAddress + " state to TWO_WAY;");

                // reply with HELLO for the sync
                outToServer.writeObject(cPacket);
            }

            // close socket
            client.close();

            // link state database
            LinkDescription linkDB = new LinkDescription();
            linkDB.linkID = ports[i].router2.simulatedIPAddress;
            linkDB.portNum = i;
            linkDB.tosMetrics = ports[i].weight;
            lsd.addLinkLSA(rd.simulatedIPAddress, linkDB);
        }
    }
    LSAUPDATE();
  }

  public void LSAUPDATE() throws IOException {
      for (int i = 0; i<ports.length; ++i) {
          if (ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY) {
              Socket client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
              SOSPFPacket packet = new SOSPFPacket();
              packet.srcProcessIP = rd.processIPAddress;
              packet.srcProcessPort = rd.processPortNumber;
              packet.srcIP = rd.simulatedIPAddress;
              packet.dstIP = ports[i].router2.simulatedIPAddress;
              packet.sospfType = 1;
              packet.routerID = rd.simulatedIPAddress;
              packet.neighborID = rd.simulatedIPAddress;
              packet.lsaArray = new Vector<LSA>();
              for (LSA lsa: lsd._store.values()) {
                  packet.lsaArray.addElement(lsa);
              }
              ObjectOutputStream outToServer = new ObjectOutputStream(client.getOutputStream());
              System.out.println("sending LSAUPDATE to neighbor: " + packet.dstIP);
              outToServer.writeObject(packet);
              client.close();
          }
      }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
      for (int i = 0; i < ports.length; ++i) {
          if(ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY) {
              System.out.println("IP Address of neighbour " + (i+1) + " is: " + ports[i].router2.simulatedIPAddress);
          }
      }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          if (cmdLine.length == 5) {
              processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                      cmdLine[3], Short.parseShort(cmdLine[4]));
          }
          else {
            System.err.println("Command not recognized.");
          }
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
            System.err.println("Command not recognized.");
            break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
