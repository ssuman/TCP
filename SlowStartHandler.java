import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class SlowStartHandler {

	DatagramChannel channel;
	Segment seg;
	List<ByteBuffer> itemQueue;
	Map<DatagramChannel, Queue<ByteBuffer>> pendingData;

	public SlowStartHandler(DatagramChannel channel, Segment seg, Map<DatagramChannel, Queue<ByteBuffer>> pendingData,
			List<ByteBuffer> itemQueue) {
		this.channel = channel;
		this.seg = seg;
		this.pendingData = pendingData;
		this.itemQueue = itemQueue;
	}

	public void handler() throws IOException {
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		if (Utils.CWND < Utils.SSTHRESH) {
			for (int i = Utils.SND_UNA; i < Utils.SND_WIND; i++) {
				System.out.println("Send UnAcknowledged: "+ Utils.SND_UNA);
				System.out.println("Send Window Size: " + Utils.SND_WIND);
				if (!UnAckedMap.unAckMap.containsKey(i)) {
					Segment segment = new Segment();
					segment.acknowledgementNbr = 0;
					segment.sequenceNbr = i;
					ByteBuffer buffer = itemQueue.get(i);
					buffer.flip();
					segment.data = buffer.array();
					segment.flag = Flags.DATA;
					System.out.println("Sending Seqment: " + segment.sequenceNbr);
					byte[] bytes = Segment.serializeToBytes(segment);
					buffer.clear();
					buffer = ByteBuffer.wrap(bytes);
					queue.add(buffer);
				}
				UnAckedMap.unAckMap.put(i, i);
			}
			if(Utils.isEnd(itemQueue)) {
				 System.out.println("Sending FIN segment");
				 Segment segment = new Segment();
				 segment.flag = Flags.FIN;
				 byte bytes []  = Segment.serializeToBytes(segment);
				 ByteBuffer buffer = ByteBuffer.wrap(bytes);
				 queue.add(buffer);
			}
		}else{
			new CongestionAvoidanceHandler(seg,pendingData, itemQueue,channel).handler();
			ClientReadHandler.state = State.CON_AVD;
			return;
		}
		pendingData.put(channel, queue);
	}

}
