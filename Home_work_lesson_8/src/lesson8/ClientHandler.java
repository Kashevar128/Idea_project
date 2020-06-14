package lesson8;

import jdk.internal.cmm.SystemResourcePressureImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    private boolean flag;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        this.socket = socket;
        this.name = "";
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                flag = false;
                try {
                    authenticate();
                    readMessages();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException ex) {
            throw new RuntimeException("Client creation error");
        }
    }

    private void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        myServer.unsubscribe(this);
        myServer.broadcast("User " + name + "left");
    }

    private void readMessages() throws IOException {
        while (true) {
            if (in.available() > 0) {
                String message = in.readUTF();
                System.out.println("From " + name + ":" + message);
                if (message.equals("/end")) {
                    return;
                }
                if (message.startsWith("/w ")) {
                    String[] parts = message.split("\\s");
                    myServer.sendDirect(parts[1], name + ": " + parts[2]);
                } else myServer.broadcast(name + ": " + message);
            }
        }
    }

    private void authenticate() throws IOException {
        new Thread(() -> {
            long a = System.currentTimeMillis();
            while(true) {
                System.currentTimeMillis();
                long b = System.currentTimeMillis() - a;
                System.out.println("Отсчет до конца авторизации клиента: " + b);
                if(b > 120000 && flag == false) {
                    closeConnection();
                    System.out.println("Authorization time has expired and the user is disabled");
                    break;
                }
                if (flag == true) {
                    break;
                }
            }
        }).start();
        while (true) {
            if (in.available() > 0) {
                flag = true;
                String str = in.readUTF();
                if (str.startsWith("/auth")) {
                    String[] parts = str.split("\\s");
                    String nick = myServer.getAuthService().getNickByLoginAndPwd(parts[1], parts[2]);
                    if (nick != null) {
                        if (!myServer.isNickLogged(nick)) {
                            System.out.println(nick + " logged into chat");
                            name = nick;
                            sendMsg("/authOk " + nick);
                            myServer.broadcast(nick + " is in chat");
                            myServer.subscribe(this);
                            return;
                        } else {
                            System.out.println("User " + nick + " tried to re-enter");
                            sendMsg("User already logged in");
                        }
                    } else {
                        System.out.println("Wrong login/password");
                        sendMsg("Incorrect login attempted");
                    }
                }
            }

        }
    }

    public void sendMsg(String s) {
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
