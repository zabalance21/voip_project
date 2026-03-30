package sip;

// SDP body (minimal)
public class SdpBuilder {
    public static String build(String localIp, int rtpPort){
        return "v=0\r\n" + "o=- 0 0 IN IP4 " + localIp + "\r\n" + "s=VoIP Session\r\n" + "t=0 0\r\n" +
                "m=audio " + rtpPort + " RTP/AVP 0 \r\n" + "a=rtpmap:0 PCMU/8000\r\n";
    }

    public static int extractRtpPort(String sdp) {
        for (String line : sdp.split("\r\n")) {
            if (line.startsWith("m=audio")) {
                return Integer.parseInt(line.split(" ")[1]);
            }
        }
        return -1;
    }
}
