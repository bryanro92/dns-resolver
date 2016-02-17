/**************************
 * Ross Bryan and Nate Lentz
 * Project 2
 * DNS Resolver
 ************************/
import java.io.*;
import java.net.*;

public class client {

    public static void main(String args[]) throws Exception{
        DatagramSocket clientSocket = new DatagramSocket();
        BufferedReader inFromUser =
                new BufferedReader(new InputStreamReader(System.in));
        String ip = "";
        int port = -1;
        System.out.println("Enter an IP adress: ");
        ip = inFromUser.readLine();
        System.out.println("Enter in a socket: ");
        port = Integer.parseInt(inFromUser.readLine());
        while (true){
            System.out.print("Enter a website: ");
            String message = inFromUser.readLine();
            System.out.println();
            byte[] sendData = new byte[1024];
            sendData=message.getBytes();
            InetAddress IPAddress = InetAddress.getByName(ip);

            DatagramPacket sendPacket =
                    new DatagramPacket(sendData,sendData.length,IPAddress,port);
            clientSocket.send(sendPacket);

            System.out.println("Message sent to server!");

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket =
                    new DatagramPacket(receiveData,receiveData.length);
            clientSocket.receive(receivePacket);
            String serverMessage = new String(receivePacket.getData());
            System.out.println("Got from server: "+serverMessage);

            if (serverMessage.trim().equals("good bye!")){
                System.out.println("you have requested exit!");
                System.exit(0);
            }

        }
    }








































}