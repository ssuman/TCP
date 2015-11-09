import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class FastRecoveryHandler {
	
	private Segment seg;
	Map<DatagramChannel, Queue<ByteBuffer>> pendingData;
	List<ByteBuffer> itemQueue;
	DatagramChannel channel;
	
	public FastRecoveryHandler(DatagramChannel channel,Segment seg, 
			Map<DatagramChannel, Queue<ByteBuffer>> pendingData, List<ByteBuffer> itemQueue){
		this.seg = seg;
		this.pendingData = pendingData;
		this.itemQueue = itemQueue;
		this.channel = channel;
	}
	
	public void handler() throws IOException{
		System.out.println("In Fast Recovery..");
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		Segment segment = new Segment();
		segment.sequenceNbr = seg.acknowledgementNbr;
		segment.data = itemQueue.get(seg.acknowledgementNbr).array();
		byte[] bytes = Segment.serializeToBytes(segment);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		queue.add(buffer);
		pendingData.put(channel, queue);
	}

}
