import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TCPClient implements ClientInterface {

	private Connection con;
	private static String filename;
	public TCPClient() {

	}

	private static Map<Integer, Boolean> ackedPack = new HashMap<>();
	private static Map<DatagramChannel, Queue<ByteBuffer>> pendingData = new HashMap<>();

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if( args.length !=1){
			System.out.println(" Usage: java TCPClient <inputFileName>");
		}
		filename = args[0];
		TCPClient client = new TCPClient();
		client.performHandShake();
		receiveRequest(client.con);
	}

	@Override
	public void performHandShake() throws IOException {
		con = new Connection();
		DatagramChannel channel = con.openConnection();
		// selectionKey.attach(filename);
		ByteBuffer buffer = initialSYN(channel);
		con.sendRequest(channel, buffer);
	}

	private static ByteBuffer initialSYN(DatagramChannel channel) throws IOException {
		Segment seg = Segment.initialACK();
		byte[] bytes = Segment.serializeToBytes(seg);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		return buffer;
	}

	private static void receiveRequest(Connection con) throws IOException, ClassNotFoundException {
		Selector selector = con.getSelector();
		while (true) {
			selector.select();
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> keyIter = keys.iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				keyIter.remove();
				if (key.isReadable()) {
					new ClientReadHandler(pendingData, filename).handle(key);
				} else if (key.isWritable()) {
					new ClientWriterHandler(pendingData, con, filename).handle(key);
				}
			}
		}
	}

	@Override
	public void sendFin() {

	}

}
