import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HTTPResponse {

	// fields 
	private Socket clientSocket;
	private HTTPRequest clientRequest;
	private DataOutputStream outputStream;
	private HashMap<String, String> headersValues;
	public static HashMap<String, String> parametersFromOurHTML = new HashMap<String, String>();
	public int httpCode;
	
	
	//Each cell holds a key(name of the link) and a value(content of the page)
	public static HashMap<String, StringBuilder> links = new HashMap<String, StringBuilder>();

	public HTTPResponse(int httpCode, Socket socket) throws IOException {
		this.clientRequest = null;
		this.clientSocket = socket;
		this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
		this.headersValues = new HashMap<String, String>();

		
		
		switch (httpCode) {
		case 400:
			sendServerMessage(ServerUtils.MESSAGE_BAD_REQUEST, ServerUtils.BAD_REQUEST_HTML, true);
			break;
		case 501:
			sendServerMessage(ServerUtils.MESSAGE_NOT_IMPLEMENTED, ServerUtils.NOT_IMPLEMENTED_HTML, true);
			break;
		case 403:
			sendServerMessage(ServerUtils.MESSAGE_FORBIDDEN_ERROR, ServerUtils.FORBIDDEN_ERROR_HTML, true);
		default:
			sendServerMessage(ServerUtils.MESSAGE_INTERNAL_SERVER_ERROR, ServerUtils.INTERNAL_SERVER_ERROR_HTML, true);
			break;
		}

		printHeadersValues();
		this.outputStream.flush();
	}

	public HTTPResponse(HTTPRequest clientRequest, Socket socket) throws IOException, ServerUtils.ServerInternalError {
		this.headersValues = new HashMap<String, String>();
		this.clientRequest = clientRequest;
		this.clientSocket = socket;
		this.outputStream = new DataOutputStream(clientSocket.getOutputStream());

		if(clientRequest.method.equals(ServerUtils.HttpMethods.TRACE)) {
			sendTrace();
		} 

		else {
			sendResponse(clientRequest.method);
		}

		printHeadersValues();
	}


	private void sendResponse(ServerUtils.HttpMethods method) throws IOException, ServerUtils.ServerInternalError {
		boolean printWithBody;

		if(method.equals(ServerUtils.HttpMethods.HEAD)) {
			printWithBody = false;
		} else {
			printWithBody = true;
		}

		File file = new File(WebServer.rootDirectory + "\\" + this.clientRequest.requestedPage);

		if(this.clientRequest.requestedPage.equals("/")) {
			sendDefaultPage(printWithBody);
			return;
		}


		if(this.clientRequest.requestedPage.equals("/execResult.html") && findDomainAndCheckboxes(printWithBody)) {

			if(!WebCrawler.s_AlreadyRunning){
				WebCrawler.s_AlreadyRunning = true;
				WebCrawler webCrawler = new WebCrawler(this.clientSocket);
				try {
					webCrawler.runTheCrawler();
				} catch (Exception e) {
					// 
					System.out.println("Couldn't run the crawler");
				}
				return;
			}
		}

		if(file.exists()) {

			String pathExtension = this.clientRequest.requestedPage.substring(this.clientRequest.requestedPage.indexOf(".") + 1);

			if(checkIfType(pathExtension, WebCrawler.s_ImageExtensions)) {
				sendFile(file, ServerUtils.contentType.image, pathExtension, printWithBody);
			} else if(pathExtension.equals("html")) {
				sendFile(file, ServerUtils.contentType.html, "", printWithBody);
			} else if(pathExtension.equals("ico")) {
				sendFile(file, ServerUtils.contentType.icon, "", printWithBody);
			}else if(checkIfType(pathExtension, WebCrawler.s_VideoExtensions)){
				sendFile(file, ServerUtils.contentType.video, pathExtension, printWithBody); 
			} else {
				sendFile(file, ServerUtils.contentType.application, "", printWithBody);
			}
		} 

		else {
			sendServerMessage(ServerUtils.MESSAGE_NOT_FOUND, ServerUtils.NOT_FOUND_HTML, true);
		}

		this.outputStream.flush();
	}

	private void sendTrace() throws IOException {
		StringBuilder message = new StringBuilder();

		message.append(this.clientRequest.headerFirstLine).append(System.lineSeparator());
		for(Entry<String, String> entry : this.clientRequest.headerValues.entrySet()) {
			message.append(entry.getKey() + ":" + entry.getValue()).append(System.lineSeparator());
		}

		sendServerMessage(ServerUtils.MESSAGE_OK, message.toString(), true);
	}

	private boolean findDomainAndCheckboxes(boolean printWithBody) throws IOException {
		System.out.println("\n\n\nThis is the mesage of Post: " + clientRequest.messageOfPostMethod);
		int dummyPort = 0;
		
		//Here we want to screen the domain entered:
		Pattern p = Pattern.compile("([^&=]+)=([^&=]+)");
		Matcher m = p.matcher(clientRequest.messageOfPostMethod);
		while(m.find()) {
			parametersFromOurHTML.put(m.group(1), m.group(2));
		}

		//Here we want to make sure that domain is an actual html page:
		Pattern domainFinder = Pattern.compile
				("(?i)((www\\.([a-zA-Z-_0-9]+)\\.([^\\s\\/]+))%3A([0-9]+))|((127.0.0.1|localhost)%3A([0-9]+))");
		Matcher domainMatcher = domainFinder.matcher(parametersFromOurHTML.get("domain"));
		if(domainMatcher.find()){
			if(WebCrawler.s_AlreadyRunning){
				crawlerAlreadyRunningHtml();
				return false;
			}
			//Client didn't supply port:
			
			if(domainMatcher.group(1) == null){
				//Our server:
				WebCrawler.s_DomainLink = domainMatcher.group(7);
				try{
					WebCrawler.s_GivenPort = Integer.parseInt(domainMatcher.group(8));
					System.out.println
					("This is domain and port: " + WebCrawler.s_DomainLink + ":" + WebCrawler.s_GivenPort);
				}catch(NumberFormatException e){
					System.out.println("You didn't give us a number for port");
				}
			}else if(domainMatcher.group(6) == null){
				//Not our server:
				WebCrawler.s_DomainLink = domainMatcher.group(2);
				try{
					int num = Integer.parseInt(domainMatcher.group(5));
					WebCrawler.s_GivenPort = num;
					System.out.println
					("This is domain and port: " + WebCrawler.s_DomainLink + ":" + WebCrawler.s_GivenPort);
				}catch(NumberFormatException e){
					System.out.println("You didn't give us a number for port");
				}
			}
			
		}else{
			dummyPort = -1;
		}
		
		if(parametersFromOurHTML.containsKey("robots")){
			WebCrawler.s_DisrespectRobots = true;
		}
		
		if(parametersFromOurHTML.containsKey("log")){
			WebCrawler.s_WriteToLogs = true;
		}
		if(parametersFromOurHTML.containsKey("head")){
			WebCrawler.s_CheckLinksInHeadTag = true;
		}
		
		if(WebCrawler.s_GivenPort == -1 || dummyPort == -1){
								
			String htmlBadSubmit = "<html><body><h1>Crawler failed to start because:<br></h1>"
					+ "<h3> Usage: [Domain]:[Port] - e.g: localhost:8080 </h3><br>"
					+ "<a href=\"index.html\">Main page</a></body></html>";
			
			sendServerMessage(ServerUtils.MESSAGE_OK, htmlBadSubmit, true);
			return false;
		}else if(WebCrawler.s_GivenPort != -1 && !WebCrawler.s_AlreadyRunning){
			
			String htmlGoodSubmit = "<html><body><h1>Crawler started successfully!<br></h1><br>"
					+ "<a href=\"index.html\">Main page</a></body></html>";
			sendServerMessage(ServerUtils.MESSAGE_OK, htmlGoodSubmit, true);
			return true;
		}else{
			crawlerAlreadyRunningHtml();
			return false;
		}
	}
	
	private void crawlerAlreadyRunningHtml() throws IOException{
		String htmlAlreadySubmit = "<html><body><h1>Crawler already running<br></h1><br>"
				+ "<a href=\"index.html\">Main page</a></body></html>";
		sendServerMessage(ServerUtils.MESSAGE_OK, htmlAlreadySubmit, true);
	}
	
	private void sendDefaultPage(boolean sendWithBody) throws IOException, ServerUtils.ServerInternalError {
		File DefaultPagePath = new File(WebServer.defaultPage);

		if(DefaultPagePath.exists()) {
			sendFile(DefaultPagePath, ServerUtils.contentType.html, "", sendWithBody);
		} else {
			sendServerMessage(ServerUtils.MESSAGE_NOT_FOUND, ServerUtils.NOT_IMPLEMENTED_HTML, sendWithBody);
		}
	}

	private boolean checkIfType(String extention, String[] extensions) {

		for(String type : extensions) {
			if(type.toString().equals(extention)) {
				return true;
			}
		}

		return false;
	}


	private void writeStatusLine(String status) throws IOException {
		System.out.println("\n*** Print response headers: ***");
		
		if(!(this.clientRequest == null)) {	
			System.out.print(this.clientRequest.HTTPVersion + " " + status);
			outputStream.writeBytes(this.clientRequest.HTTPVersion + " " + status);	
		} 
		
		else {
			//Not OK-message
			System.out.print(ServerUtils.VERSION_1_1 + " " + status);
			outputStream.writeBytes(ServerUtils.VERSION_1_1 + " " + status);		
		}
	}


	private void printHeadersValues() {
		
		for(Entry<String, String> entry : this.headersValues.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
	}
	
	private void sendFile(File file, ServerUtils.contentType contentType, String pathExtention, boolean printWithBody) throws IOException, ServerUtils.ServerInternalError {

		writeStatusLine(ServerUtils.MESSAGE_OK);
		
		//Date Key:
		setDateTime();
		
		switch (contentType) {
		case html:
			this.headersValues.put("Content-type", "text/html");
			break;

		case image:
			
			this.headersValues.put("Content-type", "image/" + pathExtention);
			break;

		case icon:
			this.headersValues.put("Content-type", "icon");
			break;
		case video:
			this.headersValues.put("Content-type", "video/" + pathExtention);
			break;
		default:
			this.headersValues.put("Content-type", "application/octet-stream");
			break;
		}
		
		if(!(this.clientRequest.headerValues.containsKey("chunked") && this.clientRequest.headerValues.get("chunked").equals("yes"))) {
			headersValues.put("Content-Length", String.valueOf(file.length()));			
		}

		writeHeadersValues();

		if(printWithBody) {
			printBody(file);
		}
	}


	private void printBody(File fileToPrint) throws FileNotFoundException, IOException, ServerUtils.ServerInternalError {

		try {

			byte[] fileAsByteArray = readFile(fileToPrint);

			// get the demand to chunk- "yes" or "no"
			String chunkDemand = this.clientRequest.headerValues.get("chunked");

			if(chunkDemand != null && chunkDemand.equals("yes")) {

				for(int i = 0; i < fileAsByteArray.length; i += ServerUtils.CHUNK_PACKAGE_SIZE) {
					if(fileAsByteArray.length - i < ServerUtils.CHUNK_PACKAGE_SIZE) {
						this.outputStream.writeBytes(Integer.toHexString(fileAsByteArray.length - i) + ServerUtils.CRLF);
						this.outputStream.write(fileAsByteArray, i, fileAsByteArray.length - i);
						this.outputStream.writeBytes(ServerUtils.CRLF);
					} 

					else {
						this.outputStream.writeBytes(Integer.toHexString(ServerUtils.CHUNK_PACKAGE_SIZE) + ServerUtils.CRLF);
						this.outputStream.write(fileAsByteArray, i, ServerUtils.CHUNK_PACKAGE_SIZE);
						this.outputStream.writeBytes(ServerUtils.CRLF);
					}
				}

				this.outputStream.writeBytes("0");
				this.outputStream.writeBytes(ServerUtils.CRLF);
				this.outputStream.writeBytes(ServerUtils.CRLF);			} 

			else {
				this.outputStream.write(fileAsByteArray, 0, fileAsByteArray.length);
			}
		}

		catch(FileNotFoundException e)
		{
			throw new ServerUtils.ServerInternalError();
		}
		catch(IOException e)
		{
			throw new ServerUtils.ServerInternalError();
		}
	}


	private byte[] readFile(File file) throws FileNotFoundException, IOException	{

		FileInputStream fis = new FileInputStream(file);
		byte[] bFile = new byte[(int)file.length()];
		// read until the end of the stream.
		while(fis.available() != 0)
		{
			fis.read(bFile, 0, bFile.length);
		}

		fis.close();
		return bFile;
	}


	private void writeHeadersValues() throws IOException {
		String key;
		String value;

		for(Entry<String, String> entry : this.headersValues.entrySet()) {
			key = entry.getKey();
			value = entry.getValue();

			this.outputStream.writeBytes(key + ": " + value + ServerUtils.CRLF);
		}
		this.outputStream.writeBytes(ServerUtils.CRLF);
	}

	private void sendServerMessage(String responseCode, String htmlMessage, boolean printWithBody) throws IOException {

		writeStatusLine(responseCode);
		if(printWithBody) {
				this.headersValues.put("Content-type", "text/html");
				this.headersValues.put("Content-Length", String.valueOf(htmlMessage.getBytes().length));
				writeHeadersValues();	
				this.outputStream.writeBytes(htmlMessage);
		}else{
			
			this.headersValues.put("Content-type", "text/html");
			this.headersValues.put("Content-Length", String.valueOf(htmlMessage.getBytes().length));
			writeHeadersValues();
		}
	}
	
	private void setDateTime(){
		Date currentTime = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.headersValues.put("Date", sdf.format(currentTime));
	}

}
