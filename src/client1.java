//import RTP
import rtp.RtpSender;

//Import SIP
import sip.SipMessage;
import sip.SdpBuilder;

//java net utilities
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

//java file utilities
import java.io.File;

//use input java util
import java.util.Scanner;

public class client1 {

    /////////////////////////////
    /// 
    /// SIP METHODS SENDER SIDE
    /// 
    ////////////////////////////
    

    //sending SIP messages
    public void sendSIP(String msg, String ip, int port) throws Exception{
        DatagramSocket socket =  new DatagramSocket();
        byte[] data = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);

        socket.send(packet);
        socket.close();
    }


    //Send Invite method
    public void sendInvite(String ip, int port) throws Exception {

        String localIP = InetAddress.getLocalHost().getHostAddress();
        int localRTPport = 5004;

        String sdp = SdpBuilder.build(localIP, localRTPport);

        SipMessage invite = new SipMessage();
        invite.startLine = "INVITE sip:user@" + ip + " SIP/2.0";
        invite.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5060");
        invite.headers.put("From", "<sip:client1@" + localIP + ">");
        invite.headers.put("To", "<sip:client2@" + ip + ">");
        invite.headers.put("Call-ID", "testRTP67@" + localIP);
        invite.headers.put("CSeq", "1 INVITE");
        invite.headers.put("Contact", "<sip:client@" + localIP + ">");
        invite.headers.put("Content-Type", "application/sdp");
        invite.headers.put("Content-Length", String.valueOf(sdp.length()));
        invite.body = sdp;

        sendSIP(invite.rawSIP(), ip, port);

    }

    //Process 200 OK and send ACK
    public void processOK(String msg, String senderIP)throws Exception{

        SipMessage sipMSG = SipMessage.parse(msg);

        //Sending of ACK
        SipMessage ack = new SipMessage();
        ack.startLine = "ACK sip:client1@" + senderIP + " SIP/2.0";
        ack.headers.put("Via", sipMSG.headers.get("Via"));
        ack.headers.put("From", sipMSG.headers.get("From"));
        ack.headers.put("To", sipMSG.headers.get("To"));
        ack.headers.put("Call-ID", sipMSG.headers.get("Call-ID"));
        ack.headers.put("CSeq", "1 ACK");
        ack.headers.put("Content-Length", "0");
        ack.body = "";

        sendSIP(ack.rawSIP(), senderIP, 5061);
    }

    //Send bye method
    public void sendBye() throws Exception{

        String localIP = InetAddress.getLocalHost().getHostAddress();

        SipMessage bye = new SipMessage();
        bye.startLine = "BYE sip:client2@" + receiverIP + " SIP/2.0";
        bye.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5060");
        bye.headers.put("From", "<sip:client1@" + localIP + ">");
        bye.headers.put("To", "<sip:client2@" + receiverIP + ">");
        bye.headers.put("Call-ID", "testRTP67@" + localIP);
        bye.headers.put("CSeq", "2 BYE");
        bye.headers.put("Content-Length", "0");
        bye.body = "";
        sendSIP(bye.rawSIP(), receiverIP, 5061);
        System.out.println("Sent BYE to receiver.");
    }

    //file method to list out the available wav files
    public File[] getWavFiles(String path) {
        
        try {
            File folder = new File(path);
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));

            if(files == null || files.length == 0) {
                System.out.println("No audio files in the folder");
                return new File[0];
            }

            return files;
        } catch (Exception e) {
            System.out.println("Error reading the folder for audio files: "  + e.getMessage());
            return new File[0];
        }

    }

    public String chooseFile(String path) {

        int i;

        try {
            File[] files = getWavFiles(path);

            if(files.length == 0){
                return null;
            }

            System.out.println("List of Available Wav files");
            
            for(i = 0; i < files.length; i++){
                System.out.println((i + 1) + ":" + files[i].getName());    
            }

            try (Scanner sc = new Scanner(System.in)) {
                System.out.print("Select a file (1-" + files.length + "): ");
                int choice = sc.nextInt();

                return files[choice - 1].getPath();
            }

        } catch(Exception e) {
            System.out.println("Error in choosing the file: "  + e.getMessage());
            return null;
        }

    }


    private static String receiverIP = "127.0.0.1"; // localhost
    


    public static void main(String arg[]) throws Exception{

        client1 csdr = new client1();

        //choose wav file
        String audiopath = csdr.chooseFile("../wav_files/");

        //handling null error for the audopath
        if(audiopath == null) {
            System.out.println("No valid file chosen");
            System.out.println("Exiting program");
            return;
        }


        System.out.println("Selected: " + audiopath);

        //Send INVITE
        csdr.sendInvite(receiverIP, 5061);



        //Listen for SIP responses
        DatagramSocket sipSocket = new DatagramSocket(5060);
        byte[] buffer = new byte[2048];
        boolean running = true;
        boolean connected = false;

        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            sipSocket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());
            String senderIP = packet.getAddress().getHostAddress();

            SipMessage sip = SipMessage.parse(msg);

            String statusLine = sip.startLine;
            System.out.println("Received: " + statusLine);

            // Process 200 OK response
            if (sip.isResponse() && statusLine.contains("200 OK")) {
                
                
                if (!connected) {
                   
                    connected = true;

                    csdr.processOK(msg, senderIP);

                    int destRtpPort = SdpBuilder.extractRtpPort(sip.body);
                    System.out.println("Remote RTP port: " + destRtpPort);

                    // Start RTP
                    new Thread(() -> {
                        try {
                            System.out.println("Sending: " + audiopath);
                            RtpSender.send(receiverIP, destRtpPort, destRtpPort + 1, audiopath);
                            System.out.println("Finished sending");

                            //calline the bye method when rtp finishes to end the VoIP
                            try {
                                csdr.sendBye();
                            } catch(Exception e) {
                                e.printStackTrace();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } 
                else {
                    System.out.println("VoIP Finished");
                    running = false;
                }


            }
            //handle error 4yy and 5xx responses
            else if(statusLine.startsWith("SIP/2.0 4") || statusLine.startsWith("SIP/2.0 5")) {
                System.out.println("Error Connecting" + statusLine);
                System.out.println("Exiting program");
                running = false;    
            }
        }

        sipSocket.close();
        System.out.println("SIP client stopped.");
    }
}