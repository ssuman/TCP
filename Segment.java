import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Segment implements Serializable{

	int sourcePort;
	int destPort;
	String sourceIPAddr;
	String destIPAddr;
	int sequenceNbr;
	int acknowledgementNbr;
	int dataOffset;
	Flags flag;
	int windowSize;
	int checksum;
	byte[] data = new byte[1024];
	
	public Segment() {
		
	}
	
	public Segment(int sourcePort, int destPort, String sourceIPAddr, String destIPAddr, int sequenceNbr,
			int acknowledgementNbr, int dataOffset, Flags flag, int windowSize, int checksum, byte[] data) {
		super();
		this.sourcePort = sourcePort;
		this.destPort = destPort;
		this.sourceIPAddr = sourceIPAddr;
		this.destIPAddr = destIPAddr;
		this.sequenceNbr = sequenceNbr;
		this.acknowledgementNbr = acknowledgementNbr;
		this.dataOffset = dataOffset;
		this.flag = flag;
		this.windowSize = windowSize;
		this.checksum = checksum;
		this.data = data;
	}
	
	
	public static byte[] serializeToBytes(Segment seg) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try{
			out  = new ObjectOutputStream(bos);
			out.writeObject(seg);
			out.flush();
			
		} finally {
			out.close();
			bos.close();
		}
		return bos.toByteArray();
	}
	
	public static Segment deserializeBytes(ByteBuffer buffer) throws IOException, ClassNotFoundException{
		
		buffer.flip();
		byte [] bytes  = new byte[buffer.limit()];
		buffer.get(bytes);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInputStream in = null;
		Segment seg = null;
		try{
			in = new ObjectInputStream(bis);
			seg = (Segment) in.readObject();
		}finally{
			in.close();
			bis.close();
		}
		return seg;	
	}
	
	public static Segment initialACK(){
		Segment segment = new Segment();
		segment.flag = Flags.SYN;
		//segment.sequenceNbr = (long)(Math.random() * 10000);
		segment.sequenceNbr = 1;
		return segment;
	}
	
	public static Segment createACK(Segment seg) {
		
		Segment segment = new Segment();
		segment.acknowledgementNbr = seg.sequenceNbr +1 ;
		segment.sequenceNbr = seg.acknowledgementNbr;
		segment.flag = Flags.ACK;
		segment.data = null;
		System.out.println("Received ACK: "+seg.acknowledgementNbr +" Sequence Nbr: "+seg.sequenceNbr);
		System.out.println("Sent ACK: " + segment.acknowledgementNbr+" Sent Nbr: "+ segment.sequenceNbr);
		return segment;
	}
	
	
	public static Segment initialSYNIncrement(Segment seg){
		Segment segment = new Segment();
		segment.flag = Flags.SYN_ACK;
		//segment.sequenceNbr = (long)(Math.random() * 10000);
		segment.sequenceNbr = 100;
		segment.acknowledgementNbr = (seg.sequenceNbr + 1);
		return segment;
	}

	public static Segment createDataPacket(Segment seg, ByteBuffer buff) {
		Segment segment = new Segment();
		segment.acknowledgementNbr = seg.sequenceNbr;
		segment.sequenceNbr = seg.acknowledgementNbr;
		buff.flip();
		segment.data = buff.array();
		segment.flag = Flags.DATA;
		return segment;
		
	}
	
	public static Segment sendFINACK(){
		Segment segment = new Segment();
		segment.flag = Flags.FIN_ACK;
		return segment;
	}

	public static Segment createACK(int expectedSeq, Segment seg) {
		Segment segment = new Segment();
		segment.acknowledgementNbr = expectedSeq;
		segment.sequenceNbr = seg.acknowledgementNbr;
		segment.flag = Flags.ACK;
		//System.out.println("Received ACK: "+seg.acknowledgementNbr +" Sequence Nbr: "+seg.sequenceNbr);
		//System.out.println("Sent ACK: " + segment.acknowledgementNbr+" Sent Nbr: "+ segment.sequenceNbr);
		return segment;
	}
	
}
