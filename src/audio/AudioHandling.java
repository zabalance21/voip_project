package audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.File;

public class AudioHandling {

    // Read .wav file and convert into PCMU
    public static byte[] readWavAsPCMU(String path) throws Exception {
        File audioFile =  new File(path);
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(audioFile);
        System.out.println("[Audio] Original format : " + pcmStream.getFormat());

        // Convert to standard PCM 8hkz/16bit/mono
        AudioFormat pcmFormat = new AudioFormat(8000, 16, 1, true, false);
        AudioInputStream pcm8kstream = AudioSystem.getAudioInputStream(pcmFormat, pcmStream);

        // Convert PCM -> PCMU (G.711 u-law)
        AudioFormat ulawFormat = new AudioFormat(
                AudioFormat.Encoding.ULAW,
                8000,
                8,
                1,
                1,
                8000,
                false
        );
        AudioInputStream ulawStream = AudioSystem.getAudioInputStream(ulawFormat, pcm8kstream);

        byte[] pcmubytes = ulawStream.readAllBytes();
        ulawStream.close();
        System.out.println("[AUDIO] Read " + pcmubytes.length + " PCMU bytes from " + path);
        return pcmubytes;
    }

    // Decode PCMU BYTES -> PCM BYTES
    public static byte[] decodePcmuBytes(byte[] pcmuBytes) throws Exception{

        AudioFormat ulawFormat = new AudioFormat(
                AudioFormat.Encoding.ULAW, 8000,
                8, 1, 1,8000,false
        );
        AudioInputStream ulawStream = new AudioInputStream(
                new ByteArrayInputStream(pcmuBytes),
                ulawFormat,
                pcmuBytes.length
        );

        // Convert PCMU -> PCM
        AudioFormat pcmFormat = new AudioFormat(8000, 16, 1, true, false);
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, ulawStream);

        byte[] pcmBytes = pcmStream.readAllBytes();
        pcmStream.close();

        return pcmBytes;
    }

    // Opens speaker for playback
    public static SourceDataLine getPlaybackLine() throws Exception {
        AudioFormat format = new AudioFormat(
                8000,
                16,
                1,
                true,
                false
        );

        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, 8000);
        line.start();

        System.out.println("[AUDIO] Playback line opened: " + format);
        return line;

    }

}
