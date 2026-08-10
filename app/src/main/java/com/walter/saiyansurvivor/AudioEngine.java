package com.walter.saiyansurvivor;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

/**
 * Reliable procedural audio engine for Android.
 * Music is synthesized to the STREAM_MUSIC output using a legacy AudioTrack
 * (better compatibility on some Xiaomi/HyperOS devices). SFX use ToneGenerator
 * so they remain audible even if streaming PCM is unavailable on a device.
 */
public final class AudioEngine {
    public static final int SFX_KI = 1;
    public static final int SFX_KAME = 2;
    public static final int SFX_DISC = 3;
    public static final int SFX_BOMB = 4;
    public static final int SFX_HIT = 5;
    public static final int SFX_PICKUP = 6;
    public static final int SFX_LEVEL = 7;
    public static final int SFX_CHEST = 8;
    public static final int SFX_TRANSFORM = 9;
    public static final int SFX_BOSS = 10;

    private static final int SAMPLE_RATE = 44100;
    private static final int FRAMES = 1024;

    private volatile boolean running;
    private volatile float targetIntensity;
    private volatile float smoothedIntensity;
    private Thread musicThread;
    private AudioTrack track;
    private ToneGenerator sfxTone;

    private double leadPhase;
    private double bassPhase;
    private double kickPhase;
    private long sampleIndex;

    public void start() {
        if (running) return;
        running = true;

        try {
            sfxTone = new ToneGenerator(AudioManager.STREAM_MUSIC, 92);
            sfxTone.startTone(ToneGenerator.TONE_PROP_ACK, 110);
        } catch (Throwable ignored) {
            sfxTone = null;
        }

        try {
            int min = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            int bytes = Math.max(min > 0 ? min : 0, FRAMES * 8);
            track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bytes,
                    AudioTrack.MODE_STREAM
            );
            if (track.getState() == AudioTrack.STATE_INITIALIZED) {
                track.setVolume(1.0f);
                track.play();
                musicThread = new Thread(this::musicLoop, "SaiyanMusic");
                musicThread.setDaemon(true);
                musicThread.start();
            } else {
                try { track.release(); } catch (Throwable ignored) { }
                track = null;
            }
        } catch (Throwable ignored) {
            track = null;
        }
    }

    public void stop() {
        running = false;
        if (musicThread != null) {
            try { musicThread.join(300); } catch (InterruptedException ignored) { }
            musicThread = null;
        }
        if (track != null) {
            try { track.stop(); } catch (Throwable ignored) { }
            try { track.flush(); } catch (Throwable ignored) { }
            try { track.release(); } catch (Throwable ignored) { }
            track = null;
        }
        if (sfxTone != null) {
            try { sfxTone.release(); } catch (Throwable ignored) { }
            sfxTone = null;
        }
    }

    public void setIntensity(float intensity) {
        targetIntensity = clamp(intensity, 0f, 1f);
    }

    public void sfx(int type) {
        ToneGenerator tone = sfxTone;
        if (tone == null) return;
        try {
            int toneType;
            int duration;
            switch (type) {
                case SFX_KAME:
                    toneType = ToneGenerator.TONE_DTMF_9; duration = 150; break;
                case SFX_DISC:
                    toneType = ToneGenerator.TONE_DTMF_D; duration = 90; break;
                case SFX_BOMB:
                    toneType = ToneGenerator.TONE_DTMF_0; duration = 220; break;
                case SFX_HIT:
                    toneType = ToneGenerator.TONE_PROP_NACK; duration = 70; break;
                case SFX_PICKUP:
                    toneType = ToneGenerator.TONE_DTMF_3; duration = 45; break;
                case SFX_LEVEL:
                    toneType = ToneGenerator.TONE_PROP_ACK; duration = 180; break;
                case SFX_CHEST:
                    toneType = ToneGenerator.TONE_DTMF_6; duration = 170; break;
                case SFX_TRANSFORM:
                    toneType = ToneGenerator.TONE_DTMF_A; duration = 320; break;
                case SFX_BOSS:
                    toneType = ToneGenerator.TONE_DTMF_0; duration = 360; break;
                default:
                    toneType = ToneGenerator.TONE_DTMF_2; duration = 35;
            }
            tone.startTone(toneType, duration);
        } catch (Throwable ignored) { }
    }

    private void musicLoop() {
        short[] buffer = new short[FRAMES];
        while (running && track != null) {
            for (int i = 0; i < buffer.length; i++) {
                smoothedIntensity += (targetIntensity - smoothedIntensity) * 0.0012f;
                float value = musicSample();
                value = clamp(value, -0.94f, 0.94f);
                buffer[i] = (short)(value * 32767f);
                sampleIndex++;
            }
            try {
                int written = track.write(buffer, 0, buffer.length);
                if (written < 0) break;
            } catch (Throwable ignored) {
                break;
            }
        }
    }

    private float musicSample() {
        float bpm = 88f + 80f * smoothedIntensity;
        double samplesPerBeat = SAMPLE_RATE * 60.0 / bpm;
        double beatPos = sampleIndex / samplesPerBeat;
        double beatFrac = beatPos - Math.floor(beatPos);
        int eighth = (int)Math.floor(beatPos * 2.0);
        double eighthFrac = beatPos * 2.0 - Math.floor(beatPos * 2.0);

        int[] pattern = {0, 3, 7, 10, 7, 5, 12, 10, 0, 3, 7, 15, 12, 10, 7, 5};
        int note = pattern[eighth & 15];
        double leadHz = 130.81 * Math.pow(2.0, note / 12.0);
        double bassHz = 65.41 * Math.pow(2.0, (note % 12) / 12.0);

        leadPhase += 2.0 * Math.PI * leadHz / SAMPLE_RATE;
        bassPhase += 2.0 * Math.PI * bassHz / SAMPLE_RATE;
        if (leadPhase > Math.PI * 2) leadPhase -= Math.PI * 2;
        if (bassPhase > Math.PI * 2) bassPhase -= Math.PI * 2;

        float leadEnv = (float)Math.exp(-eighthFrac * (4.8 - 1.6 * smoothedIntensity));
        float lead = ((float)Math.sin(leadPhase)
                + 0.22f * (float)Math.sin(leadPhase * 2.0))
                * leadEnv * (0.20f + 0.08f * smoothedIntensity);

        float bassEnv = (float)Math.exp(-beatFrac * 3.4);
        float bass = (float)Math.sin(bassPhase) * bassEnv * (0.18f + 0.07f * smoothedIntensity);

        float kickEnv = (float)Math.exp(-beatFrac * 18.0);
        double kickHz = 52.0 + 75.0 * (1.0 - beatFrac);
        kickPhase += 2.0 * Math.PI * kickHz / SAMPLE_RATE;
        if (kickPhase > Math.PI * 2) kickPhase -= Math.PI * 2;
        float kick = (float)Math.sin(kickPhase) * kickEnv * (0.18f + 0.11f * smoothedIntensity);

        float pulse = 0f;
        if (smoothedIntensity > 0.35f && (eighth & 1) == 1) {
            pulse = (float)Math.sin(leadPhase * 0.5) * (float)Math.exp(-eighthFrac * 12.0)
                    * 0.10f * smoothedIntensity;
        }

        return lead + bass + kick + pulse;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
