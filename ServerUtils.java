

public class ServerUtils {

	
	public static enum HttpMethods {
		GET,
		POST,
		HEAD,
		TRACE
	}
	
	public static enum contentType {
		html,
		image,
		icon,
		video,
		application
	}
	
	public static int CHUNK_PACKAGE_SIZE = 150;
	public static String CONTENT_LENGTH = "Content-Length";
	public static String CONFIGF_FILE_PATH = System.getProperty("user.dir") + "//config.ini";
	public static String CRLF = "\r\n";
	public static String VERSION_1_0 = "HTTP/1.0";
	public static String VERSION_1_1 = "HTTP/1.1";
	public static String MESSAGE_OK = "200 OK" + CRLF;
	public static String MESSAGE_FORBIDDEN_ERROR = "403 Forbidden" + CRLF;
	public static String MESSAGE_NOT_FOUND = "404 Not Found" + CRLF;
	public static String MESSAGE_NOT_IMPLEMENTED = "501 Not Implemented" + CRLF;
	public static String MESSAGE_BAD_REQUEST = "400 Bad Request" + CRLF;
	public static String MESSAGE_INTERNAL_SERVER_ERROR = "500 Internal Server Error" + CRLF;
	
	public static String NOT_FOUND_HTML = "<HTML><BODY><H1> 404 Not Found </H1></BODY></HTML>" + CRLF;
	public static String NOT_IMPLEMENTED_HTML = "<HTML><BODY><H1> 501 Not Implemented </H1></BODY></HTML>" + CRLF;
	public static String BAD_REQUEST_HTML = "<HTML><BODY><H1> 400 Bad Request </H1></BODY></HTML>" + CRLF;
	public static String INTERNAL_SERVER_ERROR_HTML = "<HTML><BODY><H1> 500 Internal Server Error </H1></BODY></HTML>" + CRLF;
	public static String FORBIDDEN_ERROR_HTML = "<HTML><BODY><H1> 403 Forbidden </H1></BODY></HTML>" + CRLF;
	
	@SuppressWarnings("serial")
	public static class ServerInternalError extends Exception {
		public ServerInternalError() {};
	}
	
	@SuppressWarnings("serial")
	public static class ServerNotImplementedError extends Exception {
		public ServerNotImplementedError() {};
	}
	
	@SuppressWarnings("serial")
	public static class ServerParseError extends Exception {
		public ServerParseError() {};
	}
	
	@SuppressWarnings("serial")
	public static class ServerForbiddenError extends Exception {
		public ServerForbiddenError() {};
	}
}
