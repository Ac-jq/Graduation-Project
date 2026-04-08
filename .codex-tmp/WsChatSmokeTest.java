import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WsChatSmokeTest {

    public static void main(String[] args) throws Exception {
        long appointmentId = Long.parseLong(args[0]);
        String studentToken = args[1];
        String counselorToken = args[2];

        HttpClient client = HttpClient.newHttpClient();
        QueueListener studentListener = new QueueListener();
        QueueListener counselorListener = new QueueListener();

        WebSocket student = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create("ws://127.0.0.1:8080/ws/consult-chat?appointmentId=" + appointmentId + "&token=" + studentToken), studentListener)
                .join();
        WebSocket counselor = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create("ws://127.0.0.1:8080/ws/consult-chat?appointmentId=" + appointmentId + "&token=" + counselorToken), counselorListener)
                .join();

        String studentConnected = studentListener.take();
        String counselorConnected = counselorListener.take();

        student.sendText("{\"content\":\"老师您好，我这周一直很焦虑。\"}", true).join();
        String studentMessage = studentListener.take();
        String counselorMessage = counselorListener.take();

        student.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        counselor.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();

        System.out.println("{");
        System.out.println("  \"studentConnected\": " + quote(studentConnected) + ",");
        System.out.println("  \"counselorConnected\": " + quote(counselorConnected) + ",");
        System.out.println("  \"studentMessage\": " + quote(studentMessage) + ",");
        System.out.println("  \"counselorMessage\": " + quote(counselorMessage));
        System.out.println("}");
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static class QueueListener implements WebSocket.Listener {

        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private final List<CharSequence> chunks = new ArrayList<>();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            chunks.add(data);
            if (last) {
                StringBuilder builder = new StringBuilder();
                for (CharSequence chunk : chunks) {
                    builder.append(chunk);
                }
                queue.offer(builder.toString());
                chunks.clear();
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            queue.offer("ERROR:" + error.getMessage());
        }

        public String take() throws Exception {
            String value = queue.poll(10, TimeUnit.SECONDS);
            if (value == null) {
                throw new IllegalStateException("timeout waiting for websocket message");
            }
            return value;
        }
    }
}
