import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ClientReadHandler implements Handler<SelectionKey> {

	static UnAckedMap unAckMap = new UnAckedMap();
	static Map<Integer, Integer> ackMap = new HashMap<>();
	static String filename;
	final static int BYTE_COUNT = 1024;
	static boolean isInFastRecovery = false;
	static State state;

	private static List<ByteBuffer> itemQueue = new ArrayList<>();

	private final Map<DatagramChannel, Queue<ByteBuffer>> pendingData;

	public ClientReadHandler(Map<DatagramChannel, Queue<ByteBuffer>> pendingData, String filename) {
		this.pendingData = pendingData;
		ClientReadHandler.state = State.SLOW;
		this.filename = filename;
	}

	@Override
	public void handle(SelectionKey key) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1500);
		DatagramChannel channel = (DatagramChannel) key.channel();
		channel.configureBlocking(false);
		channel.receive(buf);
		Segment seg = null;
		try {
			seg = Segment.deserializeBytes(buf);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		switch (seg.flag) {
		case SYN_ACK:
			onSYN_ACK(channel, seg);
			break;
		case ACK:
			onACK(channel, seg);
			break;
		case FIN:
			onFIN(channel, seg);
			break;
		case FIN_ACK:
			System.out.println("Received FIN");
			System.exit(0);
		default:
			break;
		}
		key.interestOps(SelectionKey.OP_WRITE);

	}

	private void onFIN(DatagramChannel channel, Segment seg) throws IOException {
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		Segment segment = Segment.sendFINACK();
		byte [] bytes = Segment.serializeToBytes(segment);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		queue.add(buffer);
		pendingData.put(channel, queue);
		
	}

	private void onACK(DatagramChannel channel, Segment seg) throws IOException {

		switch (state) {
		case SLOW:
			if (!Utils.isDupAck(seg, ackMap, itemQueue)) {
				new SlowStartHandler(channel, seg, pendingData, itemQueue).handler();
			}
			break;
		case RETRANSMIT:
			if (!Utils.isDupAckFastReceover(seg, ackMap, itemQueue)) {
				new FastRecoveryHandler(channel, seg, pendingData, itemQueue).handler();
			}
			break;
		case CON_AVD:
			if (!Utils.isDupCA(seg, ackMap, itemQueue)) {
				new CongestionAvoidanceHandler(seg, pendingData,itemQueue,channel).handler();
			}
			break;
		}

	}

	public ClientReadHandler() {
		this.pendingData = new HashMap<>();
		ClientReadHandler.state = State.SLOW;
	}

//	public static void main(String args[]) throws IOException {
//		itemQueue.add(ByteBuffer.wrap("hi".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi1".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi2".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi3".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi4".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi4".getBytes()));
//		itemQueue.add(ByteBuffer.wrap("hi5".getBytes()));
//
//		UnAckedMap.unAckMap.put(0, 0);
//		UnAckedMap.unAckMap.put(1, 1);
//		ClientReadHandler handler = new ClientReadHandler();
//		Segment seg = new Segment();
//		seg.acknowledgementNbr = 0;
//
//		Segment seg1 = new Segment();
//		seg1.acknowledgementNbr = 1;
//
//		Segment seg2 = new Segment();
//		seg2.acknowledgementNbr = 2;
//
//		Segment seg3 = new Segment();
//		seg3.acknowledgementNbr = 2;
//
//		Segment seg4 = new Segment();
//		seg4.acknowledgementNbr = 2;
//
//		Segment seg5 = new Segment();
//		seg5.acknowledgementNbr = 2;
//
//		Segment seg7 = new Segment();
//		seg7.acknowledgementNbr = 2;
//
//		Segment seg6 = new Segment();
//		seg6.acknowledgementNbr = 7;
//		
//		Segment seg8 = new Segment();
//		seg8.acknowledgementNbr = 8;
//		
//		
//
//		handler.onACK(DatagramChannel.open(), seg);
//		handler.onACK(DatagramChannel.open(), seg1);
//		handler.onACK(DatagramChannel.open(), seg2);
//		handler.onACK(DatagramChannel.open(), seg3);
//		handler.onACK(DatagramChannel.open(), seg4);
//		handler.onACK(DatagramChannel.open(), seg5);
//		handler.onACK(DatagramChannel.open(), seg7);
//		handler.onACK(DatagramChannel.open(), seg6);
//		handler.onACK(DatagramChannel.open(), seg8);
//	}

	/**
	 * When SYN_ACK is receive from server.
	 * 
	 * @param channel
	 * @param seg
	 * @throws IOException
	 */
	private void onSYN_ACK(DatagramChannel channel, Segment seg) throws IOException {
		// Create a ACK Packet for response.
		Segment s1 = Segment.createACK(seg);
		byte[] bytes = Segment.serializeToBytes(s1);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		queue.add(buffer);
		Queue<ByteBuffer> fileQueue = Utils.getSegments(filename, channel, BYTE_COUNT);
		for (ByteBuffer b : fileQueue) {
			this.itemQueue.add(b);
		}

		// Create a data packet.
		Segment segData = new Segment();
		segData.sequenceNbr = s1.sequenceNbr;
		segData.flag = Flags.DATA;
		segData.data = itemQueue.get(0).array();
		byte dataBytes[] = Segment.serializeToBytes(segData);
		ByteBuffer dataBuffer = ByteBuffer.wrap(dataBytes);

		// Add both to the sending queue.
		queue.add(dataBuffer);
		pendingData.put(channel, queue);
	}
}
