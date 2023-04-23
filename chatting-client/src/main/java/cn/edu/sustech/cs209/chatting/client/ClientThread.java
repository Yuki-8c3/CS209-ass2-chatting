package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientThread extends Thread {
  // 在线程里创建一个Controller管理
  // 有自己的套接字收来自server的信息
  // 有自己的读写器

  private Socket clientSocket;
  private Controller controller; //controller里也有一个对应的线程
  private BufferedReader br;
  private PrintWriter pw;
  //新增一个volatile变量，控制线程退出

  //从Controller传入的socket和client，严格意义上等于一个线程共用在两个类里
  public ClientThread(Socket socket, Controller controller) {
    this.controller = controller;
    this.clientSocket = socket;
    try {
      br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      pw = new PrintWriter(socket.getOutputStream());
    } catch (IOException e) {
      throw new RuntimeException("cannot get input stream from socket");
    }
  }

  /**
   * 不间断读数据和处理, 线程启动之后马上调用这个方法 调用controller的方法
   */
  @Override
  public void run() {
    try {
      br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      pw = new PrintWriter(clientSocket.getOutputStream());
      //一般都会已经创好了
      // br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      while (true) {
        String msg = br.readLine(); //读取发送过来的信息
        if (!msg.equals("")) {
          System.out.println(msg);
          // TODO：信息的解析包装
          destructMessage(msg);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("It's me");
      System.exit(0);
    }
  }


  private String wrapper(String code, String msg) {
    return "<code>" + code + "</code><msg>" + msg + "</msg>\n";
  }

  public void sendMessage(String msg){
    System.out.println("SEND---------------->" + msg);
    String[] lines = msg.split("\n");
    if(lines.length == 1) {
      pw.println(wrapper("sendMessage",msg));
      pw.flush();
      return;
    }
    StringBuffer sb = new StringBuffer();
    for (String line : lines) {
      sb.append(line).append("&");
    }
    pw.println(wrapper("sendMessage",sb.toString()));
    pw.flush();
  }
  public void switchGroup(String groupName){
//    System.out.println("!!!!!!!!!!switch to "+ groupName);
    pw.println(wrapper("roomSwitch", groupName));
    pw.flush();
  }
  public void createPrivate(String username) {
    pw.println(wrapper("privateCreate", username));
    pw.flush();
  }

  public void createGroup(List<String> userList) {
    StringBuffer stringBuffer = new StringBuffer();
    for (String str: userList
    ) {
      stringBuffer.append(str).append(",");
    }
    pw.println(wrapper("groupCreate", stringBuffer.toString()));
    pw.flush();
  }

  /**
   * 解析服务器发送的信息
   */
  private void destructMessage(String message) {
//    System.out.println(message);
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
      case "101":
//        System.out.println(msg);
        Pattern codePattern = Pattern.compile("<num>(.*)</num>");
        Matcher codeMatcher = codePattern.matcher(msg);
        String num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        controller.setNum(num);
        Pattern messagePattern = Pattern.compile("<name>(.*)</name>");
        Matcher msgMatcher = messagePattern.matcher(msg);
        String resultList = "";
        if (msgMatcher.find()) {
          resultList= msgMatcher.group(1);
        }
        String[] userArray = resultList.split(",");
        controller.setUsernames(userArray);
        break;
      case "201":
        // 201: 用户是否已经存在，true-可以用这个名字
        controller.setName(msg);
        break;
      case "202":
        controller.alert("Invalid name");
        break;
      case "301":
        // 301：exit
        if (msg.equals("true")) {
          controller.exit();
        } else {
          System.out.println("some error in server!");
        }
        break;
      case "403":
        //current + history
        codePattern = Pattern.compile("<roomname>(.*)</roomname>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        System.out.println("roomname is" + num);
        String curroom = num;
        controller.setCurrentRoom(num);
        //ChatHistory
        List<Message> messageList = new ArrayList<>();
        codePattern = Pattern.compile("<chat>(.*)</chat>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }

        if (num.equals("")) {
          break;
        }
        String[] mess = num.split("-");
        for (String mes: mess
        ) {
          String[] str = mes.split(",");
          Message newMessage = new Message(str[0], str[1]);
          messageList.add(newMessage);
        }
        controller.setChatContentList(messageList, curroom);
        break;

      case "402":
        //roomList
        codePattern = Pattern.compile("<name>(.*)</name>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
//          System.out.println(num);
        }
        String[] roomArray = num.split("-");
//        for (String s: roomArray
//        ) {
//          System.out.println(s);
//        }
        controller.setChatList(roomArray);
        //currentRoomName
        codePattern = Pattern.compile("<roomname>(.*)</roomname>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        System.out.println("roomname is" + num);
        curroom = num;
        controller.setCurrentRoom(num);
        //ChatHistory
        messageList = new ArrayList<>();
        codePattern = Pattern.compile("<chat>(.*)</chat>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        if (num.equals("")) {
          break;
        }
        mess = num.split("-");
        for (String mes: mess
        ) {
          String[] str = mes.split(",");
//          System.out.println(str.length);
          Message newMessage = new Message(str[0], str[1]);
          messageList.add(newMessage);
        }
        controller.setChatContentList(messageList, curroom);
        break;
      case "502":
        //roomList
        codePattern = Pattern.compile("<name>(.*)</name>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
//          System.out.println(num);
        }
        roomArray = num.split("-");
//        for (String s: roomArray
//        ) {
//          System.out.println(s);
//        }
        controller.setChatList(roomArray);
        //currentRoomName
        codePattern = Pattern.compile("<roomname>(.*)</roomname>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        System.out.println("roomname is" + num);
        curroom = num;
        controller.setCurrentRoom(num);
        //ChatHistory
        messageList = new ArrayList<>();
        codePattern = Pattern.compile("<chat>(.*)</chat>");
        codeMatcher = codePattern.matcher(msg);
        num = "";
        if (codeMatcher.find()) {
          num = codeMatcher.group(1);
        }
        if (num.equals("")) {
          break;
        }
        mess = num.split("-");
        for (String mes: mess
        ) {
          String[] str = mes.split(",");
          Message newMessage = new Message(str[0], str[1]);
          messageList.add(newMessage);
        }
        controller.setChatContentList(messageList, curroom);
        break;
      default:
        System.out.println("Nothing matches the code from server!" + code);
    }
  }

  public void leave() throws IOException {
    pw.println(wrapper("exit", ""));
    pw.flush();
  }

  public void checkUsername(String username) {
    pw.println(wrapper("checkUserName", username));
    pw.flush();
  }

  public BufferedReader getBr() {
    return br;
  }

  public PrintWriter getPw() {
    return pw;
  }

  public Socket getClientSocket() {
    return clientSocket;
  }

  public Controller getController() {
    return controller;
  }
}
