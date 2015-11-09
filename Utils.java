import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Utils {

	static int SSTHRESH = 5000;
	static int CWND = 1024;
	static int SND_UNA = 2;
	static int SND_WIND = 6;
	static int SND_CWND = CWND / 1024;
	final static int BYTE_COUNT = 1024;

	public static Queue<ByteBuffer> getSegments(String filename, DatagramChannel channel, final int BYTE_COUNT)
			throws IOException {
		RandomAccessFile aFile = new RandomAccessFile(filename, "rw");
		FileChannel fChannel = aFile.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(BYTE_COUNT);
		Queue<ByteBuffer> queue = new ArrayDeque<>();
		int bytesRead = 0;
		while ((bytesRead = fChannel.read(buffer)) != -1) {
			if (buffer.position() == BYTE_COUNT) {
				buffer.flip();
				byte[] bytes = new byte[BYTE_COUNT];
				buffer.get(bytes, 0, BYTE_COUNT);
				ByteBuffer newBuffer = ByteBuffer.wrap(bytes);
				queue.add(newBuffer);
				buffer.clear();
			}
		}
		fChannel.close();
		aFile.close();
		return queue;
	}

	public static void writeToFile(String filename, Map<Integer, ByteBuffer> data) throws IOException {
		FileChannel fos = new FileOutputStream(filename).getChannel();
		System.out.println(data.size());
		for (int i = 0; i < data.size(); i++) {
			ByteBuffer buffer = data.get(i);
			fos.write(buffer);
		}
		fos.close();
		System.exit(0);
	}

	public static boolean isDupCA(Segment seg, Map<Integer, Integer> ackMap, List<ByteBuffer> itemQueue) {
		if (!ackMap.containsKey(seg.acknowledgementNbr)) {
			CWND += (BYTE_COUNT * BYTE_COUNT) / CWND;
			return false;
		} else {
			int dupAck = ackMap.get(seg.acknowledgementNbr);
			if (dupAck == 3) {
				SSTHRESH = CWND / 2;
				CWND = SSTHRESH + 3 * 1024;
				ClientReadHandler.state = State.RETRANSMIT;
			} else {
				ackMap.put(seg.acknowledgementNbr, ++dupAck);
			}
		}

		return true;
	}

	public static boolean isDupAck(Segment seg, Map<Integer, Integer> ackMap, List<ByteBuffer> itemQueue) {
		if (!ackMap.containsKey(seg.acknowledgementNbr)) {
			ackMap.put(seg.acknowledgementNbr, 1);
			SND_CWND = CWND / 1024;
			CWND += BYTE_COUNT;
			SND_CWND = CWND / 1024;
			SND_UNA++;
			SND_WIND = SND_UNA + SND_CWND > itemQueue.size() ? itemQueue.size() : (SND_UNA + SND_CWND);
			return false;
		} else {
			int dupAck = ackMap.get(seg.acknowledgementNbr);
			if (dupAck == 3) {
				// Enter Fast Recovery
				SSTHRESH = CWND / 2;
				CWND = SSTHRESH + 3 * 1024;
				ClientReadHandler.state = State.RETRANSMIT;
				return true;
			} else {
				System.out.println("Dup Ack");
				ackMap.put(seg.acknowledgementNbr, ++dupAck);
				return true;
			}
		}

	}

	public static boolean isEnd(List<ByteBuffer> itemQueue) {
		if (SND_WIND == itemQueue.size() - 1) {
			return true;
		} else {
			return false;
		}
	}

	public static int incrementCWND(int cwnd) {
		return cwnd + cwnd;
	}

	public static int numberOfPackets(int cwnd) {
		return cwnd % 1024;
	}

	public static boolean isDupAckFastReceover(Segment seg, Map<Integer, Integer> ackMap, List<ByteBuffer> itemQueue) {
		if (ackMap.containsKey(seg.acknowledgementNbr)) {
			CWND += 1024;
			SND_CWND = CWND / 1024;
			SND_UNA++;
			SND_WIND = SND_UNA + SND_CWND > itemQueue.size() ? itemQueue.size() : (SND_UNA + SND_CWND);
			return false;
		}
		ClientReadHandler.state = State.CON_AVD;
		return true;
	}

}
