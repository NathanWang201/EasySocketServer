import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EasyServer is a class that controls the thread pool and all ports.For a single port,the inner class server will perform its function.
 * Created by Nathan on 2015/1/7.
 */
public class EasyServer {

	private int threadsPoolSize;//number of threads distributed to each CPU core
	private static EasyServer easyServer = null;
	private ExecutorService executorService; //thread pool
	private Map<String,Server> portMap = new HashMap<String, Server>();//stores registered ports and corresponding server instances

	private EasyServer(){
		//Assure default constructor cannot be accessed publicly
	}

	public static synchronized void initialize(){
		if(easyServer == null){
			easyServer = new EasyServer();
		}
	}

	//singleton
	public static EasyServer getInstance(){
		if(easyServer == null){
			initialize();
		}
		return easyServer;
	}

	/**
	 * set up the thread pool size
	 * @param fixedNum
	 * @param threadsPerCore
	 * @return
	 * @throws Exception
	 */
	public EasyServer setUpPool(int fixedNum,int threadsPerCore) throws Exception {
		if(fixedNum > 0){
			executorService = Executors.newFixedThreadPool(fixedNum);
			threadsPoolSize = fixedNum;
		}else if(threadsPerCore > 0){
			executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * threadsPerCore);
			threadsPoolSize = Runtime.getRuntime().availableProcessors() * threadsPerCore;
		}else{
			throw new Exception("Wrong thread number!");
		}
		return easyServer;
	}

	/**
	 * add a port to the map,this will not start listening
	 * @param port
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public EasyServer addPort(int port,Handler handler) throws Exception {
		Server server = new Server(port);
		server.setHandler(handler);
		if(portMap.containsKey(port+"")){
			throw new Exception("Port<" + port + "> Already registered!");
		}else{
			portMap.put(port+"",server);
		}
		return easyServer;
	}

	/**
	 * remove a port
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public EasyServer removePort(int port) throws Exception {
		Server server = portMap.get(port+"");
		if(server == null){
			portMap.remove(port+"");
		}else{
			throw new Exception("Port<" + port + "> in use!");
		}
		return easyServer;
	}

	/**
	 * start listening on all ports
	 * @return
	 * @throws Exception
	 */
	public EasyServer startAll() throws Exception {
		Iterator iterator = portMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			String port = (String)entry.getKey();
			Server server = (Server)entry.getValue();
			server.start();
		}
		return easyServer;
	}

	/**
	 * start listening on specific port
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public EasyServer start(String port) throws Exception {
		Server server = portMap.get(port+"");
		if(server.getServerSocket().isClosed()){
			server.start();
		}
		return easyServer;
	}

	/**
	 * stop listening on all ports
	 * @throws IOException
	 */
	public void stopAll() throws IOException {
		Iterator iterator = portMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			String port = (String)entry.getKey();
			Server server = (Server)entry.getValue();
			server.stop();
		}
	}

	/**
	 * stop listening on specific port
	 * @param port
	 * @throws IOException
	 */
	public void stop(String port) throws IOException {
		if(portMap.containsKey(port)){
			portMap.get(port).stop();
		}else{
			throw new RuntimeException("Port not registered!");
		}
	}

	public Map<String, String> showStatus()throws Exception{
		Map<String,String> map = new HashMap<String, String>();
		Iterator iterator = portMap.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = (Map.Entry)iterator.next();
			String port = (String)entry.getKey();
			Server server = (Server)entry.getValue();
			if(server!=null){
				map.put(port,"Open");
			}else {
				map.put(port,"Closed");
			}
		}
		return map;
	}

	private class Server{
		private int port;//list of port number
		private Handler handler;//Handler on this port
		private ServerSocket serverSocket; //socket server class
		private Thread thread;

		public Server(int port){
			this.port = port;
		}

		public void start() throws Exception {
			try {
				if(serverSocket!=null){
					throw new Exception("Already started!");
				}
				serverSocket = new ServerSocket(port);
				thread = new Thread(){
					@Override
					public void run(){
						System.out.println("Port<" + port + "> is listening.");

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
								executorService.execute(new ExecutionThread(requestContent,socket,handler));
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
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				};
				thread.start();
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
