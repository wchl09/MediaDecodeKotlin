package audio.wav

import audio.AudioDecoderImpl
import audio.AudioDecoderState
import java.io.InputStream

/**
 * WAV格式音频数据解析类
 * https://docs.fileformat.com/audio/wav/
 * @author wangchunlei 2022年08月26日07:58:47
 */
class WavDecoder(private val inputStream: InputStream) : AudioDecoderImpl() {
    override val TAG = "WAV音频解析"
    private val riffFlagArray =
        intArrayOf(0X52, 0X49, 0X46, 0X46)//“52 49 46 46”这个是Ascii字符“RIFF”，这部分是固定格式，表明这是一个WAVE文件头。
    private val waveFlagArray = intArrayOf(0X57, 0X41, 0X56, 0X45)//WAVE
    private val dataFlagArray = intArrayOf(0X64, 0X61, 0X74, 0X61)//data

    //当前读取到的位置
    private var currentPosition: Long = 0L

    /**
     * WAV内容大小，应该等于文件总字节数-8
     * 4个字节[0-UInt.MAX_VALUE]
     */
    private var wavSize: UInt = 0u

    /**
     * Format chunk marker. Includes trailing null
     * 格式化标记，有可能为空（4个字节）
     */
    private var formatChunkMarker = intArrayOf(0, 0, 0, 0)

    /**
     * Length of format data as listed above
     * 4个字节[0-UInt.MAX_VALUE]
     */
    private var formatDataLength: UInt = 0u

    //声道数量
    override var channelCount = 0

    /**
     * 编码格式
     * 1:PCM/非压缩格式
     */
    private var pcmFormat: Int = 0

    /**
     * 采样率
     * 16进制
     */
    override var samplesPerSec = 0x0L

    /**
     * 每秒数据量
     */
    private var avgBytesPerSec = 0x0L

    /**
     * 对齐
     */
    private var blockAlign = 0

    /**
     * 量化位数
     */
    override var bitsPerSample: Int = 0

    /**
     * 音频时长（单位毫秒）
     */
    override val duration: Long
        get() = wavSize.toLong().minus(formatDataLength.toLong()).div(avgBytesPerSec).times(1000L)

    /**
     * 数据缓冲区
     */
    private val byteArray = ByteArray(2048)

    /**
     * Size of the data section.
     * Data的数据量（4个字节）
     */
    private var dataSize = 0U

    /**
     * 开始输出PCM数据
     */
    override fun start() {
        readPCMData()
    }

    /**
     * 准备数据
     */
    override fun prepare() {
        doPrepare()
    }


    override fun close() {
        inputStream.close()
    }

    /**
     * 从头开始重新播放
     */
    override fun restart() {
        onDecoderStateChange(AudioDecoderState.AudioDecoderPrepared)
        inputStream.reset()
        stop = false
        readPCMData()
    }

    override fun pause() {
        stop = true
    }

    override fun resume() {
        stop = false
        readPCMData()
    }

    /**-------------------------------------**/
    /**
     * 检测是否是WAV格式，并读取前4个字节
     */
    private fun checkRIFF() {
        //大端
        check(currentPosition == 0L) { "流的读取没有在开头" }
        //检测是否是正确的WAV格式文件
        repeat(4) {
            check(inputStream.read() == riffFlagArray[it]) { "不是正确的WAV格式" }
        }
        logout("检测RIFF标志通过")
        currentPosition += 4L
    }

    /**
     * 获取WAV文件的内容大小
     * 返回16进制数
     * 文件总字节数-8
     */
    private fun readWAVSize() {
        //小端
        check(currentPosition == 4L)
        //每次读取都是16进制数，需要进位并累加的到最后结果
        wavSize = inputStream.read().toUInt() +
                inputStream.read().toUInt().times(0X100u) +
                inputStream.read().toUInt().times(0X100u).times(0X100u) +
                inputStream.read().toUInt().times(0X100u).times(0X100u).times(0X100u)
        logout("WAV的内容大小:${wavSize}B")
        formatDataLength
        currentPosition += 4L
    }

    /**
     *检测是否是WAVE
     */
    private fun checkWAVFormat() {
        //大端
        check(currentPosition == 8L)
        repeat(4) {
            check(inputStream.read() == waveFlagArray[it]) { "WAVE格式不对" }
        }
        logout("WAVE格式检测通过")
        currentPosition += 4L
    }

    private fun readFormatChunkMarker() {
        check(currentPosition == 12L)
        repeat(4) {
            formatChunkMarker[it] = inputStream.read()
        }
        logout("FormatChunkMarker${formatChunkMarker.contentToString()}")
        currentPosition += 4
    }

    //格式化块大小
    private fun readFormatDataLength() {
        //小端
        check(currentPosition == 16L)
        formatDataLength = inputStream.read().toUInt() +
                inputStream.read().toUInt().times(0X100U) +
                inputStream.read().toUInt().times(0X100U).times(0X100U) +
                inputStream.read().toUInt().times(0X100U).times(0X100U).times(0X100U)
        logout("格式化数据块大小:${formatDataLength}")
        currentPosition += 4L
    }

    /**
     * 读取编码格式
     */
    private fun readPCMFormat() {
        //小端
        check(currentPosition == 20L)
        pcmFormat = inputStream.read() + inputStream.read().times(0X100)
        logout("编码格式:${pcmFormat}")
        currentPosition += 2
    }

    /**
     * 读取声道数量
     */
    private fun readChannels() {
        //小端
        check(currentPosition == 22L)
        channelCount = inputStream.read() + inputStream.read().times(0X100)
        logout("声道数量:${channelCount}")
        currentPosition += 2
    }

    /**
     * 读取采样频率
     */
    private fun readSamplePerSec() {
        //小端
        check(currentPosition == 24L)
        samplesPerSec = inputStream.read() +
                inputStream.read().times(0X100L) +
                inputStream.read().times(0X100L).times(0X100L) +
                inputStream.read().times(0X100L).times(0X100L).times(0X100L)
        logout("采样频率:${samplesPerSec}")
        currentPosition += 4
    }

    /**
     *读取每秒数据量
     * (Sample Rate * BitsPerSample * Channels) / 8.
     */
    private fun readAvgBytesPerSec() {
        //小端
        check(currentPosition == 28L)
        avgBytesPerSec = inputStream.read() +
                inputStream.read().times(0X100L) +
                inputStream.read().times(0X100L).times(0X100L) +
                inputStream.read().times(0X100L).times(0X100L).times(0X100L)
        logout("每秒数据量:${avgBytesPerSec}")
        currentPosition += 4
    }

    /**
     * (BitsPerSample * Channels) / 8.1 - 8 bit mono2 - 8 bit stereo/16 bit mono4 - 16 bit stereo
     */
    private fun readBlockAlign() {
        //小端
        check(currentPosition == 32L)
        blockAlign = inputStream.read() +
                inputStream.read().times(0X100)
        logout("对齐块:${blockAlign}")
        currentPosition += 2
    }

    /**
     * Bits per sample
     */
    private fun readBitsPerSample() {
        //小端
        check(currentPosition == 34L)
        bitsPerSample = inputStream.read() +
                inputStream.read().times(0X100)
        logout(":${bitsPerSample}")
        currentPosition += 2
    }

    /**
     * 检测DATA标志
     * DATA标志之后就是PCM格式数据了
     */
    private fun checkData() {
        var char = inputStream.read()
        currentPosition++
        //DATA前边还有可能有其它值，暂时还没研究出这些值的意思
        while (char != dataFlagArray[0]) {
            char = inputStream.read()
            currentPosition++
        }
        check(inputStream.read() == dataFlagArray[1])
        check(inputStream.read() == dataFlagArray[2])
        check(inputStream.read() == dataFlagArray[3])
        logout("DATA标志位检测通过")
        currentPosition += 3
        dataSize = inputStream.read().toUInt() +
                inputStream.read().toUInt().times(0X100U) +
                inputStream.read().toUInt().times(0X100U).times(0X100U) +
                inputStream.read().toUInt().times(0X100U).times(0X100U).times(0X100U)
        logout("DATA数据量:${dataSize}")
        currentPosition += 4
        inputStream.mark(currentPosition.toInt())
    }

    private var stop: Boolean = false

    /**
     * 开始读取PCM
     */
    private fun readPCMData() {
        logout("开始读取PCM数据:指针位置${currentPosition}")
        if (decoderState != AudioDecoderState.AudioDecoderPrepared) {
            logout("当前状态还没准备好:${decoderState}")
            return
        }
        onDecoderStateChange(AudioDecoderState.AudioDecoderRunning)
        var len = inputStream.read(byteArray)
        while (len != -1 && !stop) {
            onDecode(byteArray, 0, len)
            len = inputStream.read(byteArray)
        }
        if (len == -1) {
            logout("读取PCM数据读取完成")
        } else {
            logout("停止读取PCM数据")
        }
        onDecoderStateChange(AudioDecoderState.AudioDecoderStopped)
    }

    private fun doPrepare() {
        if (decoderState != AudioDecoderState.AudioDecoderIde) {
            return
        }
        onDecoderStateChange(AudioDecoderState.AudioDecoderPreparing)
        checkRIFF()
        readWAVSize()
        logout("大小:${wavSize.toString(10)}")//10进制输出
        checkWAVFormat()
        readFormatChunkMarker()
        readFormatDataLength()
        logout("格式化数据块大小=${formatDataLength}")
        readPCMFormat()
        logout("编码格式:${pcmFormat}")
        readChannels()
        logout("声道数量:${channelCount}")
        readSamplePerSec()
        logout("采样频率:${samplesPerSec}")
        readAvgBytesPerSec()
        logout("每秒数据量:${avgBytesPerSec}")
        logout("音频总时长:${duration}毫秒")
        readBlockAlign()
        logout("块对齐:${blockAlign}")
        readBitsPerSample()
        logout("量化位数${bitsPerSample}")
        checkData()
        onDecoderStateChange(AudioDecoderState.AudioDecoderPrepared)
    }
}