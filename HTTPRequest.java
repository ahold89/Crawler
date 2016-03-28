
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HTTPRequest {

	// fields
	public ServerUtils.HttpMethods method;
	public String requestedPage;
	public String HTTPVersion;
	public HashMap<String, String> parametersFromRequest;
	public HashMap<String, String> headerValues;
	
	public String headerFirstLine;
	public String messageOfPostMethod;
	
	public HTTPRequest(Socket socket) throws IOException, ServerUtils.ServerParseError, ServerUtils.ServerNotImplementedError, ServerUtils.ServerForbiddenError {
		
		this.headerValues = new HashMap<String, String>();
		this.parametersFromRequest = new HashMap<String, String>();
		InputStreamReader isr = new InputStreamReader(socket.getInputStream());
		BufferedReader clientRequestReader = new BufferedReader(isr);
		
		this.headerFirstLine = clientRequestReader.readLine();
		parseRequestFirstLine();
		parseHeaders(clientRequestReader);
		
		// if the method is post- get the post message. if not- make it empty
		if(this.method == ServerUtils.HttpMethods.POST) {
			initializeMessageOfPost(clientRequestReader);
		} 
		
		else {
			this.messageOfPostMethod = "";
		}
		
		printTheRequest();
	}
	
	private void initializeMessageOfPost(BufferedReader clientRequestReader) throws IOException, ServerUtils.ServerParseError {
	
		try {
			int contentLength = Integer.parseInt(headerValues.get(ServerUtils.CONTENT_LENGTH));
			char[] messageFromPost = new char[contentLength];
			clientRequestReader.read(messageFromPost);
			messageOfPostMethod = new String(messageFromPost);
			getParameters(messageOfPostMethod);
		} catch (NumberFormatException e) {
			throw new ServerUtils.ServerParseError();
		}
	}
	
	private void printTheRequest() {
		
		System.out.println("\n" + this.headerFirstLine);
		for(Entry<String, String> entry : this.headerValues.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}	
	}
	
	private void parseHeaders(BufferedReader requestReader) throws IOException, ServerUtils.ServerParseError {
		
		String currLine;
		
		while((currLine = requestReader.readLine()) != null) {
			
			if(currLine.equals("")) {
				break;
			} 
	
			String[] splittedCurrLine;
			
			if(currLine.split(": ").length == 2) {
				splittedCurrLine = currLine.split(": ");
				this.headerValues.put(splittedCurrLine[0], splittedCurrLine[1]);
			} 
			
			else {
				throw new ServerUtils.ServerParseError();
			}
		}
	}
	
	private void parseRequestFirstLine() throws ServerUtils.ServerParseError, ServerUtils.ServerNotImplementedError, ServerUtils.ServerForbiddenError {

		//If the request tries to get out the serverroot We are causing a 404 Not Found on purpose!
		if(checkIfPageIsOutOfTheRoot(this.headerFirstLine)) {
			this.headerFirstLine = this.headerFirstLine.replaceAll("/../", "./"); 
		}
		Pattern valuesFinder = Pattern.compile("(?i)([a-zA-Z]+) (.+) (HTTP\\/(1.1|1.0))");
		Matcher valuesMatcher = valuesFinder.matcher(this.headerFirstLine);

		if(valuesMatcher.find()){
			if(valuesMatcher.group(1) != null && 
					valuesMatcher.group(2) != null && valuesMatcher.group(3) != null){

				String[] requestFirstLineVals = 
					{valuesMatcher.group(1), valuesMatcher.group(2).trim(), valuesMatcher.group(3)};

				method = ServerUtils.HttpMethods.valueOf(requestFirstLineVals[0]);
				
				if(requestFirstLineVals[2].equals(ServerUtils.VERSION_1_0) || requestFirstLineVals[2].equals(ServerUtils.VERSION_1_1)) {
					this.HTTPVersion = requestFirstLineVals[2];
				}else{
					throw new ServerUtils.ServerParseError();
				}

				// split by the character "?"
				String[] pageAndParameters = requestFirstLineVals[1].split("\\?");

				if(checkIfPageIsOutOfTheRoot(pageAndParameters[0])) {
					throw new ServerUtils.ServerParseError();
				}else {

					if(pageAndParameters.length == 1) {
						this.requestedPage = pageAndParameters[0];
						// needed in order to find if the user ask for the page Directly
						Pattern p = Pattern.compile
								(".*?(_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]_[0-9][0-9]-[0-9][0-9]-[0-9][0-9]\\.html)");
						Matcher m = p.matcher(requestedPage);
						if(m.find() && m.group(1) != null) {
							throw new ServerUtils.ServerForbiddenError();		
						}
					}else if(pageAndParameters.length == 2) {
						this.requestedPage = pageAndParameters[0];
						getParameters(pageAndParameters[1]);
					}else {
						throw new ServerUtils.ServerParseError();
					}	
				}
			}else{
				throw new ServerUtils.ServerParseError();
			}
		}else{
			throw new ServerUtils.ServerParseError();
		}


	}
	
	private boolean checkIfPageIsOutOfTheRoot(String path) { 
		String outOfTheRootPath = "/../";
		
		if(path.contains(outOfTheRootPath)) {
			return true;
		} 
		
		else {
			return false;	
		}
	}
	
	private void getParameters(String parameters) {
		Pattern pattern = Pattern.compile("([^&=]+)=([^&=]+)");
		Matcher matcher = pattern.matcher(parameters);
		
		while(matcher.find()) {
			parametersFromRequest.put(matcher.group(1), matcher.group(2));
		}
	}
}
