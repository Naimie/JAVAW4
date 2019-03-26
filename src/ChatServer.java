// Peter Idestam-Almquist, 2017-03-10.
// Server, multi-threaded, accepting several simultaneous clients.

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class ChatServer implements Runnable {
    private final static int PORT = 8000;
    private final static int MAX_CLIENTS = 5;
    private final static Executor executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    private static CopyOnWriteArrayList<ChatServer> clientConnections = new CopyOnWriteArrayList<>();
    private static ConcurrentHashMap<ChatServer, ArrayList<String>> messages = new ConcurrentHashMap<>();
    private boolean isFirstMessage = true;
    private final Socket clientSocket;
    private String clientName = "";

	/*private class ClientHolder{
		private String name;
		private Socket clientSocket;

		public ClientHolder(String name, Socket clientSocket){
			this.name = name;
			this.clientSocket = clientSocket;
		}

		public String getName() {
			return name;
		}

		public Socket getSocket() {
			return clientSocket;
		}
	}*/

    private ChatServer(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        SocketAddress remoteSocketAddress = clientSocket.getRemoteSocketAddress();
        SocketAddress localSocketAddress = clientSocket.getLocalSocketAddress();
        System.out.println("Accepted client " + remoteSocketAddress
                + " (" + localSocketAddress + ").");

        PrintWriter socketWriter = null;
        BufferedReader socketReader = null;
        try {
            socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            socketReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            String threadInfo = " (" + Thread.currentThread().getName() + ").";
            String inputLine = socketReader.readLine();
            System.out.println("Received: \"" + inputLine + "\" from "
                    + remoteSocketAddress + threadInfo);

            // First message is client name.
            clientName = inputLine;
            clientConnections.add(this);

            while (inputLine != null) {
                if (isFirstMessage) {
                    inputLine = socketReader.readLine();
                    System.out.println("Received: \"" + inputLine + "\" from "
                            + clientName + " " + remoteSocketAddress + threadInfo);
                    isFirstMessage = false;
                } else {
                    for (List<String> list : messages.values()) {
                        list.add(clientName + ": " + inputLine);
                    }
                    for (String s : messages.get(this)) {
                        socketWriter.println(s);
                    }
                    messages.get(this).clear();

                        //messages.add(clientName + ": " + inputLine);

                    /*socketWriter.println(inputLine);
                    System.out.println("Sent: \"" + inputLine + "\" to "
                            + clientName + " " + remoteSocketAddress + threadInfo);
                    inputLine = socketReader.readLine();
                    System.out.println("Received: \"" + inputLine + "\" from "
                            + clientName + " " + remoteSocketAddress + threadInfo);*/
                }

            }
            System.out.println("Closing connection " + remoteSocketAddress
                    + " (" + localSocketAddress + ").");
        } catch (Exception exception) {
            System.out.println(exception);
        } finally {
            try {
                if (socketWriter != null)
                    socketWriter.close();
                if (socketReader != null)
                    socketReader.close();
                if (clientSocket != null)
                    clientSocket.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Server2 started.");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            SocketAddress serverSocketAddress = serverSocket.getLocalSocketAddress();
            System.out.println("Listening (" + serverSocketAddress + ").");

            while (true) {
                clientSocket = serverSocket.accept();
                executor.execute(new ChatServer(clientSocket));
            }
        } catch (Exception exception) {
            System.out.println(exception);
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (Exception exception) {
                System.out.println(exception);
            }
        }
    }
}
