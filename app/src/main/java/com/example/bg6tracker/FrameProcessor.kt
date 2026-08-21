package com.example.bg6tracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.Image
import java.nio.ByteBuffer

class FrameProcessor {

    companion object {

        const val WIDTH = 320
        const val HEIGHT = 240

        const val MODEL_SIZE = 192
    }

    /*
     * Bitmap nguồn.
     *
     * Dùng lại buffer thay vì tạo Bitmap mới
     * cho mỗi frame.
     */
    private var sourceBitmap:
        Bitmap? = null

    /*
     * Bitmap 192x192 đưa vào model.
     */
    private val modelBitmap =
        Bitmap.createBitmap(
            MODEL_SIZE,
            MODEL_SIZE,
            Bitmap.Config.ARGB_8888
        )

    private val canvas =
        Canvas(modelBitmap)

    private val sourceRect =
        Rect()

    private val destinationRect =
        Rect(
            0,
            0,
            MODEL_SIZE,
            MODEL_SIZE
        )

    private val pixels =
        IntArray(
            MODEL_SIZE *
                MODEL_SIZE
        )

    /*
     * Tạo hoặc lấy lại Bitmap nguồn.
     */
    private fun getSourceBitmap(
        width: Int,
        height: Int
    ): Bitmap {

        val current =
            sourceBitmap

        if (
            current == null ||
            current.width != width ||
            current.height != height ||
            current.isRecycled
        ) {

            sourceBitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )
        }

        return sourceBitmap!!
    }

    /*
     * ImageReader RGBA_8888
     * ->
     * RGB UINT8
     *
     * input phải có:
     *
     * 192 * 192 * 3
     *
     * bytes.
     */
    fun process(
        image: Image,
        input: ByteBuffer
    ) {

        require(
            image.width == WIDTH &&
                image.height == HEIGHT
        ) {
            "Unexpected image size: " +
                "${image.width}x${image.height}"
        }

        val plane =
            image.planes[0]

        val buffer =
            plane.buffer

        val pixelStride =
            plane.pixelStride

        val rowStride =
            plane.rowStride

        val rowPadding =
            rowStride -
                pixelStride * WIDTH

        val bitmapWidth =
            WIDTH +
                if (pixelStride > 0) {
                    rowPadding /
                        pixelStride
                } else {
                    0
                }

        val bitmap =
            getSourceBitmap(
                bitmapWidth,
                HEIGHT
            )

        /*
         * ImageReader buffer
         * -> Bitmap
         */
        buffer.rewind()

        try {

            bitmap.copyPixelsFromBuffer(
                buffer
            )

        } catch (
            e: Exception
        ) {

            throw IllegalStateException(
                "Unable to copy ImageReader buffer",
                e
            )
        }

        /*
         * Chỉ lấy vùng 320x240 thật.
         */
        sourceRect.set(
            0,
            0,
            WIDTH,
            HEIGHT
        )

        /*
         * Resize:
         *
         * 320x240
         * ->
         * 192x192
         *
         * Đây là resize trực tiếp.
         */
        canvas.drawBitmap(
            bitmap,
            sourceRect,
            destinationRect,
            null
        )

        /*
         * Lấy pixel.
         */
        modelBitmap.getPixels(
            pixels,
            0,
            MODEL_SIZE,
            0,
            0,
            MODEL_SIZE,
            MODEL_SIZE
        )

        input.rewind()

        /*
         * ARGB -> RGB
         */
        for (pixel in pixels) {

            val r =
                (pixel shr 16) and 0xFF

            val g =
                (pixel shr 8) and 0xFF

            val b =
                pixel and 0xFF

            input.put(
                r.toByte()
            )

            input.put(
                g.toByte()
            )

            input.put(
                b.toByte()
            )
        }

        input.rewind()
    }

    /*
     * Preview dùng cho UI nếu sau này cần.
     *
     * Trả về bản copy để thread khác không
     * sửa trực tiếp bitmap đang xử lý.
     */
    fun getPreviewBitmap():
        Bitmap {

        return modelBitmap.copy(
            Bitmap.Config.ARGB_8888,
            false
        )
    }

    fun release() {

        if (
            !modelBitmap.isRecycled
        ) {

            modelBitmap.recycle()
        }

        sourceBitmap?.let {

            if (
                !it.isRecycled
            ) {

                it.recycle()
            }
        }

        sourceBitmap =
            null
    }
}
