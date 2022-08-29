package audio

/**
 * 音频解析器的数据回调
 */
interface AudioDecoderCallback {
    /**
     * 解析器状态变更
     */
    fun onStateChange(state: AudioDecoderState)

    /**
     * 解析出了数据
     */
    fun onDecoded(byteArray: ByteArray, start: Int, len: Int)
}