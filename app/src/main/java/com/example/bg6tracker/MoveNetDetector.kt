package com.example.bg6tracker

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class MoveNetDetector(
    context: Context
) {

    companion object {

        const val INPUT_SIZE =
            192

        const val INPUT_CHANNELS =
            3

        const val KEYPOINT_COUNT =
            17

        const val CONFIDENCE_THRESHOLD =
            0.30f
    }

    private val interpreter:
        Interpreter

    init {

        /*
         * Model phải nằm tại:
         *
         * app/src/main/assets/
         *
         * với tên:
         *
         * movenet_lightning_int8.tflite
         */
        val descriptor =
            context.assets.openFd(
                "movenet_lightning_int8.tflite"
            )

        val inputStream =
            FileInputStream(
                descriptor.fileDescriptor
            )

        try {

            val channel =
                inputStream.channel

            val model =
                channel.map(
                    FileChannel.MapMode.READ_ONLY,

                    descriptor.startOffset,

                    descriptor.declaredLength
                )

            /*
             * Interpreter options.
             *
             * T606:
             * dùng 2 CPU threads để tránh
             * chiếm toàn bộ CPU.
             */
            val options =
                Interpreter.Options()

            options.setNumThreads(2)

            interpreter =
                Interpreter(
                    model,
                    options
                )

        } finally {

            /*
             * File descriptor không cần giữ
             * mở sau khi model đã mmap.
             */
            inputStream.close()
        }

        validateModel()
    }

    private fun validateModel() {

        val inputTensor =
            interpreter
                .getInputTensor(0)

        val inputShape =
            inputTensor.shape()

        /*
         * Expected:
         *
         * [1, 192, 192, 3]
         */
        if (
            inputShape.size != 4 ||
            inputShape[0] != 1 ||
            inputShape[1] != INPUT_SIZE ||
            inputShape[2] != INPUT_SIZE ||
            inputShape[3] != INPUT_CHANNELS
        ) {

            throw IllegalStateException(
                "Unexpected MoveNet input shape: " +
                    inputShape.contentToString()
            )
        }
    }

    /*
     * Tạo DirectByteBuffer:
     *
     * 192 × 192 × 3
     *
     * uint8 = 110592 bytes
     */
    fun createInput():
        ByteBuffer {

        return ByteBuffer
            .allocateDirect(
                INPUT_SIZE *
                    INPUT_SIZE *
                    INPUT_CHANNELS
            )
            .order(
                ByteOrder.nativeOrder()
            )
    }

    /*
     * Chạy inference.
     *
     * Return:
     *
     * [x, y, confidence]
     *
     * x/y normalized 0..1
     */
    fun detect(
        input: ByteBuffer
    ): FloatArray {

        if (
            input.capacity() !=
            INPUT_SIZE *
                INPUT_SIZE *
                INPUT_CHANNELS
        ) {

            throw IllegalArgumentException(
                "Invalid input buffer size: " +
                    input.capacity()
            )
        }

        input.rewind()

        /*
         * MoveNet Lightning thường trả:
         *
         * [1, 1, 17, 3]
         *
         * mỗi keypoint:
         *
         * [y, x, score]
         */
        val output =
            Array(1) {

                Array(1) {

                    Array(
                        KEYPOINT_COUNT
                    ) {

                        FloatArray(3)
                    }
                }
            }

        interpreter.run(
            input,
            output
        )

        /*
         * Keypoint 0.
         *
         * Theo format MoveNet:
         *
         * [y, x, confidence]
         */
        val y =
            output[0][0][0][0]

        val x =
            output[0][0][0][1]

        val confidence =
            output[0][0][0][2]

        return floatArrayOf(
            x.coerceIn(
                0.0f,
                1.0f
            ),

            y.coerceIn(
                0.0f,
                1.0f
            ),

            confidence.coerceIn(
                0.0f,
                1.0f
            )
        )
    }

    /*
     * Lấy toàn bộ 17 keypoint.
     *
     * Dùng cho nghiên cứu/debug nếu cần.
     *
     * Return:
     * FloatArray 51 phần tử:
     *
     * [x,y,score,
     *  x,y,score,...]
     */
    fun detectAllKeypoints(
        input: ByteBuffer
    ): FloatArray {

        input.rewind()

        val output =
            Array(1) {

                Array(1) {

                    Array(
                        KEYPOINT_COUNT
                    ) {

                        FloatArray(3)
                    }
                }
            }

        interpreter.run(
            input,
            output
        )

        val result =
            FloatArray(
                KEYPOINT_COUNT * 3
            )

        for (
            i in 0 until KEYPOINT_COUNT
        ) {

            val y =
                output[0][0][i][0]

            val x =
                output[0][0][i][1]

            val confidence =
                output[0][0][i][2]

            val index =
                i * 3

            result[index] =
                x.coerceIn(
                    0.0f,
                    1.0f
                )

            result[index + 1] =
                y.coerceIn(
                    0.0f,
                    1.0f
                )

            result[index + 2] =
                confidence.coerceIn(
                    0.0f,
                    1.0f
                )
        }

        return result
    }

    fun close() {

        interpreter.close()
    }
}
