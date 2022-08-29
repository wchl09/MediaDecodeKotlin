package audio

import java.io.InputStream

/**
 *音频解析实现类
 * @author wangchunlei 2022年08月26日10:14:26
 */
abstract class AudioDecoderImpl : IAudioDecoder {

    //    override val samplesPerSec: Long
//        get() = TODO("Not yet implemented")
//    override val channelCount: Int
//        get() = TODO("Not yet implemented")
//    override val duration: Long
//        get() = TODO("Not yet implemented")
    abstract val TAG: String

    /**
     * 解析状态
     */
    private var _decoderState: AudioDecoderState = AudioDecoderState.AudioDecoderIde
    final override val decoderState: AudioDecoderState
        get() = _decoderState

    /**
     * 解析回调列表
     */
    private val _callbackList = mutableListOf<AudioDecoderCallback>()
    protected val callbackList: List<AudioDecoderCallback> = _callbackList

    //    override fun start() {
//    }
//
//    override fun prepare() {
//    }
    protected fun onDecode(byteArray: ByteArray, start: Int, len: Int) {
        _callbackList.forEach {
            it.onDecoded(byteArray, start, len)
        }
    }

    override fun addCallback(callback: AudioDecoderCallback) {
        if (!_callbackList.contains(callback)) {
            _callbackList.add(callback)
        }
    }

    override fun removeCallback(callback: AudioDecoderCallback) {
        if (_callbackList.contains(callback)) {
            _callbackList.remove(callback)
        }
    }

    protected fun onError(msg: String, exception: Exception?) {
        _decoderState = AudioDecoderState.AudioDecoderError(_decoderState, msg, exception)
        _callbackList.forEach {
            it.onStateChange(_decoderState)
        }
    }

    protected fun onDecoderStateChange(state: AudioDecoderState) {
        _decoderState = state
        _callbackList.forEach {
            it.onStateChange(_decoderState)
        }
    }

    fun logout(msg: String) {
        println(msg)
    }

    companion object {
        fun create(inputStream: InputStream) {
        }
    }
}