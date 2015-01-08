import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Nathan on 2015/1/7.
 */
public class Test {
	public static void main(String args[]) throws Exception {
		EasyServer easyServer = EasyServer.getInstance();
		easyServer.setUpPool(0,4).addPort(9000, new Handler() {
			@Override
			public String handle(String request, Socket socket) {
				System.out.println(request);
				String response = "Got it!";
				return response;
			}
		}).addPort(10000, new Handler() {
			@Override
			public String handle(String request, Socket socket) {
				System.out.println(request);
				String response = "10000 received";
				return response;
			}
		}).startAll();

		Map<String,String> map = new HashMap<String, String>();
		map = easyServer.showStatus();
		Iterator iterator = map.entrySet().iterator();
		while (iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			System.out.println("Port<" + (String)entry.getKey() + "> " + (String)entry.getValue());
		}
	}
}
