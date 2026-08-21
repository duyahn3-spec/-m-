package com.example.bg6tracker

data class TrackingResult(

    /*
     * Vị trí normalized 0..1
     * hoặc pixel tùy nơi tạo object.
     */
    val x: Float,

    val y: Float,

    /*
     * Velocity.
     */
    val vx: Float,

    val vy: Float,

    /*
     * Confidence của detector.
     */
    val confidence: Float,

    /*
     * Frame sequence.
     */
    val sequence: Int,

    /*
     * Latency từng stage.
     */
    val captureMs: Double,

    val preprocessMs: Double,

    val inferenceMs: Double,

    val trackingMs: Double,

    val controlMs: Double,

    val totalMs: Double,

    /*
     * FPS thực tế.
     */
    val fps: Double
)
