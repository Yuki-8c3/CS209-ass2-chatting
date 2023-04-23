package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList; //消息记录
  ObservableList<Message> messageObservableList = FXCollections.observableArrayList();
  @FXML
  ListView<String> chatList;
  ObservableList<String> roomObservableList = FXCollections.observableArrayList();
  @FXML
  TextArea inputArea;
  @FXML
  Label currentUsername;
  @FXML
  Label currentOnlineCnt;

  private ClientThread clientThread;

  String username;
  private StringProperty usernameProperty = new SimpleStringProperty();
  private StringProperty onlineCntProperty = new SimpleStringProperty();

  String[] users;
  String currentRoom;

  public void setCurrentRoom(String str) {
    //一般是创建了新的，所以先要清除所有的messages
    Platform.runLater(() -> {
      this.currentRoom = str;
      messageObservableList.clear();
      messageObservableList.add(new Message("System", "CURRENT ROOM:" + str));

    });

  }


  public void setChatContentList(List<Message> messages, String str) {
    Platform.runLater(() -> {
      this.currentRoom = str;
      messageObservableList.clear();
      messageObservableList.add(new Message("System", "CURRENT ROOM:" + this.currentRoom));
      for (Message m : messages
      ) {
        if (m.getData().contains("&")) {
          String changeLineData = m.getData();
          StringBuffer buffer = new StringBuffer();
          String[] strs = changeLineData.split("&");
          int size = strs.length;
          for (int i = 0; i < size - 1; i++) {
            buffer.append(strs[i]).append("\n");
          }
          buffer.append(strs[size - 1]);
          Message newMsg = new Message(m.getSentBy(), buffer.toString());
          messageObservableList.add(newMsg);
        } else {
          messageObservableList.add(m);
        }
      }
    });
  }

  public void setUsernames(String[] users) {
    this.users = users;
  }

  public void setChatList(String[] rooms) {
    Platform.runLater(() -> {
      roomObservableList.clear();
      roomObservableList.addAll(Arrays.asList(rooms));
    });
  }


  /**
   * 连接服务器，需要制定host和port
   *
   * @return
   */
  public boolean connect(String host, int port) {
    //这里和ClientThread关联，注意ClientThread过来的东西已经是处理过的了
    try {
      Socket socket = new Socket(host, port);
      System.out.println("I am on port:" + socket.getLocalPort());
      System.out.println("Connected to Server" + socket.getRemoteSocketAddress()); //返回地址和端口
      //创建一个解析消息的线程
      //传入当前客户端对象的指针，可以调用相应的处理函数
      ClientThread thread = new ClientThread(socket, this);
      clientThread = thread;
      thread.start();
      return true;
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      System.out.println("Server error");
      return false;
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    //服务器连接
    connect("localhost", 6666);
    //输入当前用户的用户名，如果不为空就赋值给username；否则退出程序
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText("I am on port:" + clientThread.getClientSocket().getLocalPort());
    dialog.setContentText("Username:");

    // 将currentUsername的text属性绑定到usernameProperty变量
    currentUsername.textProperty().bind(usernameProperty);
//    // 将currentOnlineCnt的text属性绑定到onlineCntProperty变量
    currentOnlineCnt.textProperty().bind(Bindings.concat("Online: ").concat(onlineCntProperty));
    inputArea.setText("");
    Optional<String> input = dialog.showAndWait();

    if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
      //check if there is a username like that
      username = input.get();
      clientThread.checkUsername(username); //且进行login
    } else if (!input.isPresent()) {
      try {
        clientThread.leave();
      } catch (IOException e) {
        System.out.println("Controller(103): clientThread.exit() error.");
      }
    } else {
      System.out.println("Invalid username " + input + ", exiting");

    }

    chatContentList.setCellFactory(new MessageCellFactory());
    chatContentList.setItems(messageObservableList);
    chatList.setCellFactory(new RoomCellFactory());
    chatList.setItems(roomObservableList);

  }

  public void alert(String text) {
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.WARNING);
      alert.setTitle("Warning");
      alert.setHeaderText(null);
      alert.setContentText(text);
      alert.showAndWait();
      destroy();
    });

  }

  public void setName(String cnt) {
    Platform.runLater(() -> {
      usernameProperty.set(username);
      onlineCntProperty.set(cnt);
    });
  }

  @FXML
  //创建私聊
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();

    ComboBox<String> userSel = new ComboBox<>(); //下拉框
    Button okBtn = new Button("OK"); //按钮

    int size = users.length;
    System.out.println(size);
    for (int i = 0; i < size; i++) {
      if (users[i].equals(username)) {
        continue;
      }
      userSel.getItems().add(users[i]);
    }
//    userSel.getItems().addAll("Item 1", "Item 2", "Item 3");

    okBtn.setOnAction(e -> {
      //将选择的用户保存到AtomicReference类型的user变量中
      user.set(userSel.getSelectionModel().getSelectedItem());
      clientThread.createPrivate(user.get());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.setOnCloseRequest(windowEvent -> {
      destroy();
    });
    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    // 标题为选定用户的用户名称
    stage.setTitle(currentRoom);
    stage.showAndWait();
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
    AtomicReference<String> user = new AtomicReference<>();
    Stage stage = new Stage();
    //多选框
    List<CheckBox> userSel = new ArrayList<>();
    Button okBtn = new Button("OK"); //按钮

    int size = users.length;
    System.out.println(size);
    for (int i = 0; i < size; i++) {
      if (users[i].equals(username)) {
        continue;
      }
      userSel.add(new CheckBox(users[i]));
    }

    okBtn.setOnAction(e -> {
      List<String> usersChosen = new ArrayList<>();
      for (CheckBox checkBox: userSel
      ) {
        if (checkBox.isSelected()) {
          usersChosen.add(checkBox.getText());
        }
      }
      clientThread.createGroup(usersChosen);
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel);
    box.getChildren().add(okBtn);
    stage.setScene(new Scene(box));
    stage.setOnCloseRequest(windowEvent -> {
      destroy();
    });
    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    // 标题为选定用户的用户名称
    stage.setTitle(currentRoom);
    stage.showAndWait();
  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML
  public void doSendMessage() {
    // TODO
    // 向当前聊天室发送消息，发送空白消息(此处包括全空格）是不允许的。发送消息后，清空消息输入框。
    String message = inputArea.getText();
//        System.out.println(message);
    if (message.trim().isEmpty()) {
      Alert alert = new Alert(AlertType.WARNING);
      alert.setTitle("Warning");
      alert.setHeaderText(null);
      alert.setContentText("Please enter a message.");
      alert.showAndWait();
      return;
    }

    // TODO: 将信息发送给服务器
//        sendMsg(message, "");
    clientThread.sendMessage(message);
    inputArea.clear();
  }

  //发送的消息体构造

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  //设置聊天记录的渲染，吧Message渲染为ListView的每一行
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }


  private class RoomCellFactory implements Callback<ListView<String>, ListCell<String>> {

    @Override
    public ListCell<String> call(ListView<String> param) {
      return new ListCell<String>() {

        @Override
        public void updateItem(String str, boolean empty) {
          super.updateItem(str, empty);
          if (empty || Objects.isNull(str)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();

          Label msgLabel = new Label(str);
//
//          nameLabel.setPrefSize(50, 20);
//          nameLabel.setWrapText(true);
//          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
//
//          if (username.equals(msg.getSentBy())) {
//            wrapper.setAlignment(Pos.TOP_RIGHT);
//            wrapper.getChildren().addAll(msgLabel, nameLabel);
//            msgLabel.setPadding(new Insets(0, 20, 0, 0));
//          } else {
          wrapper.setAlignment(Pos.TOP_LEFT);
          wrapper.getChildren().addAll(msgLabel);
          msgLabel.setPadding(new Insets(0, 0, 0, 20));
          msgLabel.setOnMouseClicked(event -> {
            //switch
            clientThread.switchGroup(msgLabel.getText());
          });
//            System.out.println(currentRoom);
//          if (str.equals(currentRoom)) {
//            msgLabel.setBackground(new Background(new BackgroundFill(Color.YELLOWGREEN, null, null)));
//          }
//          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }

  public void exit() {
    Platform.exit();
    System.exit(0);
  }

  public void setNum(String o) {
    System.out.println(o);
    Platform.runLater(() -> {
      onlineCntProperty.set(o);
    });
  }

  public void destroy() {
    try {
      clientThread.leave();
    } catch (IOException e) {
      System.out.println("Controller(253): clientThread.exit() error.");
    }
  }

}
