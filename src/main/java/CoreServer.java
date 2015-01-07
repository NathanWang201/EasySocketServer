import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Nathan on 2015/1/7.
 */
public class CoreServer {

	private static int threadsPerCore;//number of threads distributed to each CPU core
	private static CoreServer coreServer = null;
	private ExecutorService executorService; //thread pool
	private Handler handler;//handler class
	private Map<String,Server> portMap = new HashMap<String, Server>();

	private CoreServer(){
		//Assure default constructor cannot be accessed publicly
	}

	//singleton
	public CoreServer getInstance(){
		if(coreServer == null){
			coreServer = new CoreServer();
		}
		return coreServer;
	}

	public CoreServer setUp(int fixedNum,int threadsPerCore) throws Exception {
		this.threadsPerCore = threadsPerCore;
		if(fixedNum > 0){
			executorService = Executors.newFixedThreadPool(fixedNum);
		}else if(threadsPerCore > 0){
			executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadsPerCore);
		}else{
			throw new Exception("Wrong thread number!");
		}
		return coreServer;
	}

	public CoreServer addPort(int port,Handler handler) throws Exception {
		Server server = new Server(port);
		server.setHandler(handler);
		if(portMap.containsKey(port+"")){
			throw new Exception("Port<" + port + "> Already registered!");
		}else{
			portMap.put(port+"",server);
		}
		return coreServer;
	}

	public CoreServer removePort(int port) throws Exception {
		Server server = portMap.get(port+"");
		if(server == null){
			portMap.remove(port+"");
		}else{
			throw new Exception("Port<" + port + "> in use!");
		}
		return coreServer;
	}

	public CoreServer startAll() throws Exception {
		Iterator iterator = portMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			String port = (String)entry.getKey();
			Server server = (Server)entry.getValue();
			server.start();
		}
		return coreServer;
	}

	public CoreServer start(String port) throws Exception {
		Server server = portMap.get(port+"");
		if(server.getServerSocket().isClosed()){
			server.start();
		}
		return coreServer;
	}

	public void stopAll() throws IOException {
		Iterator iterator = portMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			String port = (String)entry.getKey();
			Server server = (Server)entry.getValue();
			server.stop();
		}
	}

	public void stop(String port) throws IOException {
		if(portMap.containsKey(port)){
			portMap.get(port).stop();
		}else{
			throw new RuntimeException("Port not registered!");
		}
	}

	class Server{
		private int port;//list of port number
		private Handler handler;//Handler on this port
		private ServerSocket serverSocket; //socket server class
		private Thread thread;

		public Server(int port){
			this.port = port;
		}


		public void start() throws Exception {
			try {
				if(!serverSocket.isClosed()){
					throw new Exception("Already started!");
				}
				serverSocket = new ServerSocket(port);
				thread = new Thread(){
					@Override
					public void run(){
						OutputStreamWriter writer = null;
						Socket socket = null;
						BufferedReader reader = null;
						try{
							while(true){
								socket = serverSocket.accept();
								InputStream in = socket.getInputStream();
								reader = new BufferedReader(new InputStreamReader(in));
								String requestContent = "";
								char[] cbuf = new char[10000];
								reader.read(cbuf);
								requestContent = String.valueOf(cbuf).trim();
								//handle request
								String responseContent = handler.handle(requestContent,socket);
								writer.write(responseContent);
								writer.flush();
							}
						}catch (Exception e){
							e.printStackTrace();
							try {
								throw e;
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
						finally {
							try {
								reader.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							try {
								writer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				};
				thread.run();
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		}

		public void stop() throws IOException {
			serverSocket.close();
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public Handler getHandler() {
			return handler;
		}

		public void setHandler(Handler handler) {
			this.handler = handler;
		}

		public ServerSocket getServerSocket() {
			return serverSocket;
		}

		public void setServerSocket(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
		}
	}
}
