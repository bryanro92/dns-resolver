/****************************


*********************** */
import java.io.*;
import java.net.*;
import java.util.Scanner;
public class server{
    public static void main(String args[]) throws Exception{
        DatagramSocket serverSocket = null;
        DatagramPacket receivePacket = null;
        DatagramPacket sendPacket = null;
        DatagramPacket send2ClientPacket = null;

        System.out.print("Enter a socket: ");
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        int selectedSock = Integer.parseInt(inFromUser.readLine());
        serverSocket = new DatagramSocket(selectedSock);
        System.out.println();
        System.out.println("Success. Listening on port: " + selectedSock);

        while(true){

            byte[] receiveData = new byte[1024];
            receivePacket = new DatagramPacket(receiveData,receiveData.length);
            serverSocket.receive(receivePacket);
            String message = new String(receiveData);
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            System.out.println(message);
            message = message.trim();
            if (message.equals("/exit")){
                System.out.println("Client requested exit!");
                message = "good bye!";
                byte[] sendData = message.getBytes();
                sendPacket =
                        new DatagramPacket(sendData,sendData.length,IPAddress,port);
                serverSocket.send(sendPacket);
            }
            else {
                System.out.println("Got message: "+message + " from client "
                        + receivePacket.getAddress());
                byte[] sendData = message.getBytes();
                sendPacket =
                        new DatagramPacket(sendData,sendData.length,IPAddress,port);
                serverSocket.send(sendPacket);
            }

        }
    }



}
