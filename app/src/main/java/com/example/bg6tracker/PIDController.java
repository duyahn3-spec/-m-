package com.example.bg6tracker;

public class PIDController {

    private final float kp;
    private final float ki;
    private final float kd;

    private float integralX;
    private float integralY;

    private float previousX;
    private float previousY;

    private final float maxOutput;
    private final float deadZone;

    public PIDController(
            float kp,
            float ki,
            float kd,
            float maxOutput,
            float deadZone) {

        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.maxOutput = maxOutput;
        this.deadZone = deadZone;
    }

    public synchronized float[] update(
            float errorX,
            float errorY,
            float dt) {

        if (dt <= 0) {
            dt = 0.01f;
        }

        if (Math.abs(errorX) <= deadZone) {
            errorX = 0;
        }

        if (Math.abs(errorY) <= deadZone) {
            errorY = 0;
        }

        integralX += errorX * dt;
        integralY += errorY * dt;

        float derivativeX =
                (errorX - previousX) / dt;

        float derivativeY =
                (errorY - previousY) / dt;

        previousX = errorX;
        previousY = errorY;

        float outputX =
                kp * errorX +
                ki * integralX +
                kd * derivativeX;

        float outputY =
                kp * errorY +
                ki * integralY +
                kd * derivativeY;

        outputX = clamp(outputX, -maxOutput, maxOutput);
        outputY = clamp(outputY, -maxOutput, maxOutput);

        return new float[]{outputX, outputY};
    }

    private float clamp(
            float value,
            float min,
            float max) {

        return Math.max(min, Math.min(max, value));
    }

    public synchronized void reset() {
        integralX = 0;
        integralY = 0;
        previousX = 0;
        previousY = 0;
    }
}
