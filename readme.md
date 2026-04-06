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