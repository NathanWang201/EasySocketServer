import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Thread used to execute handler
 * Created by Nathan on 2015/1/7.
 */
public class ExecutionThread implements Runnable{
	Socket socket;
	String request;
	Handler handler;

	public ExecutionThread(String request,Socket socket,Handler handler){
		this.socket = socket;
		this.request = request;
		this.handler = handler;
	}

	@Override
	public void run(){
		OutputStreamWriter writer = null;
		try {
			String responseContent = handler.handle(request,socket);
			writer = new OutputStreamWriter(socket.getOutputStream(),BlockingModeServer.getCharSet());
			writer.write(responseContent);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if(writer != null){
					writer.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
