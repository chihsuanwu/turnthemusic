package com.nclab.audiorecognition

import kotlin.math.cos
import kotlin.math.sin

class FFT(private var fftSize: Int, private var fftPow: Int) {

    private var cos = DoubleArray(fftSize / 2)
    private var sin = DoubleArray(fftSize / 2)

    init {
        if (fftSize != 1 shl fftPow) throw RuntimeException()

        for (i in 0 until fftSize / 2) {
            val deg = -2 * Math.PI * i / fftSize
            cos[i] = cos(deg)
            sin[i] = sin(deg)
        }
    }

    fun fft(real: DoubleArray, imaginary: DoubleArray) {
        bitReverse(real, imaginary)

        var a: Int
        var c: Double
        var s: Double
        var t1: Double
        var t2: Double

        // FFT
        var n2 = 1

        for (i in 0 until fftPow) {
            val n1 = n2
            n2 += n2
            a = 0

            for (j in 0 until n1) {
                c = cos[a]
                s = sin[a]
                a += 1 shl fftPow - i - 1

                for (k in j until fftSize step n2) {
                    t1 = c * real[k + n1] - s * imaginary[k + n1]
                    t2 = s * real[k + n1] + c * imaginary[k + n1]
                    real[k + n1] = real[k] - t1
                    imaginary[k + n1] = imaginary[k] - t2
                    real[k] = real[k] + t1
                    imaginary[k] = imaginary[k] + t2
                }
            }
        }
    }

    private fun bitReverse(real: DoubleArray, imaginary: DoubleArray) {
        var j = 0
        val halfSize = fftSize / 2

        for (i in 1 until fftSize - 1) {
            var n = halfSize
            while (j >= n) {
                j -= n
                n /= 2
            }
            j += n
            if (i < j) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imaginary[i]
                imaginary[i] = imaginary[j]
                imaginary[j] = temp
            }
        }
    }
}