import java.net.Socket;

/**
 * Created by Nathan on 2015/1/7.
 */
public interface Handler{
	/**
	 * implement this method to customize how to handle socket request
	 * @param request
	 * @param socket
	 * @return
	 */
	public String handle(String request,Socket socket);
}
