// Kristina Elmgren


import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.*;

class ChatServer implements Runnable {
    private final static int PORT = 8000;
    private final static int MAX_CLIENTS = 5;
    private final static Executor executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    private static CopyOnWriteArrayList<PrintWriter> clientConnections = new CopyOnWriteArrayList<>();
    private static BlockingQueue<String> messages = new ArrayBlockingQueue<>(100);
    private boolean isFirstMessage = true;
    private final Socket clientSocket;
    private String clientName = "";
    private ClientMessageHandler messageHandler;


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
            messageHandler = new ClientMessageHandler();
            Thread clientMsgHandler = new Thread(messageHandler);
                   clientMsgHandler .start();

            String threadInfo = " (" + Thread.currentThread().getName() + ").";
            String inputLine = socketReader.readLine();
            System.out.println("Received: \"" + inputLine + "\" from "
                    + remoteSocketAddress + threadInfo);

            // First message is client name.
            clientName = inputLine;
            clientConnections.add(socketWriter);
            String msg = "";


            while (inputLine != null) {
                if (isFirstMessage) {
                    inputLine = socketReader.readLine();
                    System.out.println("Received: \"" + inputLine + "\" from "
                            + clientName + " " + remoteSocketAddress + threadInfo);
                    isFirstMessage = false;
                } else {
                    msg = clientName + ": " + inputLine;
                    messages.put(msg);
                    inputLine = socketReader.readLine();
                    //messages.get(this).clear();

                    //messages.add(clientName + ": " + inputLine);

                    //socketWriter.println(msg);
                    /*System.out.println("Sent: \"" + inputLine + "\" to "
                            + clientName + " " + remoteSocketAddress + threadInfo);

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
            clientConnections.remove(socketWriter);
            messageHandler.stop();

        }
    }

    private class ClientMessageHandler implements Runnable {
        boolean shouldRun = true;
        @Override
        public void run() {

            String msg = "";

            while (shouldRun) {
                try {
                    msg = messages.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(PrintWriter p : clientConnections){
                    p.println(msg);
                }
                try{
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
        }
        public void stop(){
            shouldRun = false;

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
