package cn.edu.sustech.cs209.chatting.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class User {
  private String userName;
  private Socket userSocket; //服务器只做计算，所有打印跟随用户
  private InputStream uis;
  private OutputStream uos;

  private List<String> partners = new ArrayList<>();
  private List<Room> roomList = new ArrayList<>();

  private String currentRoom = "";
  public User(String userName, Socket socket) throws IOException {
    this.userName = userName;
    this.userSocket = socket;
    this.uis = socket.getInputStream();//从socket拿取信息
    this.uos = socket.getOutputStream(); //写进socket

    //还有一个好处是我们的服务器线程可以去print一些基本信息，知道哪里出问题
  }

  public String getUserName() {
    return userName;
  }

  public Socket getUserSocket() {
    return userSocket;
  }

  public InputStream getUis() {
    return uis;
  }

  public OutputStream getUos() {
    return uos;
  }

  public List<String> getPartners() {
    return partners;
  }

  public List<Room> getRoomList() {
    return roomList;
  }

  public String getCurrentRoom() {
    return currentRoom;
  }

  public void setCurrentRoom(String currentRoom) {
    this.currentRoom = currentRoom;
  }
}
