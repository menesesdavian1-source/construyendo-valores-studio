package com.construyendovalores.studio;

public class Scene {
    public final int index;
    public final String character;
    public final String narration;
    public String imageFile = "";
    public String audioFile = "";
    public double audioSeconds = 0.0;
    public String video = "";
    public String scenario = "";
    public String karaokeText = "";
    public boolean karaoke = false;
    public java.util.List<Double> beats = new java.util.ArrayList<>();
    public String bigWord = "";
    public int listNumber = 0;
    public double revealAt = 0.0;
    public String emotion = "";

    public Scene(int index, String character, String narration) {
        this.index = index;
        this.character = character;
        this.narration = narration;
    }
}