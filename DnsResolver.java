import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;

/********************************************************************************
 * Ross Bryan Nate Lentz
 * CIS 457
 * Project 2
 * Recursive DNS caching resolver
 *
 ********************************************************************************/
class DnsResolver {
    static final int DEFAULT_PORT = 8025;
    static ArrayList<String> results;
    static ArrayList<String> answers;
    static HashMap<String,Tuple> cache;

    /*********************************************************
     * @returns if valid port or not
     **********************************************************/
    public static boolean validInput(String str) {
        try {
            int i = Integer.parseInt(str);
        }
        catch(NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**************************************************************************
     * Main method that prompts user input and opens a DatagramSocket
     **************************************************************************/
    public static void main(String[] args) throws IOException {

        //Read command line argument
        String userInput;
        int socketNum;
        DatagramSocket serverSocket = null;
        System.out.print("Enter a port: ");
        Scanner sc = new Scanner(System.in);
        userInput = sc.next();

        //Check for valid argument
        if(validInput(userInput))
            socketNum = Integer.parseInt(userInput);
        else {
            System.out.printf("The port you specified was not an integer. A default "+
                    "value of %d has been substituted.\n", DEFAULT_PORT);
            socketNum = DEFAULT_PORT;
        }

        //hashmap for cache
        cache = new HashMap<String, Tuple>();
        Tuple rootval = new Tuple("198.41.0.4", 999999);
        cache.put("root", rootval);

        // Create a socket that listens on port designated by user.
        try {
            serverSocket = new DatagramSocket(socketNum);

        }
        catch (IOException e) {
            System.out.printf("The port you specified cannot be used.\n");
            System.exit(1);
        }

        System.out.printf("DNS resolver started on port %d\n", socketNum);
//////////////////////////////////////////////////////////////////////////////////////////////
        while(true) {
            // Set server to listen for a DatagramPacket from client
            byte[] receiveData = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(packet);

            // Print packet info from client using helper class PacketInfo()
            PacketInfo information = new PacketInfo(packet);
            information.getValues();
            information.getQuestions();
            information.getAnswers();
            information.getAuthority();

            System.out.printf("\nReceived query from client for %s\n", information.getNameRequested());

            // Check for question type A and class type IN before proceeding
            if(!information.getValidQuestion()) {
                information.setErrorCode();
                System.out.println("\nSending ERROR to client\n");

                //send error code back to client
                InetAddress retAddress = InetAddress.getByName("127.0.0.1");
                int port = packet.getPort();
                DatagramPacket toClient = new DatagramPacket(information.getByteArray(),
                        information.getByteArray().length, retAddress, port);
                serverSocket.send(toClient);
            }

            //If the requested URL is not in cache
            if(!cache.containsKey(information.getNameRequested()) ||
                    cache.get(information.getNameRequested()).checkTTL()) {

                if(cache.containsKey(information.getNameRequested()))
                    if(cache.get(information.getNameRequested()).checkTTL())
                        System.out.println("cache timed out");

                //Send DatagramPacket down server tree with recursion desired bit unset
                information.unsetRecursion();
                String nextServer = cache.get("root").getIP(); //Initial server to check is 198.41.0.4
                boolean doneSearching = false;
                boolean error = false;
                InetAddress address;
                DatagramPacket response;
                PacketInfo responseInfo = null;

                //Loop until an answer is found or there is an error
                while(!doneSearching && !error) {
                    address = InetAddress.getByName(nextServer);
                    DatagramPacket sendPacket = new DatagramPacket(information.getByteArray(),
                            information.getByteArray().length, address, 53);
                    serverSocket.send(sendPacket);

                    byte[] fromServer = new byte[1024];
                    response = new DatagramPacket(fromServer, fromServer.length);
                    try{
                        serverSocket.receive(response);
                    } catch(SocketTimeoutException e) {
                        System.out.println("Server Timed out");
                        System.exit(-1);
                    }
                    responseInfo = new PacketInfo(response);


                    System.out.printf("\nQuerying server %s\n", nextServer);
                    //read information in received packet
                    responseInfo.getValues();
                    responseInfo.getQuestions();
                    responseInfo.getAnswers();
                    responseInfo.getAuthority();
                    responseInfo.getAdditional();

                    //This code is useful for viewing more complete packing info in terminal
                    //System.out.printf("%s\n", responseInfo.getValues());
                    //System.out.printf("%s\n", responseInfo.getQuestions());
                    //System.out.printf("%s\n", responseInfo.getAnswers());
                    //System.out.printf("%s\n", responseInfo.getAuthority());
                    //System.out.printf("%s\n----------------------\n", responseInfo.getAdditional());
                    //System.out.println(responseInfo.isAnswer());
                    //System.out.println(responseInfo.nextIP());

                    ArrayList<String> names = responseInfo.getNames();
                    ArrayList<String> ips = responseInfo.getResults();
                    ArrayList<Long> ttls = responseInfo.getTTL();

                  //  System.out.println(names.size()+" == "+ips.size());
                    if(names.size() == ips.size()) {
                        for(int i = 0; i < names.size(); i++)
                            //cache.put(names.get(i), new Tuple(ips.get(i), System.currentTimeMillis() + ttls.get(i)));
                            cache.put(names.get(i), new Tuple(ips.get(i), System.currentTimeMillis()*1000 + ttls.get(i)));

                    }

                    for(int i = 0; i < names.size(); i++)
                        System.out.println(names.get(i));

                    System.out.printf("Received answer: %b\n", responseInfo.isAnswer());
                    error = responseInfo.isError();
                    doneSearching = responseInfo.isAnswer();
                    nextServer = responseInfo.nextIP();
                    results = responseInfo.getResults();

                    if(!doneSearching) {
                        System.out.println("Authority records found:");
                        for(int i = 0; i < results.size(); i++) {
                            System.out.println(results.get(i));
                        }
                    }
                    if(nextServer.equals("")) {
                        error = true;
                        responseInfo.setErrorCode();
                    }
                }
                //Send answer back to client
                answers = responseInfo.getAnswerRecords();
                System.out.println("Answers found:");
                for(int i = 0; i < answers.size(); i++) {
                    System.out.println(answers.get(i));
                }
                System.out.println("\nSending answer to client\n");

                address = InetAddress.getByName("127.0.0.1");
                int port = packet.getPort();
                DatagramPacket toClient = new DatagramPacket(responseInfo.getByteArray(),
                        responseInfo.getByteArray().length, address, port);
                serverSocket.send(toClient);

                //if the URL is in the cache
            } else {

                Tuple results = cache.get(information.getNameRequested());
                System.out.println("Answer found in cache: \n" + results.getIP());

                InetAddress ip = InetAddress.getByName(results.getIP());
                information.setAnswer(information.getNameRequested(), ip);

                System.out.println("\nSending answer to client\n");

                InetAddress address = InetAddress.getByName("127.0.0.1");
                int port = packet.getPort();
                DatagramPacket toClient = new DatagramPacket(information.getByteArray(),
                        information.getByteArray().length, address, port);

                serverSocket.send(toClient);
            }
        }
    }
}
