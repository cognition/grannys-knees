package ca.aeo3.grannysknees.data

import androidx.core.util.AtomicFile
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.Charset

/**
 * Atomically replaces a file's contents with UTF-8 text.
 *
 * Writes through AndroidX [AtomicFile] so a process kill mid-write leaves the
 * previous file intact (or no file, when creating for the first time).
 */
internal class AtomicFileWriter(
    private val destination: File,
) {
    /**
     * Writes [text] to [destination], replacing any existing contents.
     *
     * The [OutputStreamWriter] is flushed but not closed before
     * [AtomicFile.finishWrite], which must sync and close the stream itself.
     *
     * @throws IOException when the parent directory cannot be created or the write fails
     */
    fun writeText(text: String, charset: Charset = Charsets.UTF_8) {
        ensureParentExists()
        val atomic = AtomicFile(destination)
        val stream = atomic.startWrite()
        try {
            val writer = OutputStreamWriter(stream, charset)
            writer.write(text)
            writer.flush()
            // Do not close writer/stream here — finishWrite syncs and closes.
            atomic.finishWrite(stream)
        } catch (error: IOException) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun ensureParentExists() {
        val parent = destination.parentFile ?: return
        if (!parent.exists()) {
            parent.mkdirs()
        }
    }

    companion object {
        /** Convenience entry point for one-off writes. */
        fun writeText(
            destination: File,
            text: String,
            charset: Charset = Charsets.UTF_8,
        ) {
            AtomicFileWriter(destination).writeText(text, charset)
        }
    }
}
