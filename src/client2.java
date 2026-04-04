//import RTP
import rtp.rtp_packet;
import rtp.RtpSender;
import rtp.RtpReceiver;

//Import RTCP
import rtp.RtcpPacket;

//Import SIP
import sip.SipMessage;
import sip.SdpBuilder;

//java net utilities
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetAddress;

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

        String localSdp = SdpBuilder.build(localIP, localPort);

        SipMessage ok_200 = new SipMessage();
        ok_200.startLine = "SIP/2.0 200 OK";
        ok_200.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5060");
        ok_200.headers.put("From", "<sip:client2@" + localIP + ">");
        ok_200.headers.put("To", "<sip:client1@" + senderIP + ">");
        ok_200.headers.put("CSeq", "1 INVITE");
        ok_200.headers.put("Contact", "<sip:clien2@" + localIP + ">");
        ok_200.headers.put("Content-Type", "application/sdp");
        ok_200.body = localSdp;


        sendSIP(ok_200.rawSIP(), senderIP, 5060);
        this.senderIP = senderIP;
        this.senderRtpPort = rtp_port;


    }

    //Process ACK
    public void processACK() throws Exception{



    }

    //Process Bye
    public void processBye(String ip) throws Exception {
        //stop rtp


        String localIP = InetAddress.getLocalHost().getHostAddress();

        //send ok for bye
        SipMessage ok200 = new SipMessage();
        ok200.startLine = "SIP/2.0 200 OK";
        ok200.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5060");
        ok200.headers.put("From", "<sip:client2@" + localIP + ">");
        ok200.headers.put("To", "<sip:client1@" + senderIP + ">");
        ok200.headers.put("CSeq", "2 BYE");
        
        sendSIP(ok200.rawSIP(), senderIP, 5060);
        System.out.println("SIP MESSAGE: BYE (sent)");
    }


    //class variables
    private String senderIP;
    private int senderRtpPort;
    private static volatile boolean stop = false;


    public static void main(String arg[]) throws Exception{

        client2 crcv = new client2();
        DatagramSocket socket = new DatagramSocket(5060);
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
                        break;
                    case "ACK":
                        //rtp processing
                    case "BYE":
                        crcv.processBye(client1_IP);
                        running = false;
                        break;
                }
            }
        }

        socket.close();
        System.out.println("RTP closed");

    }
}