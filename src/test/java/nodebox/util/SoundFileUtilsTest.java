package nodebox.util;

import org.junit.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SoundFileUtilsTest {

    @Test
    public void testSynthesizedWavDecoding() throws Exception {
        // Create a 1-second 44.1kHz 16-bit mono 440Hz sine wave WAV file
        float sampleRate = 44100;
        int numSamples = (int) sampleRate;
        byte[] pcmData = new byte[numSamples * 2];
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * 440.0 * i / sampleRate;
            short val = (short) (Math.sin(angle) * 30000);
            pcmData[i * 2] = (byte) (val & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
        AudioInputStream ais = new AudioInputStream(bais, format, numSamples);

        File tempWav = File.createTempFile("nodebox_test_sound_", ".wav");
        tempWav.deleteOnExit();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWav);
        String path = tempWav.getAbsolutePath();

        // 1. Test getAudioInfo
        Map<String, Object> info = SoundFileUtils.getAudioInfo(path);
        assertNotNull(info);
        assertEquals(1, info.get("channels"));
        assertEquals(44100.0, (Double) info.get("sampleRate"), 0.1);
        assertTrue("Duration should be ~1.0 second", Math.abs((Double) info.get("duration") - 1.0) < 0.1);

        // 2. Test getWaveform
        List<Double> waveform = SoundFileUtils.getWaveform(path, 64, "left");
        assertNotNull(waveform);
        assertEquals(64, waveform.size());
        for (Double amp : waveform) {
            assertTrue("Waveform amplitudes should be between -1.0 and 1.0", amp >= -1.0 && amp <= 1.0);
        }

        // 3. Test getSpectrum
        List<Double> spectrum = SoundFileUtils.getSpectrum(path, 32);
        assertNotNull(spectrum);
        assertEquals(32, spectrum.size());
        for (Double band : spectrum) {
            assertTrue("Spectrum bands should be >= 0.0", band >= 0.0);
        }

        tempWav.delete();
    }

    @Test
    public void testNonExistentFile() {
        String missing = "missing_file_for_testing_12345.wav";
        List<Double> waveform = SoundFileUtils.getWaveform(missing, 50, "left");
        assertNotNull(waveform);
        assertTrue(waveform.isEmpty());

        List<Double> spectrum = SoundFileUtils.getSpectrum(missing, 16);
        assertNotNull(spectrum);
        assertTrue(spectrum.isEmpty());

        Map<String, Object> info = SoundFileUtils.getAudioInfo(missing);
        assertNotNull(info);
        assertEquals(0, info.get("channels"));
    }
}
