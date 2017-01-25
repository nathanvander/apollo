package apollo.iface;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class ConnectionHandle extends PointerType {
	public ConnectionHandle(Pointer p) {
		super(p);
	}
}
