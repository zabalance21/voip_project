//import RTP
import rtp.RtpReceiver;


//Import SIP
import sip.SipMessage;
import sip.SdpBuilder;

//java net utilities
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

//java utility for user input
import java.util.Scanner;

public class client2 {

    /////////////////////////////
    /// 
    /// SIP METHODS RECEIVER SIDE
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


    //Process Invite from sender
    public void processInvite(String rcvMsg, String senderIP) throws Exception {

        SipMessage msg = SipMessage.parse(rcvMsg);

        String sdp = msg.body;
        int rtp_port = SdpBuilder.extractRtpPort(sdp);

        int localPort = 5006;
        String localIP = InetAddress.getLocalHost().getHostAddress();

        SipMessage response = new SipMessage();

        //case when timeout mode is selected
        if(MODE.equals("TIMEOUT")) {
            String localSdp = SdpBuilder.build(localIP, localPort);

            response.startLine = "SIP/2.0 200 OK";
            response.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5061");
            response.headers.put("From", "<sip:client2@" + localIP + ">");
            response.headers.put("To", "<sip:client1@" + senderIP + ">");
            response.headers.put("Call-ID", "testRTP67@" + localIP);
            response.headers.put("CSeq", "1 INVITE");
            response.headers.put("Contact", "<sip:client2@" + localIP + ">");
            response.headers.put("Content-Type", "application/sdp");
            response.headers.put("Content-Length", String.valueOf(localSdp.length()));
            response.body = localSdp;

            this.senderIP = senderIP;
            this.senderRtpPort = rtp_port;
        }

        //building 200 OK response if mode is OK
        if(MODE.equals("OK")) {
            String localSdp = SdpBuilder.build(localIP, localPort);

            response.startLine = "SIP/2.0 200 OK";
            response.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5061");
            response.headers.put("From", "<sip:client2@" + localIP + ">");
            response.headers.put("To", "<sip:client1@" + senderIP + ">");
            response.headers.put("Call-ID", "testRTP67@" + localIP);
            response.headers.put("CSeq", "1 INVITE");
            response.headers.put("Contact", "<sip:client2@" + localIP + ">");
            response.headers.put("Content-Type", "application/sdp");
            response.headers.put("Content-Length", String.valueOf(localSdp.length()));
            response.body = localSdp;

            this.senderIP = senderIP;
            this.senderRtpPort = rtp_port;
        }

        //sample sip message with error of 4yy
        if(MODE.equals("4YY")) {
            response.startLine = "SIP/2.0 404 Not Found";
            response.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5061");
            response.headers.put("From", "<sip:client2@" + localIP + ">");
            response.headers.put("To", "<sip:client1@" + senderIP + ">");
            response.headers.put("Call-ID", "testRTP67@" + localIP);
            response.headers.put("CSeq", "1 INVITE");
            response.headers.put("Content-Length", "0");
            response.body = "";
        }

        //sample sip message with error of 5xx
        if(MODE.equals("5XX")) {    
            response.startLine = "SIP/2.0 500 Internal Server Error";
            response.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5061");
            response.headers.put("From", "<sip:client2@" + localIP + ">");
            response.headers.put("To", "<sip:client1@" + senderIP + ">");
            response.headers.put("Call-ID", "testRTP67@" + localIP);
            response.headers.put("CSeq", "1 INVITE");
            response.headers.put("Content-Length", "0");
            response.body = "";
        }

        //send SIP response
        sendSIP(response.rawSIP(), senderIP, 5060);
    }

    //Process ACK
    public void processACK() throws Exception{
        System.out.println("ACK received will now start rtp session");

        rtp_recv = new RtpReceiver();

        new Thread(() -> {

            try {
                rtp_recv.receive(5006, 5007, senderIP, senderRtpPort + 1);
            } catch(Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    //Process Bye
    public void processBye(String ip) throws Exception {
        
        //stop rtp
        if (rtp_recv != null) {
            rtp_recv.stop();
        }


        String localIP = InetAddress.getLocalHost().getHostAddress();

        //send ok for bye
        SipMessage ok200 = new SipMessage();
        ok200.startLine = "SIP/2.0 200 OK";
        ok200.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5061");
        ok200.headers.put("From", "<sip:client2@" + localIP + ">");
        ok200.headers.put("To", "<sip:client1@" + senderIP + ">");
        ok200.headers.put("Call-ID", "testRTP67@" + localIP);
        ok200.headers.put("CSeq", "2 BYE");
        ok200.headers.put("Content-Length", "0");
        ok200.body = "";
        
        sendSIP(ok200.rawSIP(), ip, 5060);
        System.out.println("SIP MESSAGE: BYE (sent)");
    }


    //class variables
    private String senderIP;
    private int senderRtpPort;
    private RtpReceiver rtp_recv;
    private static String MODE = "OK";

    public static void main(String arg[]) throws Exception{

        Scanner sc = new Scanner(System.in);

        System.out.println("Select mode: OK, TIMEOUT, 4YY, 5XX");
        System.out.println("1: OK");
        System.out.println("2: TIMEOUT");
        System.out.println("3: 4YY");
        System.out.println("4: 5XX");

        int choice = sc.nextInt();
        switch(choice) {
            case 1:
                MODE = "OK";
                break;
            case 2:
                MODE = "TIMEOUT";
                break;
            case 3:
                MODE = "4YY";
                break;
            case 4:
                MODE = "5XX";
                break;
        }

        client2 crcv = new client2();
        DatagramSocket socket = new DatagramSocket(5061);
        byte[] buffer = new byte[2048];

        System.out.println("Waiting for SIP messages.....");

        boolean running = true;
        while(running){
            
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());
            String client1_IP = packet.getAddress().getHostAddress();
            SipMessage sip = SipMessage.parse(msg);

            if(sip.isRequest()) {
                
                switch(sip.getMethod()) {
                    case "INVITE":
                        crcv.processInvite(msg, client1_IP);

                        //ending when there is an error like 4yy or 5xx
                        if(MODE.equals("4YY") || MODE.equals("5XX")) {
                            System.out.println("Ending session due encountered error");
                            running = false;
                        }
                        break;
                    case "ACK":
                        //rtp processing
                        crcv.processACK();
                        break;
                    case "BYE":
                        crcv.processBye(client1_IP);
                        running = false;
                        break;
                }
            }
        }

        socket.close();
        System.out.println("RTP closed");
        sc.close();
    }
}