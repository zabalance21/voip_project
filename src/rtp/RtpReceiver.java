package rtp;

import audio.AudioHandling;

import javax.sound.sampled.SourceDataLine;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Random;

//utilites for storing received audio file
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class RtpReceiver {
    private volatile boolean running = true;

    // Stats for RTCP RR
    private int cumulativeLost = 0;
    private int expectedSeq = -1;
    private int highestSeq = 0;
    private long lastTransit = 0;
    private double jitter = 0;
    private long lastSrTimestamp = 0; // LSR from last received SR
    private long lastSrReceivedAt = 0; // time we recieved at that SR
    private long recieverSSRC = new Random().nextLong() & 0xFFFFFFFFL;
    private DatagramSocket rtcpSocket;
    private DatagramSocket rtpSocket;

    public void receive(int rtpPort, int rtcpPort, String senderIp, int senderRtcpPort) throws Exception{
        rtpSocket = new DatagramSocket(rtpPort);
        rtcpSocket = new DatagramSocket(rtcpPort);
        rtpSocket.setSoTimeout(5000); // Stop if silent 5s

        // Open speaker for playback
        SourceDataLine speaker = AudioHandling.getPlaybackLine();

        byte[] buf = new byte[4096];
        InetAddress senderAddr = InetAddress.getByName(senderIp);
        System.out.println("[RTP] Listening on port "+ rtpPort);

        // RTCP listener thread
        Thread rtcpThread = new Thread(() -> {
            byte[] rbuf = new byte[1024];
            while(running){
                try{
                    DatagramPacket dp = new DatagramPacket(rbuf, rbuf.length);
                    rtcpSocket.receive(dp);

                    byte[] srBytes = Arrays.copyOf(dp.getData(), dp.getLength());
                    RtcpPacket.parse(srBytes);

                    lastSrTimestamp = RtcpPacket.extractLSR(srBytes);
                    lastSrReceivedAt = System.currentTimeMillis();

                } catch (SocketException e){
                    if (running) e.printStackTrace();
                    break;
                }catch (Exception e) {
                    break;
                }
            }
        });
        rtcpThread.setDaemon(true);
        rtcpThread.start();

        long packetCount = 0;

        //buffer to store pcmu frames for writing to file
        ByteArrayOutputStream pcm_recv = new ByteArrayOutputStream();
        File outputFolder = new File("../recv_files/");
        File recv_wav = new File(outputFolder, "recv_file.wav");
        


        
        // RTP Receive Loop
        while(running){
            try{
                DatagramPacket dp =  new DatagramPacket(buf, buf.length);
                rtpSocket.receive(dp);

                // Parse raw bytes into rtp_packet
                byte[] raw = Arrays.copyOf(dp.getData(), dp.getLength());
                rtp_packet pkt = new rtp_packet(raw, dp.getLength());

                // Extract PCMU payload
                byte[] pcmuFrame = new byte[pkt.getpayload_length()];
                pkt.getpayload(pcmuFrame);

                System.out.println("[RTP] Received packet seq=" + pkt.getsequencenumber() +
                        " ts=" + pkt.gettimestamp() +
                        " size=" + pcmuFrame.length);

                updateStats(pkt);

                // Decode PCMU -> PCM
                byte[] pcm = AudioHandling.decodePcmuBytes(pcmuFrame);

                // Play decoded PCMU
                speaker.write(pcm, 0, pcm.length);

                //Save PCM to pcm_recv buffer in order to save to Wav
                pcm_recv.write(pcm);

                packetCount++;

                // Send RTCP RR every 50 packets
                if(packetCount % 50 == 0){
                    sendRR(rtcpSocket, senderAddr, senderRtcpPort);
                }

            }catch (SocketTimeoutException e){
                System.out.println("[RTP] No packets for 5s — stopping receiver.");
                break;
            } catch (SocketException e){
                if (running) e.printStackTrace();
                break;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        speaker.drain();
        speaker.close();
        rtpSocket.close();
        rtcpSocket.close();

        //Save received PCM data to wav file
        try {
            byte[] audioData = pcm_recv.toByteArray();
            ByteArrayInputStream pcm_recv_InputStream = new ByteArrayInputStream(audioData);
            
            AudioInputStream ais = new AudioInputStream(pcm_recv_InputStream,new javax.sound.sampled.AudioFormat(8000,16,1,true,false),audioData.length / 2 );

            AudioSystem.write(ais, javax.sound.sampled.AudioFileFormat.Type.WAVE, recv_wav);
            System.out.println("Saved received audio to " + recv_wav.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[RTP] Receiver stopped.");
    }

    // ── Update jitter and loss stats per packet ────────────────────────────
    private void updateStats(rtp_packet pkt) {
        int seq = pkt.getsequencenumber();
        highestSeq = seq;

        // ── Packet loss tracking ───────────────────────────────────────────
        if (expectedSeq == -1) {
            expectedSeq = seq; // very first packet
        } else {
            int gap = seq - expectedSeq;
            if (gap > 1) {
                cumulativeLost += (gap - 1);
                System.out.println("[RTP] Gap detected — lost "
                        + (gap - 1) + " packet(s)");
            }
        }
        expectedSeq = seq + 1;

        // ── Jitter calculation ───────────────────
        // arrival time in 8kHz timestamp units
        long arrival = System.currentTimeMillis() * 8;
        long transit = arrival - pkt.gettimestamp();
        if (lastTransit != 0) {
            double d = Math.abs(transit - lastTransit);
            jitter += (d - jitter) / 16.0;
        }
        lastTransit = transit;
    }

    private void sendRR(DatagramSocket rtcpSocket, InetAddress dest, int destRtcpPort) throws Exception{
        long dlsr = RtcpPacket.computeDLSR(lastSrReceivedAt);

        byte[] rr = RtcpPacket.buildRR(
                recieverSSRC,
                0L,
                0,
                cumulativeLost,
                highestSeq,
                (long) jitter,
                lastSrTimestamp,
                dlsr
        );

        rtcpSocket.send(new DatagramPacket(rr, rr.length, dest, destRtcpPort));
        System.out.println("[RTCP] Sent RR — lost=" + cumulativeLost + " jitter=" + String.format("%.1f", jitter)
        + " dlsr=" + dlsr);
    }

    public void stop(){
        running = false;
        if(rtpSocket != null && !rtpSocket.isClosed()) rtpSocket.close();
        if(rtcpSocket != null && !rtcpSocket.isClosed()) rtcpSocket.close();
    }

}
