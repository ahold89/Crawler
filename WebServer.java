
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



public class WebServer {

	// fields
	public static int s_Port;
	public static int maxThreads;
	public static String rootDirectory; 
	public static String defaultPage;
	public ThreadPool threadPool;
	ServerSocket serverSocket;

	
	
	public WebServer() throws IOException, NumberFormatException {
		readConfigFile(ServerUtils.CONFIGF_FILE_PATH);
		threadPool = new ThreadPool(maxThreads);
	}
	
	
	private void runTheServer() throws IOException {
		serverSocket = new ServerSocket(s_Port);
		System.out.println("The Server is activated.\nListening to port: " + s_Port);

		while(true) {
			Socket clientConnection = serverSocket.accept();
			clientConnection.setSoTimeout(1000);
			TaskFromClientThread clientTask = new TaskFromClientThread(clientConnection);
			try {
				threadPool.execute(clientTask);
			} catch (Exception e) {
				System.out.println("Thread Pool has stopped working:"
						+ " threads were interupted from waiting in line for queue of tasks."
						+ "\n please re-connect to our server");
//				threadPool.stopOurThreadPool();
			}
		}

	}
	
	public static void main(String[] args) {
		try {
			
			WebServer webServer = new WebServer();
			webServer.runTheServer();

		} catch(Exception e) {
			System.out.println("Server has to turn off");
			System.exit(0);
		}
	}

	private void readConfigFile(String i_ConfigFilePath) throws IOException, NumberFormatException {

		File configFile = new File(i_ConfigFilePath);

		if(configFile.exists()) {
			FileReader fr = new FileReader(configFile);
			BufferedReader br = new BufferedReader(fr);
			String currLine;
			String key, Value;
			String tempImExtensions, tempVidExtensions, tempDocExtensions;

			while((currLine = br.readLine()) != null) {
				key = currLine.split("=")[0];
				Value = currLine.split("=")[1];

				switch (key) {
				case "root":
					rootDirectory = Value.trim();
					break;
				case "defaultPage":
					defaultPage = rootDirectory + Value.trim(); 
					break;
				case "port":
					s_Port = Integer.parseInt(Value.trim());
					break;
				case "maxThreads":
					maxThreads = Integer.parseInt(Value.trim());
				case "maxDownloaders":
					WebCrawler.s_MaxDownloaders = Integer.parseInt(Value.trim());
					break;
				case "maxAnalyzers":
					WebCrawler.s_MaxAnalyzers = Integer.parseInt(Value.trim());
					break;
				case "imageExtensions":
					tempImExtensions = Value.trim();
					WebCrawler.s_ImageExtensions = configFileHelper(tempImExtensions);
					break;
				case "videoExtensions":
					tempVidExtensions = Value.trim();
					WebCrawler.s_VideoExtensions = configFileHelper(tempVidExtensions);
					break;
				case "documentExtensions":
					tempDocExtensions = Value.trim();
					WebCrawler.s_DocExtensions = configFileHelper(tempDocExtensions);
//					String docHelper = "(";
//					for(String docExt : WebCrawler.s_DocExtensions){
//						docHelper += docExt + "|";
//					}
//					WebCrawler.s_DocExtensionsPattern = docHelper.substring(0 , docHelper.length() - 1) + ")";
					break;
				default:
					break;
				}
			}
			br.close();

		} else {
			System.out.println("The config.ini file doesn't exist. this is curr dir: " + System.getProperty("user.dir"));
		}
	}

	private static String[] configFileHelper(String i_ValueList){
		String noQuotes = i_ValueList.substring(1, i_ValueList.length() - 1);
		String[] extensions = noQuotes.split(",");

		for(int i = 0; i < extensions.length; i++){
			extensions[i] = extensions[i].trim();
		}

		return extensions;
	}	

}
