import java.io.IOException;
import java.nio.channels.DatagramChannel;

public interface ClientInterface {

	public void performHandShake() throws IOException;

	public void sendFin();
}
