package audio

sealed class AudioDecoderState {
    /**
     * 音频解析状态：刚初始化
     */
    object AudioDecoderIde : AudioDecoderState()

    /**
     * 音频解析状态：正在准备中
     */
    object AudioDecoderPreparing : AudioDecoderState()

    /**
     * 音频解析状态：准备完成，可以开始输出数据了
     */
    object AudioDecoderPrepared : AudioDecoderState()

    /**
     * 音频解析状态：正在输出数据中
     */
    object AudioDecoderRunning : AudioDecoderState()

    /**
     * 音频解析状态：已经停止了
     */
    object AudioDecoderStopped : AudioDecoderState()

    /**
     * 音频解析状态：出错了
     * @param preState 出错前的状态
     * @param errorMsg 出错信息
     * @param exception 错误
     */
    data class AudioDecoderError(val preState: AudioDecoderState, val errorMsg: String, val exception: Exception?) :
        AudioDecoderState()

}
