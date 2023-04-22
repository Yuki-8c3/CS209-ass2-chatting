package cn.edu.sustech.cs209.chatting.server;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;
    private ServerSocket serverSocket;

    public Server(int port) throws Exception{
        this.port = port;
        serverSocket = new ServerSocket(port);
        System.out.println("Server is started");
    }

    public void listen() throws Exception{
        while (true) {
            Socket socket = serverSocket.accept();
            int port = socket.getPort();
            System.out.println("The new client is:"+port);
        }
    }
    public static void main(String[] args) {
        System.out.println("Starting server");
        //监听端口号6666
        try{
            Server server = new Server(6666);
            server.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
