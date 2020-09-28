package com.nclab.audiorecognition

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.round

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val RECORDER_SAMPLE_RATE = 44100
private const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
private const val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
private const val MIN_FREQUENCY = 20
private const val MAX_FREQUENCY = 8000
private const val BUFFER_SIZE = 8192
private const val FFT_SIZE = 16384
private const val FFT_SIZE_LN = 14

class MainActivity : AppCompatActivity() {

    private var permissionToRecordAccepted = false

    private var recordingThread: Thread? = null

    private var isRecording = false

    private var recorder: AudioRecord? = null

    private val handler = Handler()
    private val fft = FFT(FFT_SIZE, FFT_SIZE_LN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)

        tgb_record.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tgb_record.isChecked = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun startRecording() {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BUFFER_SIZE
        ).apply {
            startRecording()
        }

        isRecording = true

        recordingThread = Thread({ audioAnalyze() }, "AudioAnalyzeThread").apply { start() }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
            isRecording = false
            recordingThread?.join()
            recordingThread = null
            recorder = null
        }
        recordingThread?.run()
    }

    private fun audioAnalyze() {
        val prevData = ShortArray(BUFFER_SIZE)
        val data = ShortArray(BUFFER_SIZE)
        while (isRecording) {
            val length = recorder?.read(data, 0, BUFFER_SIZE)
            if (length == BUFFER_SIZE) {
                getFFT(prevData + data)
                data.copyInto(prevData)
            }
            //Log.wtf("DEBUG", "L=$length")
        }
    }

    private fun getFFT(input: ShortArray) {

        //Log.wtf("DEBUG-", input.maxOrNull().toString())

        val real = DoubleArray(FFT_SIZE)
        val y = DoubleArray(FFT_SIZE)
        for (i in 0 until FFT_SIZE) {
            real[i] = input[i] / 32768.0
            y[i] = 0.0
        }
        fft.fft(real, y)

        var maxVal = 0.0
        var maxIndex = 0
        for (i in MIN_FREQUENCY until MAX_FREQUENCY) {
            if (real[i] > maxVal) {
                maxVal = real[i]
                maxIndex = i
            }
        }

        val hz = maxIndex * RECORDER_SAMPLE_RATE / FFT_SIZE
        Log.wtf("FHZ", hz.toString())
        handler.post { Runnable {
            tv_frequency.text = "$hz Hz"
            getPitch(hz)
        }.run() }
    }

    private val pitchName = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val pitchNameSimple = arrayOf("1", "1#", "2", "2#", "3", "4", "4#", "5", "5#", "6", "7#", "7")

    private fun getPitch(frequency: Int) {
        val pitchNo = 12 * log2(frequency / 440.0) + 45
        val pitchNoInt = round(pitchNo).toInt()
        val pitchStr = pitchName[pitchNoInt % 12]
        val pitchStrSimple = pitchNameSimple[pitchNoInt % 12]
        val offset = pitchNoInt / 12 - 3

        tv_Info.text = "pitch: $pitchStr  $offset"
        tv_Info2.text = "simple: $pitchStrSimple  $offset"
        tv_Info3.text = "diff: ${pitchNo - pitchNoInt}"
    }
}