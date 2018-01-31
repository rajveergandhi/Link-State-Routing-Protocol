package socs.network.node;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

public class ServerThread extends Thread {

	private Router router;
	private ServerSocket socket;
	
	public ServerThread(ServerSocket sock, Router rout){
        socket = sock;
        router = rout;
	}

	public void run() {
	    while(true){
            try {
                Socket subSocket = socket.accept();
                ServerThreadBranch sBranch = new ServerThreadBranch(subSocket, router);
                sBranch.start();
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
	    }
	}
}
