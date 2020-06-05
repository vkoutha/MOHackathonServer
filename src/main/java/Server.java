import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {

    private static ServerSocket server;
    private static ArrayList<Socket> clients = new ArrayList<>();
    private static ArrayList<String> adminJsons = new ArrayList<>();
    private static ArrayList<String> studentJSONs = new ArrayList<>();
    private static HashMap<String, String> adminLogins = new HashMap<String, String>();
    private static HashMap<String, String> studentLogins = new HashMap<String, String>();
    private static int SERVER_PORT = 8888;

    public static void startServer() throws IOException {
        server = new ServerSocket(SERVER_PORT);
        System.out.println("Server started");
    }

    public static void listenForConnections() {
        new Thread(() -> {
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
        });
    }

    public static void listenForIncomingReports(DataInputStream inputStream){
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        new Thread(() -> {
            try {
                while (true) {
                    String studentJSON = inputStream.readUTF();
                    Student student = gson.fromJson(studentJSON, Student.class);
                    synchronized (studentJSONs) {
                        for (int i = studentJSONs.size() - 1; i >= 0; i--) {
                            Student existingStudent = gson.fromJson(studentJSONs.get(i), Student.class);
                            if (existingStudent.getFullName().equals(student.getFullName()) && existingStudent.getTeacherFullName().equals(student.getTeacherFullName())) {
                                studentJSONs.remove(i);
                                break;
                            }
                        }
                    }
                    studentJSONs.add(studentJSON);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    private static void checkRequest(Socket client) throws IOException {
        DataInputStream inputStream = new DataInputStream(client.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
        String request = inputStream.readUTF();
        if (request.equals("REGISTER")) {
            register(inputStream);
        } else if (request.equals("LOGIN")) {
            if (loginPassed(inputStream)) {
                clients.add(client);
                listenForIncomingReports(inputStream);
            }
            String json = inputStream.readUTF();
            sendTeacherReports(outputStream, json);
        }
        inputStream.close();
        outputStream.close();
    }

    private static void register(DataInputStream inputStream) throws IOException {
        String userType = inputStream.readUTF();
        String json = inputStream.readUTF();
        String username = inputStream.readUTF();
        String password = inputStream.readUTF();
        if (userType.equals("ADMIN")) {
            adminJsons.add(json);
            adminLogins.put(username, password);
        } else {
            studentJSONs.add(json);
            studentLogins.put(username, password);
        }
    }

    private static boolean loginPassed(DataInputStream inputStream) throws IOException {
        String userType = inputStream.readUTF();
        String username = inputStream.readUTF();
        String password = inputStream.readUTF();
        System.out.printf("Received\nUser Type: %s\nUsername: %s\nPassword: %s", userType, username, password);
        if (userType.equals("ADMIN")) {
            if (adminLogins.containsKey(username) && adminLogins.get(username).equals(password)) {
                return true;
            }
        } else {
            if (studentLogins.containsKey(username) && studentLogins.get(username).equals(password)) {
                return true;
            }
        }
        return false;
    }

    private static void sendTeacherReports(DataOutputStream outputStream, String teacherJSON) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        Admin admin = gson.fromJson(teacherJSON, Admin.class);
        for (String studentJSON : studentJSONs) {
            Student student = gson.fromJson(studentJSON, Student.class);
            if (student.getTeacherFullName().equals(admin.getFullName())) {
                outputStream.writeUTF(studentJSON);
            }
        }
        outputStream.writeUTF("END");
        outputStream.flush();
    }

}
