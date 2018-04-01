package socs.network.node;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.message.LSA;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketTimeoutException;
import java.util.TimerTask;
import java.net.Socket;

public class HeartbeatTask extends TimerTask {
    private Router router;
    HeartbeatTask(Router r) {
        router = r;
    }
    public void run() {
        for (int i = 0; i < router.ports.length; ++i) {
            if (router.ports[i] != null && router.ports[i].router2.status == RouterStatus.TWO_WAY) {
                Socket client;
                try {
                    client = new Socket(router.ports[i].router2.processIPAddress, router.ports[i].router2.processPortNumber);
                } catch (IOException e) {
                    LSA lsa = router.lsd._store.get(router.rd.simulatedIPAddress);
                    for(LinkDescription ld: lsa.links) {
                        if(ld.linkID.equals(router.ports[i].router2.simulatedIPAddress)) {
                            lsa.links.remove(ld);
                            break;
                        }
                    }
                    lsa.lsaSeqNumber++;
                    router.ports[i] = null;
                    try {
                        router.LSAUPDATE();
                    } catch (IOException ee) {
                        ee.printStackTrace();
                    }
                    continue;
                }
                    SOSPFPacket cPacket = new SOSPFPacket();
                    cPacket.srcProcessIP = router.rd.processIPAddress;
                    cPacket.srcProcessPort = router.rd.processPortNumber;
                    cPacket.srcIP = router.rd.simulatedIPAddress;
                    cPacket.dstIP = router.ports[i].router2.simulatedIPAddress;
                    cPacket.sospfType = 2;
                    cPacket.routerID = router.rd.simulatedIPAddress;
                    cPacket.neighborID = router.rd.simulatedIPAddress;
                    cPacket.weight = router.ports[i].weight;
                    ObjectOutputStream outToServer = null;
                    try {
                        outToServer = new ObjectOutputStream(client.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if(outToServer != null) {
                            outToServer.writeObject(cPacket);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        client.setSoTimeout(3000);
                        DataInputStream inFromServer = new DataInputStream(client.getInputStream());
                    } catch (SocketTimeoutException e) {
                        LSA lsa = router.lsd._store.get(router.rd.simulatedIPAddress);
                        for(LinkDescription ld: lsa.links) {
                            if(ld.linkID.equals(router.ports[i].router2.simulatedIPAddress)) {
                                lsa.links.remove(ld);
                                break;
                            }
                        }
                        lsa.lsaSeqNumber++;
                        router.ports[i] = null;
                        try {
                            router.LSAUPDATE();
                        } catch (IOException ee) {
                            ee.printStackTrace();
                        }
                    }
                    catch (IOException ee) {
                        ee.printStackTrace();
                    }
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
