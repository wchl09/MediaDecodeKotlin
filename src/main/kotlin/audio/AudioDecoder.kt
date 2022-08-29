package audio

import audio.wav.WavDecoder
import java.io.File
import java.io.InputStream
import java.net.URI

/**
 * 音频解析器
 */
object AudioDecoderFactory {
    fun create(inputStream: InputStream): IAudioDecoder {
        return WavDecoder(inputStream)
    }
}