import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class WriteHandler implements Handler<SelectionKey> {

	private final Map<DatagramChannel, Queue<ByteBuffer>> pendingData;
	private final static Map<Integer,ByteBuffer> bufferedData = new HashMap<>();
	
	public WriteHandler(Map<DatagramChannel, Queue<ByteBuffer>> pendingData) {
		this.pendingData = pendingData;
	}
	
	public WriteHandler() {
		this.pendingData = new HashMap<>();
	}

	private static List<Integer> segmentsReceived = new ArrayList<>();
	private static int highestSeq = Integer.MIN_VALUE;
	
	@Override
	public void handle(SelectionKey key) throws IOException {
		DatagramChannel ch = (DatagramChannel) key.channel();
		ch.configureBlocking(false);
		SocketAddress addr = (SocketAddress) key.attachment();
		Queue<ByteBuffer> queue = pendingData.get(ch);
		int size = queue.size();
		for (int i = 0; i < size; i++) {
			ByteBuffer bf = queue.poll();
			if (bf != null) {
				try {
					Segment seg = Segment.deserializeBytes(bf);

					switch (seg.flag) {

					case SYN:
						onSYN(ch, addr, seg);
						break;
					case ACK:
						System.out.println("Final ACK: " + seg.acknowledgementNbr);
						System.out.println("Final SYN: " + seg.sequenceNbr);
						break;
					case FIN:
						Segment segment = new Segment();
						segment.flag = Flags.FIN;
						byte [] bytes = Segment.serializeToBytes(segment);
						ByteBuffer buffer = ByteBuffer.wrap(bytes);
						ch.send(buffer, addr);
						break;
					case DATA:
						onData(ch, addr, seg);
						break;
					case FIN_ACK:
						Segment segment1 = new Segment();
						segment1.flag = Flags.FIN_ACK;
						byte [] byt = Segment.serializeToBytes(segment1);
						ByteBuffer buff = ByteBuffer.wrap(byt);
						ch.send(buff, addr);
						Utils.writeToFile("name.txt", bufferedData);
					default:
						break;

					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		key.interestOps(SelectionKey.OP_READ);
	}
	
	private static int nextAck(){
		Collections.sort(segmentsReceived);
		for(int i=0; i< segmentsReceived.size()-1;i++){
			int n1 = segmentsReceived.get(i);
			int n2 = segmentsReceived.get(i+1);
			if(n2 - n1 == 2){
				return n1;
			}
		}
		return segmentsReceived.get(segmentsReceived.size() -1);
	}
	
	private static int RCV_NXT=2;
	
	private void onData(DatagramChannel ch, SocketAddress addr, Segment seg) throws IOException {
		
		segmentsReceived.add(seg.sequenceNbr);
		highestSeq = Math.max(seg.sequenceNbr, highestSeq);
		int nextAck = WriteHandler.nextAck() + 1;
		
		bufferedData.put(seg.sequenceNbr-2, ByteBuffer.wrap(seg.data));
		int seqNbr=0;
		if(RCV_NXT ==2 && seg.sequenceNbr!=2){
			seqNbr = RCV_NXT;
		}
		else if(nextAck < seg.sequenceNbr){
			RCV_NXT = nextAck;
			seqNbr = RCV_NXT;
		}
		else if(nextAck == seg.sequenceNbr){
			RCV_NXT++;
			seqNbr = RCV_NXT;
		}else{		
			RCV_NXT = nextAck;
			seqNbr = RCV_NXT;
		}
		Segment segment = Segment.createACK(seqNbr, seg);
		byte[] bytes = Segment.serializeToBytes(segment);
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		ch.send(buf, addr);
		System.out.println("Send Acknowledgement: " +segment.acknowledgementNbr);
	}
	
	

	private void onSYN(DatagramChannel channel, SocketAddress addr, Segment seg) throws IOException {
		
		Segment segment = Segment.initialSYNIncrement(seg);
		byte[] bytes = Segment.serializeToBytes(segment);
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		channel.send(buf, addr);
	}

}
