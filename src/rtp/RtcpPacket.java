package rtp;

public class RtcpPacket {
    // Packet Types
    public static final int senderPT = 200; // Sender Report
    public static final int receiverPT = 201; // Receiver Report

    // =========================================================================
    // SENDER REPORT (PT = 200)
    // Sent by: Client1 (the active RTP sender)
    // Contains: how much data was sent + NTP timestamp for sync
    // Size: 28 bytes (8 fixed header + 20 sender info)
    // =========================================================================
    public static byte[] buildSR(long ssrc, long packetCount, long octetCount){
        byte[] sr = new byte[28];

        // RTCP header (4 bytes)
        sr[0] = (byte) 0x80;
        sr[1] = (byte) senderPT;
        sr[2] = 0;
        sr[3] = 6;

        // SSRC of sender (4 bytes)
        sr[4] = (byte) ((ssrc >> 24) & 0xFF);
        sr[5] = (byte) ((ssrc >> 16) & 0xFF);
        sr[6] = (byte) ((ssrc >> 8) & 0xFF);
        sr[7] = (byte) (ssrc & 0xFF);

        // NTP Timestamp (8 bytes)
        // Full 64-bit NTP timestamp: seconds since Jan 1, 1900
        long now = System.currentTimeMillis();
        long ntpSeconds = (now / 1000L) + 2208988800L;
        long ntpFraction = ((now % 1000L) * 0x100000000L) / 1000L;
        sr[8] = (byte)((ntpSeconds >> 24) & 0xFF);
        sr[9] = (byte)((ntpSeconds >> 16) & 0xFF);
        sr[10] = (byte)((ntpSeconds >> 8) & 0xFF);
        sr[11] = (byte)(ntpSeconds & 0xFF);
        sr[12] = (byte)((ntpFraction >> 24) & 0xFF);
        sr[13] = (byte)((ntpFraction >> 16) & 0xFF);
        sr[14] = (byte)((ntpFraction >> 8) & 0xFF);
        sr[15] = (byte)(ntpFraction & 0xFF);

        // RTP Timestamp (4 bytes)
        // Corresponds to NTP timestamp above, in RTP clock units (8000 Hz)
        long rtpTimestamp = (now * 8) & 0xFFFFFFFFL;
        sr[16] = (byte)((rtpTimestamp >> 24) & 0xFF);
        sr[17] = (byte)((rtpTimestamp >> 16) & 0xFF);
        sr[18] = (byte)((rtpTimestamp >> 8) & 0xFF);
        sr[19] = (byte)(rtpTimestamp & 0xFF);

        // Sender's Packet Count (4 bytes)
        sr[20] = (byte)((packetCount >> 24) & 0xFF);
        sr[21] = (byte)((packetCount >> 16) & 0xFF);
        sr[22] = (byte)((packetCount >> 8) & 0xFF);
        sr[23] = (byte)(packetCount & 0xFF);

        // Sender's Octet Count (4 bytes)
        // Total payload bytes sent (does NOT include RTP headers)
        sr[24] = (byte)((octetCount >> 24) & 0xFF);
        sr[25] = (byte)((octetCount >> 16) & 0xFF);
        sr[26] = (byte)((octetCount >> 8)  & 0xFF);
        sr[27] = (byte)(octetCount & 0xFF);

        return sr;
    }

    // =========================================================================
    // RECEIVER REPORT (PT = 201)
    // Sent by: Client2 (the RTP receiver)
    // Contains: quality stats about the stream it is receiving
    // Size: 32 bytes (8 fixed header + 24 report block)
    // =========================================================================
    public static byte[] buildRR(long ssrc, long senderSsrc, int fractionlost, int cumLost,
                                 int extendedHighSeq, long jitter, long lastSR, long delaySinceLastSr){
        byte[] rr = new byte[32];

        //safeguard for range values of fractionlost and cumLost
        fractionlost = Math.max(0, Math.min(255, fractionlost));
        cumLost = Math.max(0, Math.min(0xFFFFFF, cumLost));

        // RTCP Common Header (4bytes)
        rr[0] = (byte) 0x81;
        rr[1] = (byte) receiverPT;
        rr[2] = 0;
        rr[3] = 7;

        // SSRC of Client 2 (Receiver) (4 bytes)
        rr[4] = (byte)((ssrc >> 24) & 0xFF);
        rr[5] = (byte)((ssrc >> 16) & 0xFF);
        rr[6] = (byte)((ssrc >> 8) & 0xFF);
        rr[7] = (byte)(ssrc & 0xFF);

        // ==== Report Block ====

        // SSRC of Client 1 (4 bytes)
        rr[8] = (byte)((senderSsrc >> 24) & 0xFF);
        rr[9] = (byte)((senderSsrc >> 16) & 0xFF);
        rr[10] = (byte)((senderSsrc >> 8) & 0xFF);
        rr[11] = (byte)(senderSsrc & 0xFF);

        // Fraction of RTP packets Lost (1 byte)
        // 0 = no loss, 255 = 100% loss since last RR
        rr[12] = (byte)(fractionlost & 0xFF);

        // Cumulative Packets Lost (3 bytes)
        // Total RTP packets lost
        rr[13] = (byte)((cumLost >> 16) & 0xFF);
        rr[14] = (byte)((cumLost  >> 8) & 0xFF);
        rr[15] = (byte)(cumLost  & 0xFF);

        // Extended Highest Sequence Number Received (4 bytes)
        // High 16 bits: count of sequence number cycles (wraps)
        // Low  16 bits: highest sequence number received
        rr[16] = (byte)((extendedHighSeq >> 24) & 0xFF);
        rr[17] = (byte)((extendedHighSeq >> 16) & 0xFF);
        rr[18] = (byte)((extendedHighSeq >> 8)  & 0xFF);
        rr[19] = (byte)(extendedHighSeq          & 0xFF);

        // Interarrival jitter (4 bytes)
        // Statistical variance of the packet interarrival time
        rr[20] = (byte)((jitter >> 24) & 0xFF);
        rr[21] = (byte)((jitter >> 16) & 0xFF);
        rr[22] = (byte)((jitter >> 8) & 0xFF);
        rr[23] = (byte)(jitter & 0xFF);

        // Last SR timestamp (4 bytes)
        // 0 = no SR received yet,  Middle 32 bits of the NTP timestamp from the most recent SR received
        rr[24] = (byte)((lastSR >> 24) & 0xFF);
        rr[25] = (byte)((lastSR >> 16) & 0xFF);
        rr[26] = (byte)((lastSR >> 8) & 0xFF);
        rr[27] = (byte)(lastSR & 0xFF);

        // Delay since last SR (4 bytes) (DLSR)
        //  Time elapsed between receiving the last SR and sending this R, 0 = no SR received yet
        rr[28] = (byte)((delaySinceLastSr >> 24) & 0xFF);
        rr[29] = (byte)((delaySinceLastSr >> 16) & 0xFF);
        rr[30] = (byte)((delaySinceLastSr >> 8) & 0xFF);
        rr[31] = (byte)(delaySinceLastSr & 0xFF);

        return rr;
    }

    // PARSE INCOMING RTCP PACKET
    public static void parse(byte[] data){
        if(data == null || data.length < 8){
            System.out.println("[RTCP] packet too short to parse.");
            return;
        }

        int version = (data[0] >> 6) & 0x03;
        int rc = data[0] & 0x1F;
        int packetType = data[1] & 0xFF;
        int length = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        long ssrc = ((long)(data[4] & 0xFF) << 24) | ((long)(data[5] & 0xFF) << 16)
                | ((long)(data[6] & 0xFF) << 8) |  (long)(data[7] & 0xFF);
        System.out.println("[RTCP] Version=" + version + " RC=" + rc + " Type=" + packetType
                + " Length=" + ((length + 1) * 4) + "B" + " SSRC=" + ssrc);

        if(packetType == senderPT && data.length >= 28){
            long packetCount = ((long)(data[20] & 0xFF) << 24) | ((long)(data[21] & 0xFF) << 16) |
                    ((long)(data[22] & 0xFF) << 8) |   (long)(data[23] & 0xFF);

            long octetCount = ((long)(data[24] & 0xFF) << 24) | ((long)(data[25] & 0xFF) << 16) |
                    ((long)(data[26] & 0xFF) << 8) | (long)(data[27] & 0xFF);

            System.out.println("[RTCP] SR — packetsSent=" + packetCount + " bytesSent=" + octetCount);

        }else if(packetType == receiverPT && data.length >= 32){
            int fractionLost = data[12] & 0xFF;
            int cumLost = ((data[13] & 0xFF) << 16) | ((data[14] & 0xFF) << 8) | (data[15] & 0xFF);
            long jitter = ((long)(data[20] & 0xFF) << 24) | ((long)(data[21] & 0xFF) << 16) |
                    ((long)(data[22] & 0xFF) << 8) | (long)(data[23] & 0xFF);

            System.out.println("[RTCP] RR — fractionLost=" + fractionLost + " cumulativeLost=" + cumLost +
                    " jitter=" + jitter);
        }else{
            System.out.println("[RTCP] Unknown or unsupported packetType=" + packetType);
        }
    }

    // HELPER - Extract LSR from a received SR
    // Used to populate the LSR field in next RR
    public static long extractLSR(byte[] srData){
        if (srData == null || srData.length < 16) return 0;

        int packetType = srData[1] & 0xFF;
        if (packetType != senderPT) return 0;
        return ((long)(srData[10] & 0xFF) << 24) | ((long)(srData[11] & 0xFF) << 16) |
                ((long)(srData[12] & 0xFF) << 8) | ((long)(srData[13] & 0xFF));
    }

    // HELPER - Compute DLSR
    public static long computeDLSR(long lastSrReceivedAtMs){
        if(lastSrReceivedAtMs == 0){ return 0; }
        long elapsedMs = System.currentTimeMillis() - lastSrReceivedAtMs;
        return (elapsedMs * 65536L) / 1000L; // Convert ms → 1/65536 sec units: (elapsed / 1000) * 65536

    }
}



