package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Room;
import cn.edu.sustech.cs209.chatting.common.User;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {
    //类似于总服务器
    private int port;

    public static int onlineUser = 0;
    public static List<User> onlineUserList;
    //先测试注册的用户能不能同步并且显示
//    private List<User> userList;
    private ServerSocket serverSocket;
    //管理服务线程

    public static List<Room> roomList;


    public Server(int port) throws Exception{
        this.port = port;
        onlineUserList = new ArrayList<>();
        roomList = new ArrayList<>();
//        this.userList = new ArrayList<>();
        serverSocket = new ServerSocket(port);
        System.out.println("Server is started");
    }

    public void listen() throws Exception{
        while (true) {
            Socket socket = serverSocket.accept();
            int port = socket.getPort();
            System.out.println("The new client is:"+port);
            //创建新的用户并且入表，分配新的线程并且参数转移
            //有一个信息的解构这里放在单独的线程里完成，所以用户在服务器线程内构建
            ServerThread thread = new ServerThread(socket);
            thread.start();
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

//    public List<User> getUserList() {
//        return userList;
//    }
//
//    public void setUserList(List<User> userList) {
//        this.userList = userList;
//    }
}
