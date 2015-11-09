import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class Connection {
	
	private DatagramChannel channel;
	private SocketAddress addr;
	private Selector selector;
	
	// TODO: put this in configuration file
	private final static String SERVER_ADDR = "localhost";
	private final static int PORT_NUM = 10000;
	
	
	public Connection(){
		
	}
	
	
	
	/**
	 * @return the channel
	 */
	public DatagramChannel getChannel() {
		return channel;
	}



	/**
	 * @param channel the channel to set
	 */
	public void setChannel(DatagramChannel channel) {
		this.channel = channel;
	}



	/**
	 * @return the addr
	 */
	public SocketAddress getAddr() {
		return addr;
	}



	/**
	 * @param addr the addr to set
	 */
	public void setAddr(SocketAddress addr) {
		this.addr = addr;
	}



	/**
	 * @return the selector
	 */
	public Selector getSelector() {
		return selector;
	}



	/**
	 * @param selector the selector to set
	 */
	public void setSelector(Selector selector) {
		this.selector = selector;
	}



	public Connection(DatagramChannel channel, SocketAddress addr, Selector selector) {
		super();
		this.channel = channel;
		this.addr = addr;
		this.selector = selector;
	}

	public int sendRequest(DatagramChannel channel, ByteBuffer buffer) throws IOException {
		int numberOfBytes = channel.send(buffer, addr);
		return numberOfBytes;
	}


	public DatagramChannel openConnection(){
		try {
			channel = DatagramChannel.open();
			addr = new InetSocketAddress(SERVER_ADDR, PORT_NUM);
			channel.configureBlocking(false);
			selector = Selector.open();
			SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return channel;
	}

}
