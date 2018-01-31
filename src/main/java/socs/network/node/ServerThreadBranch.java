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
	}
}
