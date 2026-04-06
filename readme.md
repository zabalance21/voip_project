# MCO2: Real-Time Audio Streaming over IP

## Group Members
Ronin P. Zerna<br>
Don Oswin D. Campos

## Instructions for Compiling and Running

First, compile both the `client1` and `client2` Java files:

```bash
javac client1.java
javac client2.java
```

Run client2 first:
- client2 will listen for any SIP message that comes through:
```bash
java client2
```
Run client1 next:
- You will choose among the available sample audio files:
```bash
java client1
```

SIP negotiation:
- client1 automatically sends a SIP INVITE.
- client2 receives and processes the INVITE, sending a 200 OK response.
- This finishes the SIP handshake between the two clients.

RTP session (audio streaming):
- After SIP negotiation, an RTP session is initialized.
- client1 sends the audio file, and client2 receives and plays it in real-time.

Ending the session:
- After finishing the audio, client1 automatically sends a SIP BYE message.
- client2 receives the BYE, which ends the RTP session and terminates the program.

## Description of Implemented Features

### 1. SIP Signaling (over UDP)
- Client1 sends a SIP **INVITE** message to Client2 containing an SDP body
  with media details (IP address, RTP port, codec)
- Client2 responds with SIP **200 OK** containing its own SDP body
- Client1 confirms with SIP **ACK** to establish the session
- After audio streaming is done, Client1 sends SIP **BYE** to terminate the call
- Client2 responds with SIP **200 OK** to confirm termination
- SIP error responses (4xx, 5xx) are handled gracefully with logging

### 2. SDP (Session Description Protocol)
- SDP body is included in INVITE and 200 OK messages
- Specifies: IP address, RTP port, and codec (PCMU)
- Receiver extracts RTP port from received SDP to know where to send/receive audio

### 3. RTP Audio Streaming (over UDP)
- After SIP negotiation, Client1 reads a WAV audio file and converts it to
  PCMU (G.711 u-law) using Java built-in audio conversion
- Audio is packetized into **160-byte frames** (20ms per frame at 8kHz)
- Each frame is wrapped in an RTP packet with: Version, Sequence Number,
  Timestamp, and SSRC
- Packets are sent over UDP every **20ms**
- Client2 receives RTP packets, extracts PCMU payload, decodes it back to
  PCM, and plays it in real-time through the system speaker

### 4. RTCP Statistics (over UDP)
- Client1 sends **RTCP Sender Report (SR)** every 50 RTP packets (~1 second)
  containing: Packet count, Octet count, NTP timestamp
- Client2 sends **RTCP Receiver Report (RR)** every 50 packets containing:
  Cumulative packets lost, Jitter, LSR, DLSR
- RTCP listener runs on a separate background (daemon) thread on the
  receiver side

### 5. Audio Codec (G.711 PCMU)
- WAV file is read and converted to PCMU using Java `AudioSystem`
- Received PCMU payload is decoded back to 16-bit PCM using Java `AudioSystem`
- Audio is played in real-time using `javax.sound.sampled.SourceDataLine`

### 6. Error Handling
- SIP 4xx and 5xx responses are logged and handled without crashing
- Unexpected or malformed packets are caught and logged
- RTP receiver stops cleanly after **5 seconds** of no incoming packets
  (`SocketTimeoutException`)
- All sockets are properly closed after the session ends
