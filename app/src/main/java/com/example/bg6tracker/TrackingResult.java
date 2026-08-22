package com.example.bg6tracker;

public class TrackingResult {

    public final float x;
    public final float y;
    public final float confidence;
    public final boolean valid;

    public TrackingResult(
            float x,
            float y,
            float confidence,
            boolean valid) {

        this.x = x;
        this.y = y;
        this.confidence = confidence;
        this.valid = valid;
    }
          }
