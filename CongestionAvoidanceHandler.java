import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class CongestionAvoidanceHandler {

	Segment seg;
	Map<DatagramChannel, Queue<ByteBuffer>> pendingData;
	List<ByteBuffer> itemQueue;
	DatagramChannel channel;

	public CongestionAvoidanceHandler(Segment segment, Map<DatagramChannel, Queue<ByteBuffer>> pendingData,
			List<ByteBuffer> itemQueue, DatagramChannel channel) {
		this.seg = segment;
		this.pendingData = pendingData;
		this.itemQueue = itemQueue;
		this.channel = channel;
	}

	public CongestionAvoidanceHandler() {

	}

	public void handler() throws IOException {
		
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		for (int i = Utils.SND_UNA; i < Utils.SND_WIND; i++) {
			System.out.println("Send UnAcknowledged: "+ Utils.SND_UNA);
			System.out.println("Send Window Size: " + Utils.SND_WIND);
			if (!UnAckedMap.unAckMap.containsKey(i)) {
				Segment segment = new Segment();
				segment.flag = Flags.DATA;
				segment.sequenceNbr = i;
				segment.data = itemQueue.get(seg.acknowledgementNbr).array();
				System.out.println("Sending Seqment: " + segment.sequenceNbr);
				byte[] bytes = Segment.serializeToBytes(segment);
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				queue.add(buffer);
			}
			if(Utils.isEnd(itemQueue)){
				 System.out.println("Sending FIN segment");
				 Segment segment = new Segment();
				 segment.flag = Flags.FIN;
				 byte bytes []  = Segment.serializeToBytes(segment);
				 ByteBuffer buffer = ByteBuffer.wrap(bytes);
				 queue.add(buffer);
			}
			UnAckedMap.unAckMap.put(i, i);
		}

		pendingData.put(channel, queue);
	}

}
