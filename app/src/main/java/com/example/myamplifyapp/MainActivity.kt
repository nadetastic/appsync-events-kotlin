package com.example.myamplifyapp
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Base64
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

const val REALTIME_DOMAIN = ""
const val HTTP_DOMAIN = ""
const val API_KEY = ""

val authorization = mapOf("x-api-key" to API_KEY, "host" to HTTP_DOMAIN)

fun getAuthProtocol(): String {
    val jsonString = JSONObject(authorization).toString()
    val header = Base64.encodeToString(jsonString.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    return "header-$header"
}

class MainActivity : ComponentActivity() {

    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val client = OkHttpClient()
            val request = Request.Builder().url("wss://$REALTIME_DOMAIN/event/realtime")
                .addHeader("Sec-WebSocket-Protocol", "aws-appsync-event-ws, ${getAuthProtocol()}")
                .build()
            val listener = EventWebSocketListener()
            webSocket = client.newWebSocket(request, listener)


            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {

                    Text("APPSYNC EVENTS", fontSize = 30.sp)
                    Button(onClick = {
                        subscribeToChannel(webSocket,"/default/channel")
                    }) {
                        Text(text = "Subscribe to default channel")
                    }
                }
            }
        }

    }

    override fun onDestroy(){
        super.onDestroy()
        webSocket.close(1000,"Activity destroyed")
    }
}


class EventWebSocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        // Connection opened
        Log.i("Event API Open", response.message)

        val connInitString = JSONObject(
            mapOf("type" to "connection_init")
        ).toString()
        Log.i("Event API CON_INT",connInitString)
        webSocket.send(connInitString)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // Received message
        Log.i("Event API Message", text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        // Connection closing
        Log.i("Event API Closing", "$reason - $code")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // Connection failed
        Log.i("Event API Failure", "ERROR CONNECTING $response")
    }
}

fun subscribeToChannel(socket: WebSocket, channel: String){
    val date = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    Log.i("Subscribe id", date)
    val message = mapOf("type" to "subscribe", "id" to date.toString(), "channel" to channel, "authorization" to authorization)
    socket.send(JSONObject(message).toString())
}