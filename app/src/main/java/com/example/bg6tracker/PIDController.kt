package com.example.bg6tracker

import kotlin.math.abs

class PIDController(
    private val kp: Double = 0.75,
    private val ki: Double = 0.001,
    private val kd: Double = 0.28,
    private val maxOutput: Double = 250.0,
    private val deadZone: Double = 3.0
) {

    private var integralX =
        0.0

    private var integralY =
        0.0

    private var previousX =
        0.0

    private var previousY =
        0.0

    private var initialized =
        false

    fun reset() {

        integralX = 0.0
        integralY = 0.0

        previousX = 0.0
        previousY = 0.0

        initialized =
            false
    }

    /*
     * Return:
     *
     * Pair(
     *     outputX,
     *     outputY
     * )
     */
    @Synchronized
    fun update(
        errorXInput: Double,
        errorYInput: Double,
        dtInput: Double
    ): Pair<Int, Int> {

        val dt =
            dtInput.coerceIn(
                0.001,
                0.1
            )

        /*
         * Dead zone.
         */
        val errorX =
            applyDeadZone(
                errorXInput
            )

        val errorY =
            applyDeadZone(
                errorYInput
            )

        /*
         * Integral.
         */
        integralX +=
            errorX * dt

        integralY +=
            errorY * dt

        /*
         * Anti-windup.
         */
        integralX =
            integralX.coerceIn(
                -1000.0,
                1000.0
            )

        integralY =
            integralY.coerceIn(
                -1000.0,
                1000.0
            )

        /*
         * Derivative.
         */
        val derivativeX =
            if (initialized) {

                (
                    errorX -
                        previousX
                    ) / dt

            } else {

                0.0
            }

        val derivativeY =
            if (initialized) {

                (
                    errorY -
                        previousY
                    ) / dt

            } else {

                0.0
            }

        previousX =
            errorX

        previousY =
            errorY

        initialized =
            true

        /*
         * PID.
         */
        val outputX =
            kp * errorX +
            ki * integralX +
            kd * derivativeX

        val outputY =
            kp * errorY +
            ki * integralY +
            kd * derivativeY

        /*
         * Saturation.
         */
        val limitedX =
            outputX.coerceIn(
                -maxOutput,
                maxOutput
            )

        val limitedY =
            outputY.coerceIn(
                -maxOutput,
                maxOutput
            )

        return Pair(
            limitedX.toInt(),
            limitedY.toInt()
        )
    }

    private fun applyDeadZone(
        value: Double
    ): Double {

        return if (
            abs(value) <
            deadZone
        ) {

            0.0

        } else {

            value
        }
    }

    /*
     * Cho phép thay đổi thông số
     * trong lúc nghiên cứu.
     */
    fun getKp():
        Double = kp

    fun getKi():
        Double = ki

    fun getKd():
        Double = kd

    fun getMaxOutput():
        Double = maxOutput

    fun getDeadZone():
        Double = deadZone
    }
}
