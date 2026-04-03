//import RTP
import rtp.rtp_packet;
import rtp.RtpSender;
import rtp.RtpReceiver;

//Import RTCP
import rtp.RtcpPacket;

//Import SIP
import sip.SipMessage;
import sip.SdpBuilder;

import java.net.InetAddress;

//Import AudioHandling
import audio.AudioHandling;

//other things that are needed
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class VoIP_client {

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
        invite.startLine = "INVITE sip:user@" + ip + "SIP/2.0";
        invite.headers.put("Via", "SIP/2.0/UDP " + localIP + ":5060");
        invite.headers.put("From", "<sip:client@" + localIP + ">");
        invite.headers.put("To", "<sip:user@" + ip + ">");
        invite.headers.put("Call-ID", "12345");
        invite.headers.put("CSeq", "1 INVITE");
        invite.headers.put("Contact", "<sip:client@" + localIP + ">");
        invite.headers.put("Content-Type", "application/sdp");
        invite.body = sdp;

        sendSIP(invite.rawSIP(), ip, port);

    }

    //Process 200 OK and send ACK
    public void processOK(String msg, String senderIP)throws Exception{

        SipMessage sipMSG = SipMessage.parse(msg);

        //Sending of ACK
        SipMessage ack = new SipMessage();
        ack.startLine = "ACK sip:user@" + senderIP + "SIP/2.0";
        ack.body = "";

        sendSIP(ack.rawSIP(), senderIP, 5060);
    }




    public static void main(String arg[]){

    }
}