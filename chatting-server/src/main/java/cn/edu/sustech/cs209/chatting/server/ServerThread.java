package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Room;
import cn.edu.sustech.cs209.chatting.common.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerThread extends Thread {

  private BufferedReader br;
  private boolean login = false;
  private PrintWriter pw;
  private User user;
//  private List<String> partnerList;
//  private List<Room> roomList;

  private Socket userSocket;

  private boolean exit = false;

  public ServerThread(Socket socket) throws IOException {
    this.userSocket = socket;
//    this.partnerList = new ArrayList<>();
//    this.roomList = new ArrayList<>();
    this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));//从socket拿取信息
    this.pw = new PrintWriter(socket.getOutputStream());
  }

  @Override
  public void run() {
//    System.out.println("Hi, I am alive");
    try {
      while (true) {
        String msg = br.readLine();
        if (!msg.equals("")) {
          System.out.println("The msg" + msg);
          destructMessage(msg);
        }
      }
    } catch (SocketException e) {
      System.out.println("Socket<" + userSocket.getPort() + ">" + "has logged out.");
    } catch (IOException e) {
      System.out.println("Error in readLine");
    } finally {
      //在用户端线程关闭的时候，服务器线程通过user调用杀死进程
      //关闭进程
      //TODO：改变总服务器的在线人数和列表，并且由该线程广播，然后再死
      if (login) {
        System.out.println("here");
        Server.onlineUser--;
        Server.onlineUserList.remove(this.user);
        // code = 101 更新在线列表，一般是上线或者下线,数量和名字一起
        // 简短的信息放前面
        sendOnlineMessage(wrapper(101, "<num>" + Server.onlineUser + "</num>" + getOnlineUsers()));
        // 广播完了，现在线程可以死了
        // TODO：可能有个房间的提示好一点，但是目前先这样吧
        try {
          //全部关闭
          user.getBr().close();
          user.getUserSocket().close();
          user.getPw().close();
          exit = true;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


  /**
   * String builders
   */
  private String getAllRooms(User user) {
    //针对单个user自己的list
    List<Room> roomsRelated = user.getRoomList();
    StringBuffer stringBuffer = new StringBuffer();
    //经常更新所以应该问题不大
    stringBuffer.append("<name>");
    for (Room room : roomsRelated) {
      stringBuffer.append(room.getRoomName()).append("-");
    }
    stringBuffer.append("</name>");
    return stringBuffer.toString();
  }

  private String getOnlineUsers() {
    List<User> onlineUsers = Server.onlineUserList;
    StringBuffer stringBuffer = new StringBuffer();
    //经常更新所以应该问题不大
    stringBuffer.append("<name>");
    for (User user : onlineUsers) {
      stringBuffer.append(user.getUserName()).append(",");
    }
    stringBuffer.append("</name>");
    return stringBuffer.toString();
  }

  private String getChatHistory(Room room) {
    List<Message> chatHistory = room.getChatHistory();
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("<chat>");
    for (Message message : chatHistory
    ) {
      stringBuffer.append(message.getSentBy())
          .append(",").append(message.getData())
              .append(",").append("-");
      //send,data,-
    }
    stringBuffer.append("</chat>");
    return stringBuffer.toString();
  }

  private void destructMessage(String message) throws IOException {

    String code = null;
    String msg = null;
    if (message.length() > 0) {
      Pattern codePattern = Pattern.compile("<code>(.*)</code>");
      Matcher codeMatcher = codePattern.matcher(message);
      if (codeMatcher.find()) {
        code = codeMatcher.group(1);
      }
      Pattern messagePattern = Pattern.compile("<msg>(.*)</msg>");
      Matcher msgMatcher = messagePattern.matcher(message);
      if (msgMatcher.find()) {
        msg = msgMatcher.group(1);
      }
    }

    switch (code) {
      case "checkUserName":
        boolean flag = checkUserName(msg);
        System.out.println(flag);
        if (flag) {
          User user = new User(msg, this.userSocket);
          this.user = user;
          Server.onlineUserList.add(user);
          Server.onlineUser++;
          this.login = true;
          sendOnlineMessage(
              wrapper(101, "<num>" + Server.onlineUser + "</num>" + getOnlineUsers()));
          returnMessage(wrapper(201, String.valueOf(Server.onlineUser)));
        } else {
          returnMessage(wrapper(202, "false"));
        }
        break;
      case "exit":
        exit = true;
        //在用户端线程关闭的时候，服务器线程通过user调用杀死进程
        //关闭进程
        //TODO：改变总服务器的在线人数和列表，并且由该线程广播，然后再死
        if (login) {
          Server.onlineUser--;
          Server.onlineUserList.remove(this.user);
          System.out.println("hi" + Server.onlineUser);
        }
        // code = 101 更新在线列表，一般是上线或者下线,数量和名字一起
        // 简短的信息放前面

        sendOnlineMessage(wrapper(101, "<num>" + Server.onlineUser + "</num>" + getOnlineUsers()));
        returnMessage(wrapper(301, "true"));
        break;

      case "join":

        break;

      case "privateCreate":
        //username
        String partnerName = msg;
        boolean isUsernameExist = this.user.getPartners().contains(partnerName);
        System.out.println("The username exists:" + isUsernameExist);
        if (isUsernameExist) {
          //有房了，可以直接返回对应的房子
          Room existRoom = Server.roomList.stream()
              .filter(u -> u.getRoomName().equals(user.getUserName() + "," + partnerName))
              .findFirst()
              .orElse(null);
          if (existRoom == null) {
            existRoom = Server.roomList.stream()
                .filter(u -> u.getRoomName().equals( partnerName + "," + user.getUserName()))
                .findFirst()
                .orElse(null);
          }

          //有房就要返回名字和历史
          //不改变roomList
          //roomSwitch, 返回roomList,room, chatlist
          if (existRoom != null) {
            this.user.setCurrentRoom(existRoom.getRoomName());
            //确保进入下一个函数的时候，大家都发current
            sendGroupPartnerMessage(existRoom);
          }

          break;
        } else {
          //不存在,在在线的朋友这里找
          User partner = Server.onlineUserList.stream()
              .filter(u -> u.getUserName().equals(partnerName))
              .findFirst()
              .orElse(null);
          //建房
          Room doubleRoom = new Room(user.getUserName() + "," + partnerName);
//          System.out.println(user.getUserName() + "," + partnerName);
          doubleRoom.setTotalUsers(2);
          doubleRoom.getUserList().add(user);
          doubleRoom.getUserList().add(partner);

          //成为彼此的新拍档
          assert partner != null;
          this.user.getPartners().add(partner.getUserName());
          partner.getPartners().add(this.user.getUserName());
          //上服务器，改子服务器
          this.user.getRoomList().add(doubleRoom);
          partner.getRoomList().add(doubleRoom);
          Server.roomList.add(doubleRoom);
          //广播 join
          this.user.setCurrentRoom(doubleRoom.getRoomName());
          sendGroupPartnerMessage(doubleRoom);
          break;
        }

      case "groupCreate":
        //string of usernames
        //username
        String groupName = msg;
        Room groupRoom = new Room(groupName);
        List<User> usersInGroup = groupRoom.getUserList();
        usersInGroup.add(user);//房主
        String[] users = groupName.split(",");
        int size = users.length;
        for (int i = 0; i < size; i++) {
          int finalI = i;
          User user = Server.onlineUserList.stream()
              .filter(u -> u.getUserName().equals(users[finalI]))
              .findFirst()
              .orElse(null);
          user.getRoomList().add(groupRoom);
          //直接往里面加人
          usersInGroup.add(user);
        }
        //建房
        groupRoom.setTotalUsers(size);
        //上服务器，改子服务器
        this.user.getRoomList().add(groupRoom);
        Server.roomList.add(groupRoom);
        //广播
        sendGroupPartnerMessage(groupRoom);
        break;
      case "roomSwitch":
        //roomname
        String switchToRoom = msg;
        Room switchRoom = this.user.getRoomList().stream()
            .filter(u -> u.getRoomName().equals(switchToRoom))
            .findFirst()
            .orElse(null);
        assert switchRoom != null;
        this.user.setCurrentRoom(switchRoom.getRoomName());
        returnMessage(wrapper(403,
            "<roomname>" + switchRoom.getRoomName() + "</roomname>"
                + getChatHistory(switchRoom)));
        break;
      case "sendMessage":
        //send, message
        //拆分roomname
//        Pattern codePattern = Pattern.compile("<roomname>(.*)</roomname>");
//        assert msg != null;
//        Matcher codeMatcher = codePattern.matcher(msg);
        String currentRoomName= this.user.getCurrentRoom();

//        if (codeMatcher.find()) {
//          roomname = codeMatcher.group(1);
//        } else {
//          roomname = "";
//        }
//        Pattern codePattern = Pattern.compile("<data>(.*)</data>");
//        assert msg != null;
//        Matcher codeMatcher = codePattern.matcher(msg);
//        String dataSent;
//        if (codeMatcher.find()) {
//          dataSent = codeMatcher.group(1);
//        } else {
//          dataSent = "";
//        }
        String sentBy = this.user.getUserName();
        String data = msg;
        Message newMessage = new Message(sentBy, data);
        //房间历史变化，并且返回只更新房间内聊天室的内容
        Room currentRoom = this.user.getRoomList().stream()
            .filter(r -> r.getRoomName().equals(currentRoomName))
            .findFirst()
            .orElse(null);
        assert currentRoom != null;
        currentRoom.getChatHistory().add(newMessage);
        //只改这个发信息的人和在聊天室里面的
        sendPartnerInRoom(currentRoom);
        break;
      default:
        System.out.println("No valid code from socket<" + this.userSocket.getPort() + ">");
    }
  }


  private boolean checkUserName(String username) throws IOException {
    System.out.println("> I am checking userName....");
    return !Server.onlineUserList.stream().anyMatch(user -> user.getUserName().equals(username));
  }

  private String wrapper(int code, String msg) {
    return "<code>" + code + "</code><msg>" + msg + "</msg>\n";
  }

  /**
   * Message senders
   */
  private void returnMessage(String message) {
    pw.println(message);
    pw.flush();
  }

  //给所有在线的朋友们发信息
  private void sendOnlineMessage(String message) {
    //TODO：如果大家都去改在线的...就不太合理
    PrintWriter pw;
    for (User user : Server.onlineUserList) {
      try {
        pw = user.getPw();
        pw.println(message);
        pw.flush();
      } catch (Exception e) {
        System.out.println("Error in sendMessage()");
      }
    }
  }

  private void sendGroupPartnerMessage(Room room) {
    PrintWriter pw;
    System.out.println("------"+room.getRoomName());
    //改的是房间渲染，可以不发chatHistory
    List<User> users = room.getUserList();
    String str = "";
    for (User u : users) {
      if (u.getCurrentRoom().equals(room.getRoomName())) {
        str = wrapper(402,
            getAllRooms(u) + "<roomname>" + room.getRoomName() + "</roomname>" + getChatHistory(
                room));//当前创建了新的房间的人
      } else {
        str = wrapper(402,
            getAllRooms(u) + "<roomname>" + u.getCurrentRoom() + "</roomname>"
        );
      }
      pw = u.getPw();
      pw.println(str);
      pw.flush();
    }
  }

  private void sendPartnerInRoom(Room room) {
    PrintWriter pw;
    //send message, 更新聊天记录
    List<User> parternersInRoom = room.getUserList();
    for (User u : parternersInRoom) {
      if (u.getCurrentRoom().equals(room.getRoomName())) {
        //如果是在聊天中的
        String str = wrapper(502,
            getAllRooms(u) + "<roomname>" + room.getRoomName() + "</roomname>"
                + getChatHistory(room));
        pw = u.getPw();
        pw.println(str);
        pw.flush();
      }
    }
  }

}
