package audio

import java.io.Closeable

/**
 * 音频文件解析接口类
 * @author wangchunlei 2022年08月26日07:56:58
 */
interface IAudioDecoder : Closeable {
    /**
     * 采样率
     */
    val samplesPerSec: Long

    /**
     * 声道数量
     */
    val channelCount: Int

    /**
     * 时长
     */
    val duration: Long

    /**
     * 解析器状态
     */
    val decoderState: AudioDecoderState

    /**
     * bit per sample
     */
    val bitsPerSample:Int

    /**
     * 开始输出PCM数据
     */
    fun start()

    /**
     * 准备
     */
    fun prepare()

    /**
     * 从头开始重新播放
     */
    fun restart()

    fun pause()

    fun resume()

    fun addCallback(callback: AudioDecoderCallback)
    fun removeCallback(callback: AudioDecoderCallback)

}