import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private static ServerSocket server;
    private static ArrayList<Socket> clients = new ArrayList<>();
    private static ArrayList<String> adminJsons = new ArrayList<>();
    private static ArrayList<String> studentJSONs = new ArrayList<>();
    private static int SERVER_PORT = 8886;

    public static void startServer() throws IOException {
        server = new ServerSocket(SERVER_PORT);
        System.out.println("Server started");
    }

    public static void listenForConnections() {
        while(true) {
            try {
                while (true) {
                    System.out.println("Listening for connections");
                    Socket newClient = server.accept();
                    System.out.println("Connection established with: " + newClient.getInetAddress());
                    checkRequest(newClient);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void listenForIncomingReports(DataInputStream inputStream) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        System.out.println("Listening for incoming reports from clients");
        new Thread(() -> {
            try {
                while (true) {
                    System.out.println("Listening for incoming reports");
                    String studentJSON = inputStream.readUTF();
                    System.out.println("Just received a student json report!! ");
                    System.out.println(studentJSON);
                    Student student = gson.fromJson(studentJSON, Student.class);
                    synchronized (studentJSONs) {
                        for (int i = studentJSONs.size() - 1; i >= 0; i--) {
                            Student existingStudent = gson.fromJson(studentJSONs.get(i), Student.class);
                            if (existingStudent.getFullName().equals(student.getFullName()) && existingStudent.getTeacherName().equals(student.getTeacherName())) {
                                studentJSONs.remove(i);
                                break;
                            }
                        }
                    }
                    studentJSONs.add(studentJSON);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void checkRequest(Socket client) throws IOException {
        DataInputStream inputStream = new DataInputStream(client.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
        String request = inputStream.readUTF();
        System.out.println("Request: " + request);
        if (request.equals("REGISTER")) {
            register(inputStream);
        } else if (request.equals("LOGIN")) {
            if (loginPassed(inputStream, outputStream)) {
                clients.add(client);
                System.out.println("added to clients");
                listenForIncomingReports(inputStream);
            }
        }
//        inputStream.close();
//        outputStream.close();
    }

    private static void register(DataInputStream inputStream) throws IOException {
        String userType = inputStream.readUTF();
        String json = inputStream.readUTF();
        if (userType.equals("ADMIN")) {
            adminJsons.add(json);
        } else {
            studentJSONs.add(json);
        }
    }

    private static boolean loginPassed(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        while(true) {
            String userType = inputStream.readUTF();
            String username = inputStream.readUTF();
            String password = inputStream.readUTF();
            System.out.printf("Received\nUser Type: %s\nUsername: %s\nPassword: %s", userType, username, password);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String accountJson = "";
            boolean accountFound = false;
            if (userType.equals("ADMIN")) {
                for (int i = 0; i < adminJsons.size(); i++) {
                    Admin admin = gson.fromJson(adminJsons.get(i), Admin.class);
                    if (username.equals(admin.getUsername()) && password.equals(admin.getPassword())) {
                        accountJson = adminJsons.get(i);
                        accountFound = true;
                    }
                }
            } else {
                for (int i = 0; i < studentJSONs.size(); i++) {
                    Student student = gson.fromJson(studentJSONs.get(i), Student.class);
                    if (username.equals(student.getUsername()) && password.equals(student.getPassword())) {
                        accountJson = studentJSONs.get(i);
                        accountFound = true;
                    }
                }
            }
            outputStream.writeBoolean(accountFound);
            System.out.println("Wrote boolean: " + accountFound);
            if (accountFound) {
                outputStream.writeUTF(accountJson);
                if (userType.equals("ADMIN")) {
                    sendTeacherReports(outputStream, accountJson);
                    System.out.println("Sent all student reports to admin");
                } else {
                    System.out.println("Student account found: " + accountJson);
                }
                break;
            }
            outputStream.flush();
            System.out.println("Flushed");
        }
        return true;
    }

    private static void sendTeacherReports(DataOutputStream outputStream, String teacherJSON) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Admin admin = gson.fromJson(teacherJSON, Admin.class);
        for (String studentJSON : studentJSONs) {
            Student student = gson.fromJson(studentJSON, Student.class);
            if (student.getTeacherName().equals(admin.getFullName())) {
                outputStream.writeUTF(studentJSON);
                System.out.println("Sending: " + studentJSON);
            }
        }
        outputStream.writeUTF("END");
        outputStream.flush();
    }

    public static void shutdown() {
        try {
            for (Socket socket : clients) {
                if (socket.isConnected()) {
                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                }
                socket.close();
            }
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
