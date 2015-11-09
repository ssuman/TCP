import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

public class ReadHandler implements Handler<SelectionKey> {

	private final Map<DatagramChannel, Queue<ByteBuffer>> pendingData;

	public ReadHandler(Map<DatagramChannel, Queue<ByteBuffer>> pendingData) {
		this.pendingData = pendingData;
	}

	@Override
	public void handle(SelectionKey key) throws IOException {
		
		DatagramChannel ch = (DatagramChannel) key.channel();
		ch.configureBlocking(false);
		ByteBuffer buffer = ByteBuffer.allocate(1500);
		SocketAddress addr = ch.receive(buffer);
		key.attach(addr);
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		queue.add(buffer);
		pendingData.put(ch, queue);
		key.interestOps(SelectionKey.OP_WRITE);

	}

}
