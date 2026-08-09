package com.walter.saiyansurvivor;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.concurrent.ConcurrentLinkedQueue;

/** Procedural music + SFX engine: no copyrighted audio assets required. */
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

    private static final int SAMPLE_RATE = 22050;
    private static final int BUFFER_SAMPLES = 1024;

    private final ConcurrentLinkedQueue<SfxVoice> queuedSfx = new ConcurrentLinkedQueue<>();
    private final SfxVoice[] active = new SfxVoice[8];
    private volatile boolean running;
    private volatile float targetIntensity;
    private Thread thread;
    private AudioTrack track;

    private double musicPhase;
    private double bassPhase;
    private double hatPhase;
    private double beatCursor;
    private float smoothedIntensity;
    private long musicalSample;

    public void start() {
        if (running) return;
        int min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int size = Math.max(min, BUFFER_SAMPLES * 4);
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(size).setTransferMode(AudioTrack.MODE_STREAM).build();
        running = true;
        track.play();
        thread = new Thread(this::loop, "SaiyanAudio");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) { try { thread.join(250); } catch (InterruptedException ignored) { } thread = null; }
        if (track != null) { try { track.pause(); track.flush(); track.release(); } catch (Exception ignored) { } track = null; }
    }

    public void setIntensity(float intensity) { targetIntensity = clamp(intensity, 0f, 1f); }
    public void sfx(int type) { queuedSfx.offer(new SfxVoice(type)); }

    private void loop() {
        short[] buffer = new short[BUFFER_SAMPLES];
        while (running && track != null) {
            for (int i = 0; i < buffer.length; i++) {
                smoothedIntensity += (targetIntensity - smoothedIntensity) * 0.0008f;
                float mixed = clamp(musicSample() * 0.52f + sfxSample() * 0.72f, -0.96f, 0.96f);
                buffer[i] = (short) (mixed * 32767f);
                musicalSample++;
            }
            try { track.write(buffer, 0, buffer.length, AudioTrack.WRITE_BLOCKING); }
            catch (Exception ignored) { running = false; }
        }
    }

    private float musicSample() {
        float bpm = 84f + 80f * smoothedIntensity;
        double samplesPerBeat = SAMPLE_RATE * 60.0 / bpm;
        double pos = (musicalSample % (long) (samplesPerBeat * 16.0)) / samplesPerBeat;
        int step = ((int) Math.floor(pos * 2.0)) & 31;
        double eighthPhase = (pos * 2.0) - Math.floor(pos * 2.0);
        double beatPhase = pos - Math.floor(pos);
        int[] melody = {0,3,7,10,7,3,12,10,0,3,7,15,12,10,7,3};
        int note = melody[(step / 2) & 15];
        double root = 110.0 * Math.pow(2.0, note / 12.0);
        musicPhase += 2.0 * Math.PI * root / SAMPLE_RATE; if (musicPhase > Math.PI * 2) musicPhase -= Math.PI * 2;
        double bassFreq = 55.0 * Math.pow(2.0, (note % 12) / 12.0);
        bassPhase += 2.0 * Math.PI * bassFreq / SAMPLE_RATE; if (bassPhase > Math.PI * 2) bassPhase -= Math.PI * 2;
        float gate = (float) Math.exp(-eighthPhase * (5.2 - 2.0 * smoothedIntensity));
        float lead = (float) Math.sin(musicPhase) * gate * (0.13f + 0.06f * smoothedIntensity);
        float harmonic = (float) Math.sin(musicPhase * 2.01) * gate * 0.035f * smoothedIntensity;
        float bass = (float) Math.sin(bassPhase) * (float) Math.exp(-beatPhase * 3.8) * (0.11f + 0.05f * smoothedIntensity);
        float kickEnv = (float) Math.exp(-beatPhase * 18.0);
        float kickFreq = 55f + 70f * (1f - (float) beatPhase);
        beatCursor += 2.0 * Math.PI * kickFreq / SAMPLE_RATE; if (beatCursor > Math.PI * 2) beatCursor -= Math.PI * 2;
        float kick = (float) Math.sin(beatCursor) * kickEnv * (0.10f + 0.08f * smoothedIntensity);
        hatPhase = hatPhase * 1664525.0 + 1013904223.0;
        float noise = (float) ((hatPhase % 2048.0) / 1024.0 - 1.0);
        float hatGate = step % 2 == 1 ? (float) Math.exp(-eighthPhase * 28.0) : 0f;
        float hat = noise * hatGate * 0.055f * smoothedIntensity;
        return lead + harmonic + bass + kick + hat;
    }

    private float sfxSample() {
        SfxVoice next;
        while ((next = queuedSfx.poll()) != null) {
            boolean inserted = false;
            for (int i = 0; i < active.length; i++) if (active[i] == null || active[i].done) { active[i] = next; inserted = true; break; }
            if (!inserted) active[0] = next;
        }
        float out = 0f;
        for (SfxVoice voice : active) if (voice != null && !voice.done) out += voice.sample();
        return out;
    }

    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private static final class SfxVoice {
        final int type; int age; boolean done; double phase;
        SfxVoice(int type) { this.type = type; }
        float sample() {
            float t = age / (float) SAMPLE_RATE, duration, start, end, amp;
            switch (type) {
                case SFX_KAME: duration=.48f;start=170;end=880;amp=.34f;break;
                case SFX_DISC: duration=.30f;start=980;end=520;amp=.25f;break;
                case SFX_BOMB: duration=.70f;start=120;end=42;amp=.42f;break;
                case SFX_HIT: duration=.10f;start=120;end=55;amp=.33f;break;
                case SFX_PICKUP: duration=.08f;start=780;end=1320;amp=.16f;break;
                case SFX_LEVEL: duration=.40f;start=390;end=1040;amp=.27f;break;
                case SFX_CHEST: duration=.45f;start=300;end=950;amp=.28f;break;
                case SFX_TRANSFORM: duration=.85f;start=95;end=720;amp=.34f;break;
                case SFX_BOSS: duration=.75f;start=85;end=48;amp=.38f;break;
                default: duration=.10f;start=520;end=850;amp=.14f;
            }
            if (t >= duration) { done = true; return 0f; }
            float k = t / duration, freq = start + (end - start) * k;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float)Math.sin(Math.PI * Math.min(1f,k*5f)) * (float)Math.pow(1f-k,1.7);
            float result = ((float)Math.sin(phase) + (float)Math.sin(phase*2.03)*.22f) * env * amp;
            age++; return result;
        }
    }
}
