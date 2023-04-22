package cn.edu.sustech.cs209.chatting.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientThread extends Thread{
  // 在线程里创建一个Controller管理
  // 有自己的套接字收来自server的信息
  // 有自己的读写器
  private Socket clientSocket;
  private Controller controller; //controller里也有一个对应的线程
  private BufferedReader br;
  private PrintWriter pw;

  //从Controller传入的socket和client，严格意义上等于一个线程共用在两个类里
  public ClientThread(Socket socket, Controller controller) {
    this.controller = controller;
    this.clientSocket = socket;
    try{
      br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (IOException e) {
      throw new RuntimeException("cannot get input stream from socket");
    }
  }

  /**
   * 不间断读数据和处理, 线程启动之后马上调用这个方法
   * 调用controller的方法
   */
  @Override
  public void run() {
    try{
      //一般都会已经创好了
      br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      while(true) {
        String msg = br.readLine(); //读取发送过来的信息
        // TODO：信息的解析包装
        System.out.println(msg);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 解析服务器发送的信息
   */
  public void dismantleMessage(String message) {
    String code = null; //什么类型的指令,服务器希望客户端做什么
    String msg = null;  //参数信息
    /**
     * 正则表达式进行匹配
     */
    if (message.length() > 0) {
      Pattern pattern = Pattern.compile("<code>(.*)</code>");
      Matcher matcher = pattern.matcher(message);
    }
  }
}
