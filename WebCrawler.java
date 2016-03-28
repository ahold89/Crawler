import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebCrawler {

	public static int s_MaxDownloaders;
	public static int s_MaxAnalyzers;
	public static String[] s_ImageExtensions;
	public static String[] s_VideoExtensions;
	public static ThreadPool s_ThreadPoolOfAnalyzers;
	public static ThreadPool s_ThreadPoolOfDownloaders;
	public static String s_DomainLink;
	public static boolean s_DisrespectRobots;
	public static ArrayList<String> s_LinksVisited = new ArrayList<String>();
	public static ArrayList<String> s_WildCards = new ArrayList<String>();
	public static int s_GivenPort = -1;
	public static String[] s_DocExtensions;
	public boolean m_FullPortScan;
	public static StatisticSums s_Stats;
	public static boolean s_AlreadyRunning;
	public static Socket s_ClientSocket;
	public static AtomicInteger s_DownloadTasksWorking = new AtomicInteger(0);
	public static AtomicInteger s_AnalyzeTasksWorking = new AtomicInteger(0);

	//Bonus:
	public static boolean s_WriteToLogs;
	public static boolean s_CheckLinksInHeadTag;

	
	public WebCrawler(Socket i_ClientSocket) throws NumberFormatException, IOException{
		s_ThreadPoolOfAnalyzers = new ThreadPool(s_MaxAnalyzers);
		s_ThreadPoolOfDownloaders = new ThreadPool(s_MaxDownloaders);
		this.m_FullPortScan = HTTPResponse.parametersFromOurHTML.containsKey("scan"); 
		s_Stats = new StatisticSums();
		s_ClientSocket = i_ClientSocket;
	}


	public void runTheCrawler() throws Exception {

		//We first take care of robots.txt:
		try{
			takeCareOfRobotsLinks();
		}catch(Exception e){
			System.out.println("Couldn't read robots.txt");
		}

		TaskOfDownloadersThread domainTask;

		if(s_GivenPort != -1){ //It Works:
			synchronized(s_Stats){
				s_Stats.m_PortsOpened.add(s_GivenPort);
			}
			if(m_FullPortScan){

				System.out.println("Beginning full-port scan...");
				for(int i = 0; i < 1025; i++){
					WebCrawler.s_ThreadPoolOfDownloaders.execute
					(new TaskOfDownloadersThread("", s_GivenPort, "/"));
					synchronized(s_Stats){
						s_Stats.m_PortsOpened.add(i);
					}
				}

			}else{
				domainTask = new TaskOfDownloadersThread("", s_GivenPort, "/");
				s_ThreadPoolOfDownloaders.execute(domainTask);
				
			}
			Thread finisher = new Thread(new ThreadsManager());
			finisher.start();
		}

	}

	private void takeCareOfRobotsLinks() throws UnknownHostException, IOException{

		System.out.println("\nHandling now robots.txt...");

		if(s_GivenPort != -1){

			String contentAsString = TaskOfAnalyzerThread.httpMethodReader
					("GET", "/robots.txt", s_GivenPort, s_DomainLink);

			if(s_WriteToLogs){
				FileWriter fw = new FileWriter("logs//robotsLog.txt");
				fw.append("GET " + "/robots.txt" + " HTTP/1.0" + "\n");
				fw.append("HOST: " + s_DomainLink + "\n");
				fw.append(contentAsString);
				fw.close();
			}

			//We take care of wild cards by finding the prefix of a link containing a wc.
			Pattern robotsFinder = Pattern.compile("(?i)(Dis)?allow:( *?)([^*\n\r]+)(\\*)?");  
			Matcher robotsMatcher = robotsFinder.matcher(contentAsString);

			while(robotsMatcher.find()) {
				if(robotsMatcher.group(1) == null){ //Allow
					TaskOfDownloadersThread robotTask;
					try{
						System.out.println("Allow: " + robotsMatcher.group(3).trim());
						robotTask = new TaskOfDownloadersThread
								(robotsMatcher.group(3).trim(), s_GivenPort, "");

						if(!s_LinksVisited.contains(robotsMatcher.group(3).trim())){
							s_ThreadPoolOfDownloaders.execute(robotTask);
						}
					}catch(Exception e){
						System.out.println("Couldn't create allowed robot task");
					}

				}else{ //Disallow

					if(s_DisrespectRobots){
						TaskOfDownloadersThread robotTask;
						try{
							if(robotsMatcher.group(4) == null){ 
								System.out.println("This is a Disallow: " + robotsMatcher.group(3).trim());

								robotTask = new TaskOfDownloadersThread
										(robotsMatcher.group(3).trim(), s_GivenPort, "/");

								if(!s_LinksVisited.contains(robotsMatcher.group(3).trim())){
									s_ThreadPoolOfDownloaders.execute(robotTask);
								}
							}
							//If we found a wild card, we won't crawl into it, 
							//since it is a directory / a bad request 
							//(* on suffixes will take for example index.html -> index)

						}catch(Exception e){
							System.out.println("Couldn't create disallowed robot task");
						}

					}else{
						if(robotsMatcher.group(4) != null){ //There is a WildCard, uses a smaller arrayList.
							s_WildCards.add(robotsMatcher.group(3).trim());
						}
						//We add this anyway:
						s_LinksVisited.add(robotsMatcher.group(3).trim());
					}

				}
			}

		}
	}

	public synchronized static void doWhenCrawlerIsDone(Socket i_Socket) {

		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			Date date = new Date();
			char[] arr = dateFormat.format(date).toCharArray();
			arr[10] = '_';

			StringBuilder fileName = new StringBuilder();

			fileName.append('_').append(arr);
			String sfileName = WebServer.rootDirectory + s_DomainLink + fileName.toString() + ".html";
			File file = new File(sfileName);
			FileWriter fw = new FileWriter(file);

			StringBuilder htmlPage = new StringBuilder();

			htmlPage.append("<html><body><h1>Crawling Results:</h1>");

			if(s_DisrespectRobots) {
				htmlPage.append("<p>The crawler did not respect robots.txt.</p>");				
			} else {
				htmlPage.append("<p>The crawler did respect robots.txt.</p>");
			}

			htmlPage.append("<p>Number of images: " + s_Stats.m_NumImages.toString() + "</p>");

			htmlPage.append("<p>Total size (in bytes) of images: " + s_Stats.m_SizeImages.toString() + "</p>");

			htmlPage.append("<p>Number of videos: " + s_Stats.m_NumVideos.toString() + "</p>");

			htmlPage.append("<p>Total size (in bytes) of videos: " + s_Stats.m_SizeVideos.toString() + "</p>");

			htmlPage.append("<p>Number of document: " + s_Stats.m_NumDocs.toString() + "</p>");

			htmlPage.append("<p>Total size (in bytes) of documents: " + s_Stats.m_SizeDocs.toString() + "</p>");

			htmlPage.append("<p>Number of pages: " + s_Stats.m_NumPages.toString() + "</p>");
			StringBuilder domainsConnectedto = new StringBuilder();
			for(String link : s_Stats.m_ExternalLinks){
				String domain = domainFinder(link);
				domainsConnectedto.append("\n<br><a href=" + domain +" >"+ domain +"</a>");
			}
			htmlPage.append("<p>These are the domains you tried to connect to: " + domainsConnectedto + "</p>");

			htmlPage.append("<p>Total size (in bytes) of pages: " + s_Stats.m_SizePages.toString() + "</p>");

			htmlPage.append("<p>Number of internal links: " + s_Stats.m_NumInternalLinks.toString() + "</p>");

			htmlPage.append("<p>Number of external links: " + s_Stats.m_NumExternalLinks.toString() + "</p>");

			htmlPage.append("<p>Number of domains the crawled domain is connected to: " + s_Stats.m_NumExternalLinks.toString() + "</p>");
			

			StringBuilder openedPorts = new StringBuilder();
			int time = 0;
			for(Integer port : s_Stats.m_PortsOpened){
				openedPorts.append(port + ",");
				if(time % 15 == 0){
					openedPorts.append("\n");
				}
			}
			openedPorts.deleteCharAt(openedPorts.lastIndexOf(","));
			htmlPage.append("<p>Ports opened are: " + openedPorts + "</p>");
		 
			long sum = 0;
			for(Long rtt :  s_Stats.m_AverageRTT){
				sum += rtt;
			}
			double avg = (double) sum / s_Stats.m_AverageRTT.size();  
			htmlPage.append("<p>Average RTT in milliseconds: " + avg + "</p>");

			htmlPage.append("<a href=\"index.html\"> Main Page </a>");
			fw.append(htmlPage);
			fw.close();
			
			String linkToAdd = "<br><a href=" + s_DomainLink + fileName.toString() + ".html?id=0" + ">" 
					+ s_DomainLink + fileName.toString() + ".html</a>" + "\n";
			
			String indexPage = TaskOfAnalyzerThread.httpMethodReader("GET", "index.html", WebServer.s_Port, "127.0.0.1");
			int indexOfHTML = indexPage.indexOf("<html>");
			indexPage = indexPage.substring(indexOfHTML);
			
			int indexWhereBodyEnds = indexPage.indexOf("</body>");
			StringBuilder linkPageBuilder = new StringBuilder(indexPage);
			
			linkPageBuilder.insert(indexWhereBodyEnds - 1, linkToAdd);
			FileWriter indexHTML = new FileWriter(WebServer.rootDirectory + "//index.html", false);
						
			indexHTML.append(linkPageBuilder.toString());
			indexHTML.close();
			
			System.out.println("YOU JUST FINISHED CRAWLING!");
		} catch (Exception e) {
			System.out.println("Exception in last function");
			e.printStackTrace();
		}


	}
	
	
	public static String domainFinder(String i_Link){
		i_Link = i_Link.replaceAll("https?:\\/\\/", "");
		int firstSlash = i_Link.indexOf("/");
		String returnVal;
		if(firstSlash != -1){
			returnVal = i_Link.substring(0, firstSlash);
		}else{
			returnVal = i_Link;
		}
		 
		//even if it was https it is still acceptable by browser
		return "http://" + returnVal;
	}

}