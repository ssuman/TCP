import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ClientWriterHandler implements Handler<SelectionKey> {

	private Map<DatagramChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();
	private Connection con ;
	private static String filename;
	
	public ClientWriterHandler(Map<DatagramChannel, Queue<ByteBuffer>> pendingData , 
			Connection con, String filename) {
		this.pendingData = pendingData;
		this.con = con;
		this.filename = filename;
	}
	
	@Override
	public void handle(SelectionKey key) throws IOException {
		
		DatagramChannel channel = (DatagramChannel) key.channel();
		Queue<ByteBuffer> queue = pendingData.get(channel);
		int size = queue.size();
		for (int i = 0; i < size; i++) {
			ByteBuffer buffer = queue.poll();
			if (buffer != null) {
				con.sendRequest(channel, buffer);
			}
		}
		key.interestOps(SelectionKey.OP_READ);
	}

}
