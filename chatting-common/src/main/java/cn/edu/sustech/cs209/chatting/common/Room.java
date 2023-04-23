package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class Room {
  //Room不分一对一或者group
   private String roomName;
   private List<User> userList;
   private int totalUsers = 0;

   private List<Message> chatHistory;

   public Room(String roomName) {
     this.roomName = roomName;
     this.totalUsers = 0;
     this.userList = new ArrayList<>();
     this.chatHistory = new ArrayList<>();
   }

  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public int getTotalUsers() {
    return totalUsers;
  }

  public void setTotalUsers(int totalUsers) {
    this.totalUsers = totalUsers;
  }

  public List<User> getUserList() {
    return userList;
  }

  public void setUserList(List<User> userList) {
    this.userList = userList;
  }

  public List<Message> getChatHistory() {
    return chatHistory;
  }

  public void setChatHistory(List<Message> chatHistory) {
    this.chatHistory = chatHistory;
  }
}
