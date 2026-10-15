package tech.nimbbl.webviewsdk.testutil

import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Minimal single-threaded HTTP/1.1 server backed by a plain [ServerSocket] (loopback only).
 * Duplicated from nimbbl_mobile_kit_core_api_sdk's androidTest sources (test code isn't shared
 * across modules) — lets NimbblCheckoutMainActivity's real updateOrder() network call be tested
 * against a local server instead of the live Nimbbl backend.
 */
class LocalHttpServer : Closeable {

    data class RecordedRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String
    )

    data class CannedResponse(
        val code: Int,
        val message: String = "OK",
        val headers: Map<String, String> = emptyMap(),
        val body: String = ""
    )

    private val serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    @Volatile private var nextResponse = CannedResponse(200, "OK", body = "{}")
    private val requests = ArrayBlockingQueue<RecordedRequest>(16)
    private val thread = Thread { acceptLoop() }.apply {
        isDaemon = true
        start()
    }

    val port: Int get() = serverSocket.localPort
    val baseUrl: String get() = "http://127.0.0.1:$port/"

    fun enqueueResponse(response: CannedResponse) {
        nextResponse = response
    }

    /** Waits briefly for the server to have handled a request, then returns it (or null on timeout). */
    fun takeRequest(timeoutSeconds: Long = 5): RecordedRequest? =
        requests.poll(timeoutSeconds, TimeUnit.SECONDS)

    private fun acceptLoop() {
        while (!serverSocket.isClosed) {
            val socket = try {
                serverSocket.accept()
            } catch (_: Exception) {
                return // socket closed from close()
            }
            try {
                handle(socket)
            } catch (_: Exception) {
                // Malformed request or client disconnect — ignore, keep serving.
            } finally {
                socket.close()
            }
        }
    }

    private fun handle(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val requestLine = input.readLine() ?: return
        val parts = requestLine.split(" ")
        val method = parts.getOrElse(0) { "" }
        val path = parts.getOrElse(1) { "" }

        val headers = LinkedHashMap<String, String>()
        var contentLength = 0
        while (true) {
            val line = input.readLine()
            if (line.isNullOrEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                headers[key] = value
                if (key.equals("Content-Length", ignoreCase = true)) {
                    contentLength = value.toIntOrNull() ?: 0
                }
            }
        }

        val bodyChars = CharArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = input.read(bodyChars, read, contentLength - read)
            if (n == -1) break
            read += n
        }
        val body = String(bodyChars, 0, read)

        requests.offer(RecordedRequest(method, path, headers, body))

        val response = nextResponse
        val bodyBytes = response.body.toByteArray(Charsets.UTF_8)
        val output = socket.getOutputStream()
        val head = buildString {
            append("HTTP/1.1 ${response.code} ${response.message}\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            response.headers.forEach { (k, v) -> append("$k: $v\r\n") }
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(head.toByteArray(Charsets.UTF_8))
        output.write(bodyBytes)
        output.flush()
    }

    override fun close() {
        serverSocket.close()
        thread.interrupt()
    }
}
