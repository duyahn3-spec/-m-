package com.example.bg6tracker

import kotlin.math.max

class KalmanTracker {

    /*
     * State:
     *
     * x
     * y
     * vx
     * vy
     */
    private var x =
        0.0

    private var y =
        0.0

    private var vx =
        0.0

    private var vy =
        0.0

    private var initialized =
        false

    /*
     * Measurement smoothing.
     *
     * Giá trị cao:
     * theo measurement nhanh hơn.
     *
     * Giá trị thấp:
     * mượt hơn nhưng trễ hơn.
     */
    private val alpha =
        0.65

    fun reset() {

        x = 0.0
        y = 0.0

        vx = 0.0
        vy = 0.0

        initialized =
            false
    }

    /*
     * Nhận measurement mới.
     *
     * Return:
     *
     * [x, y, vx, vy]
     */
    @Synchronized
    fun update(
        measurementX: Double,
        measurementY: Double,
        dtInput: Double
    ): DoubleArray {

        val dt =
            sanitizeDt(
                dtInput
            )

        if (!initialized) {

            x =
                measurementX

            y =
                measurementY

            vx = 0.0
            vy = 0.0

            initialized =
                true

            return state()
        }

        /*
         * Prediction.
         */
        val predictedX =
            x +
                vx * dt

        val predictedY =
            y +
                vy * dt

        /*
         * Correction.
         */
        val correctedX =
            predictedX +
                alpha *
                (
                    measurementX -
                        predictedX
                )

        val correctedY =
            predictedY +
                alpha *
                (
                    measurementY -
                        predictedY
                )

        /*
         * Velocity.
         */
        val newVx =
            (
                correctedX -
                    x
                ) / dt

        val newVy =
            (
                correctedY -
                    y
                ) / dt

        /*
         * Giới hạn velocity để tránh
         * một measurement lỗi tạo ra
         * spike quá lớn.
         */
        vx =
            newVx.coerceIn(
                -5000.0,
                5000.0
            )

        vy =
            newVy.coerceIn(
                -5000.0,
                5000.0
            )

        x =
            correctedX

        y =
            correctedY

        return state()
    }

    /*
     * Prediction khi không có measurement
     * đáng tin cậy.
     */
    @Synchronized
    fun predict(
        dtInput: Double
    ): DoubleArray {

        val dt =
            sanitizeDt(
                dtInput
            )

        if (!initialized) {
            return state()
        }

        x +=
            vx * dt

        y +=
            vy * dt

        return state()
    }

    /*
     * Predict trước một khoảng thời gian.
     *
     * Ví dụ:
     *
     * predictAhead(0.030)
     *
     * = dự đoán 30ms.
     */
    @Synchronized
    fun predictAhead(
        milliseconds: Double
    ): DoubleArray {

        if (!initialized) {
            return state()
        }

        val seconds =
            (
                milliseconds /
                    1000.0
            ).coerceIn(
                0.0,
                0.2
            )

        return doubleArrayOf(
            x + vx * seconds,
            y + vy * seconds,
            vx,
            vy
        )
    }

    fun isInitialized():
        Boolean {

        return initialized
    }

    fun getX():
        Double {

        return x
    }

    fun getY():
        Double {

        return y
    }

    fun getVelocityX():
        Double {

        return vx
    }

    fun getVelocityY():
        Double {

        return vy
    }

    private fun state():
        DoubleArray {

        return doubleArrayOf(
            x,
            y,
            vx,
            vy
        )
    }

    private fun sanitizeDt(
        dtInput: Double
    ): Double {

        return max(
            0.001,
            dtInput.coerceAtMost(
                0.2
            )
        )
    }
}
