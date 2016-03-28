import java.io.IOException;
import java.net.Socket;


public class TaskFromClientThread implements Runnable{

	private Socket clientConnection;
	
	public TaskFromClientThread(Socket clientConnection) {
		this.clientConnection = clientConnection;
	}

	@Override
	public void run() {
		
		System.out.println("Start a new connection.\n");
		
		boolean toContinue = true;
		HTTPRequest request = null;
		@SuppressWarnings("unused")
		HTTPResponse response = null;
		
		while (toContinue) {		
			try {			
				try {
					request = new HTTPRequest(clientConnection);
					response = new HTTPResponse(request, this.clientConnection);

					// Check connection
					try {
						if (request.HTTPVersion.equals("HTTP/1.1") 
								|| request.HTTPVersion.equals("HTTP/1.0") 
								&& (!request.headerValues.get("Connection").equalsIgnoreCase("keep-alive"))) {
							toContinue = false;
						}
					} catch (Exception e) {
						toContinue = false;
					}
					
				} catch (ServerUtils.ServerNotImplementedError e) {
					response = new HTTPResponse(501, this.clientConnection);
				} catch (ServerUtils.ServerParseError e) {
					System.out.println("Bad request!");
					response = new HTTPResponse(400, this.clientConnection);
				} catch (ServerUtils.ServerInternalError | NullPointerException e) {
					response = new HTTPResponse(500, this.clientConnection);
				}catch (ServerUtils.ServerForbiddenError e){
					response = new HTTPResponse(403, this.clientConnection);
				}
				
			} catch (IOException | NullPointerException e) {
				toContinue = false;
			}
		}
		
		// Close connection
		System.out.println("\nTaskFromClient: Connection closed.");
		System.out.println(System.lineSeparator());
		try {
			clientConnection.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
