package cn.edu.sustech.cs209.chatting.common;

public class Message {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    public Message( String sentBy,  String data) {

        this.sentBy = sentBy;

        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }
}
