import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Server.startServer();
        Server.listenForConnections();
        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            Server.shutdown();
        }));
    }

}
