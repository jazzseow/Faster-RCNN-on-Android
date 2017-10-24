package com.example.jazz.realtimeobjectdetection.Utils;

/**
 * Created by jazz on 21/10/17.
 */

public class PredictionResult {

    private float left;
    private float top;
    private float right;
    private float bot;
    private float confidence;
    private String label;

    public PredictionResult(float left, float top, float right, float bot, float confidence, String label) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bot = bot;
        this.confidence = confidence;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBot() {
        return bot;
    }

    public void setBot(float bot) {
        this.bot = bot;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
