import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client1 {
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out = null;

    public static void setAuthorized(boolean authorized) {
        Client1.authorized = authorized;
    }


    static boolean authorized;

    public static void main(String[] args) throws IOException {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("/auth login1 pass1");
            setAuthorized(false);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            if (in.available() > 0) {
                                String strFromServer = in.readUTF();
                                if (strFromServer.startsWith("/authOk")) {
                                    setAuthorized(true);
                                    System.out.println("Authorized on server");
                                    Client1.runOutputThread(out);
                                    break;
                                }
                                System.out.println(strFromServer + "\n");
                            }
                        }
                        while (true) {
                            if (in.available() > 0) {
                                String strFromServer = in.readUTF();
                                if (strFromServer.equalsIgnoreCase("/end")) {
                                    in.close();
                                    out.close();
                                    socket.close();
                                    System.exit(0);
                                    break;
                                }
                                System.out.println(strFromServer);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            t.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
        in.close();
        out.close();
        socket.close();
        System.exit(0);
        }
    }

    private static Thread runOutputThread(DataOutputStream out) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String message = scanner.nextLine();
                    try {
                        out.writeUTF(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (message.equals("/end")) {
                        break;
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    public static void closeConnection() {
        System.out.println("Работа клиента завершена");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
