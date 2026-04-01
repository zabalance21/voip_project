package rtp;

import audio.AudioHandling;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RtpSender {

    private static int FRAME_SIZE = 160;
    private static int INTERVAL_MILISECS = 20;

    public static void send(String destIp, int destRtpPort, int destRtcpPort, String wavFile) throws Exception {
        DatagramSocket rtpSocket = new DatagramSocket();
        DatagramSocket rtcpSocket = new  DatagramSocket();
        InetAddress dest = InetAddress.getByName(destIp);

        // Read .wav and convert to PCMU
        byte[] pcmu = AudioHandling.readWavAsPCMU(wavFile);
        System.out.println("[RTP] Total PCMU bytes to send: "+ pcmu.length);
        System.out.println("[RTP] Sending to " + destIp + ":" + destRtpPort);

        int seq = 0;
        long timestamp = 0;
        long packetCount = 0;
        long octetCount = 0;

        // Send loop
        for(int offset = 0; offset < pcmu.length; offset += FRAME_SIZE) {

            // Slice one 20 ms frame
            int frameLen = Math.min(FRAME_SIZE, pcmu.length - offset);
            byte[] frame = new byte[frameLen];
            System.arraycopy(pcmu, offset, frame, 0, frameLen);

            // Wrap fram in rtp packet
            rtp_packet pkt = new rtp_packet(seq, timestamp, frame, frameLen);

            // Serialize and send over UDP
            byte[] raw = new byte[pkt.getlength()];
            pkt.getpacket(raw);
            rtpSocket.send(new DatagramPacket(raw, raw.length, dest, destRtpPort));
            packetCount++;
            octetCount += frameLen;
            seq++;
            timestamp += FRAME_SIZE;
            System.out.println("[RTP] Sent packet seq=" + (seq - 1) +
                    " ts="+ timestamp +
                    " size=" + frameLen);

            // SEND RTCP SENDER REPORT every 50 
            if(packetCount % 50 == 0){
                byte[] sr = RtcpPacket.buildSR(
                        0L,
                        packetCount,
                        octetCount
                );
                rtcpSocket.send(new DatagramPacket(sr, sr.length, dest, destRtpPort));
                System.out.println("[RTCP] Sent SR — packets=" + packetCount + " bytes=" + octetCount);
            }
            Thread.sleep(INTERVAL_MILISECS); // sending at 20ms intervals
        }

        // Send final RTCP SR when done
        byte[] finalSr = RtcpPacket.buildSR(0L, packetCount, octetCount);
        rtcpSocket.send(new DatagramPacket(finalSr, finalSr.length, dest, destRtpPort));
        System.out.println("[RTCP] Sent final SR — packets=" + packetCount + " bytes=" + octetCount);
        rtpSocket.close();
        rtcpSocket.close();
        System.out.println("[RTCP] Done sending.");

    }
}
