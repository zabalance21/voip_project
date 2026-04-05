package sip;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SipClient {
    private DatagramSocket socket;
    private String localIP;
    private int localSipPort;

    public SipClient(String localIP, int localSipPort) throws Exception {
        this.localIP = localIP;
        this.localSipPort = localSipPort;
        this.socket = new DatagramSocket(localSipPort);
        this.socket.setSoTimeout(30000);
        System.out.println("[SIP] Listening on " + localIP + ":" + localSipPort);
    }

    // SEND 
    public void send(SipMessage msg, String destIp, int destPort) throws Exception {
        byte[] data = msg.serialize().getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName(destIp), destPort);
        socket.send(dp);
        System.out.println("[SIP TX] " + msg.startLine);
    }

    // RECEIVE
    public SipMessage receive() throws Exception {
        byte[] buf = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        socket.receive(dp);
        String raw  = new String(dp.getData(), 0 ,  dp.getLength());
        SipMessage msg = SipMessage.parse(raw);
        System.out.println("[SIP RX] " + msg.startLine);
        return msg;
    }

    public void close(){
        socket.close();
        System.out.println("[SIP] Socket closed");
    }

    // BUILD INVITE
    public SipMessage buildInvite(String fromIp, int fromSIpPort, String toIp, int toIpPort, int rtpPort,
                                  String callId){
        SipMessage m = new SipMessage();
        m.startLine = "INVITE sip:receiver@" + toIp + " SIP/2.0";
        m.headers.put("Via", "SIP/2.0/UDP " + fromIp + ":" + fromSIpPort + ";branch=z9hkG4bK" + generateBranch());
        m.headers.put("From", "<sip:caller@" + fromIp + ">;tag=" + generateTag());
        m.headers.put("To", "<sip:receiver@" + toIp + ">");
        m.headers.put("Call-ID", callId);
        m.headers.put("CSeq","1 INVITE");
        m.headers.put("Contact", "<sip:caller@" + fromIp + ":" + fromSIpPort + ">");
        m.headers.put("Content-Type", "application/udp");
        m.headers.put("Max-Forwards", "70"); // Required
        m.body = SdpBuilder.build(fromIp, rtpPort);
        return m;
    }

    // BUILD 200 OK
    public SipMessage build200OK(SipMessage invite, String localIP, int localSipPort, int rtpPort){
        SipMessage m = new SipMessage();
        m.startLine = "SIP/2.0 200 OK";
        m.headers.put("Via", invite.headers.get("Via"));
        m.headers.put("From", invite.headers.get("From"));
        m.headers.put("To", invite.headers.get("To") + ";tag=" + generateTag());
        m.headers.put("Call-ID", invite.headers.get("Call-ID"));
        m.headers.put("CSeq", invite.headers.get("CSeq"));
        m.headers.put("Contact", "<sip:receiver@" + localIP + ":" + localSipPort + ">");
        m.headers.put("Content-Type", "application/udp");
        m.body = SdpBuilder.build(localIP, rtpPort);
        return m;
    }

    // BUILD ACK
    public SipMessage buildAck(SipMessage invite, SipMessage okResponse, String toIp){
        SipMessage m = new SipMessage();
        m.startLine = "ACK sip:receiver@" + toIp + " SIP/2.0";
        m.headers.put("Via", invite.headers.get("Via"));
        m.headers.put("From", invite.headers.get("From"));
        m.headers.put("To", okResponse.headers.get("To"));
        m.headers.put("Call-ID", invite.headers.get("Call-ID"));
        m.headers.put("CSeq", "1 ACK");
        m.headers.put("Max-Forwards", "70");
        return m;
    }

    // BU"ILD 200 OK FOR BYE
    public SipMessage build200OkBye(SipMessage bye){
        SipMessage m = new SipMessage();
        m.startLine = "SIP/2.0 200 OK";
        m.headers.put("Via", bye.headers.get("Via"));
        m.headers.put("From", bye.headers.get("From"));
        m.headers.put("To", bye.headers.get("To"));
        m.headers.put("Call-ID", bye.headers.get("Call-ID"));
        m.headers.put("CSeq", bye.headers.get("CSeq"));
        return m;
    }

    // HELPERS
    private String generateBranch() {
        // branch must start with z9hG4bK
        return Integer.toHexString((int)(Math.random() * 0xFFFFFF));
    }

    private String generateTag() {
        return Integer.toHexString((int)(Math.random() * 0xFFFFFF));
    }
}
