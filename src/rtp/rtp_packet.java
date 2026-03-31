package rtp;


public class rtp_packet {
    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public long TimeStamp;
    public long Ssrc;
    
    //Bitstream of the RTP header
    public byte[] header;

    //size of the RTP payload
    public int payload_size;
    //Bitstream of the RTP payload
    public byte[] payload;

    public rtp_packet(int Framenb, long Time, byte[] data, int data_length) {
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        PayloadType = 0;

        SequenceNumber = Framenb;
        TimeStamp = Time;

        header = new byte[HEADER_SIZE];

        header[0] = (byte) (Version << 6);
        header[0] |= (byte) (Padding << 5);
        header[0] |= (byte) (Extension << 4);
        header[0] |= (byte) (CC);

        header[1] = (byte) (Marker << 7);
        header[1] |= (byte) (PayloadType & 0x7F);

        header[2] = (byte) (SequenceNumber >> 8);
        header[3] = (byte) (SequenceNumber & 0xFF);

        header[4] = (byte) (TimeStamp >> 24);
        header[5] = (byte) (TimeStamp >> 16);
        header[6] = (byte) (TimeStamp >> 8);
        header[7] = (byte) (TimeStamp & 0xFF);

        header[8] = (byte) (Ssrc >> 24);
        header[9] = (byte) (Ssrc >> 16);
        header[10] = (byte) (Ssrc >> 8);
        header[11] = (byte) (Ssrc & 0xFF);

        payload_size = data_length;
        payload = new byte[data_length];

        for (int i = 0; i < data_length; i++) 
        {
        payload[i] = data[i];
        }
    }

    public rtp_packet(byte[] packet, int packet_size) {
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        if (packet_size >= HEADER_SIZE) {

        header = new byte[HEADER_SIZE];
        for (int i = 0; i < HEADER_SIZE; i++)
            header[i] = packet[i];

        payload_size = packet_size - HEADER_SIZE;
        payload = new byte[payload_size];
        for (int i = HEADER_SIZE; i < packet_size; i++)
            payload[i - HEADER_SIZE] = packet[i];

        PayloadType = header[1] & 127;

        SequenceNumber = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);

        TimeStamp = unsigned_int(header[7])
            + 256 * unsigned_int(header[6])
            + 65536 * unsigned_int(header[5])
            + 16777216 * unsigned_int(header[4]);
        }
    }

    public int getpayload(byte[] data) {
        for (int i = 0; i < payload_size; i++)
        data[i] = payload[i];
        return payload_size;
    }

    public int getpayload_length() {
        return payload_size;
    }

    public int getlength() {
        return payload_size + HEADER_SIZE;
    }

    public int getpacket(byte[] packet) {
        for (int i = 0; i < HEADER_SIZE; i++)
        packet[i] = header[i];
        for (int i = 0; i < payload_size; i++)
        packet[i + HEADER_SIZE] = payload[i];
        return payload_size + HEADER_SIZE;
    }

    public long gettimestamp() {
        return TimeStamp;
    }

    public int getsequencenumber() {
        return SequenceNumber;
    }

    public int getpayloadtype() {
        return PayloadType;
    }

    static int unsigned_int(int nb) {
        if (nb >= 0)
        return nb;
        else
        return 256 + nb;
    }

}
