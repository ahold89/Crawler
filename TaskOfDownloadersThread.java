import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TaskOfDownloadersThread implements Runnable{


	public String m_LinkToCrawl;
	public int m_Port;
	public static String s_LogsPath = System.getProperty("user.dir") + "\\logs";
	public String m_NameOfFile;
	public String m_ParentPath;


	public TaskOfDownloadersThread(String i_PageToCrawl, int i_Port, String i_ParentPath){
		this.m_LinkToCrawl = i_PageToCrawl;
		this.m_Port = i_Port;
		this.m_ParentPath = i_ParentPath;

		//For bonus:
		this.m_NameOfFile = "page_" + 
				this.m_LinkToCrawl.replaceAll("(<|>|:|\"|\\/|\\\\|\\||\\?|\\*)", "_") + ".txt";
		//No name of file can be longer than 128 chars in windows:
		if(m_NameOfFile.length() > 128){
			this.m_NameOfFile = m_NameOfFile.substring(0, 115) + ".txt"; //because we are adding a prefix and suffix.
		}
		
		WebCrawler.s_DownloadTasksWorking.addAndGet(1);
	}

	public void run(){

		System.out.println("Starting to download link: " + m_LinkToCrawl + "\n");

		Socket weAsClients;
		try {
			if(!WebCrawler.s_LinksVisited.contains(m_LinkToCrawl)){//We only crawl if we haven't visited
				
				//if we are respecting robots, we won't crawl into links that begin with robot links
				if(!WebCrawler.s_DisrespectRobots){
					for(String robot : WebCrawler.s_WildCards){
						if(m_LinkToCrawl.startsWith(robot)){
							System.out.println("Sorry robots doesn't allow us to keep crawling: " + m_LinkToCrawl);
							return;
						}
					}
				}
				
				WebCrawler.s_LinksVisited.add(m_LinkToCrawl);
				

				System.out.println("This is the page we are about to crawl: " + m_LinkToCrawl);
				weAsClients = new Socket(WebCrawler.s_DomainLink, m_Port);
				PrintWriter out = new PrintWriter(new BufferedWriter
						(new OutputStreamWriter(weAsClients.getOutputStream())));
				//Measuring RTT:
				long firstMeasure;
				if(!this.m_LinkToCrawl.equals("")){
					out.print("GET " + this.m_LinkToCrawl + " HTTP/1.0" + ServerUtils.CRLF);
					firstMeasure = System.currentTimeMillis();
					System.out.println("GET " + this.m_LinkToCrawl + " HTTP/1.0");

				}else{
					out.print("GET / HTTP/1.0" + ServerUtils.CRLF);
					firstMeasure = System.currentTimeMillis();
					System.out.println("GET / HTTP/1.0");
				}
				out.println("HOST: " + WebCrawler.s_DomainLink + ServerUtils.CRLF);
				System.out.println("HOST: " + WebCrawler.s_DomainLink);
				
				out.flush();

				InputStreamReader isr = new InputStreamReader(weAsClients.getInputStream());
				BufferedReader br = new BufferedReader(isr);
				String currString;
				StringBuilder contentOfPage = new StringBuilder();
				long secondMeasure = System.currentTimeMillis();
				while((currString = br.readLine()) != null){
					contentOfPage.append(currString + "\n");
				}
				out.close();
				weAsClients.close();
				String contentAsString = contentOfPage.toString();
				Pattern contentLengthFinder = Pattern.compile("(?i)content-length\\s*:\\s*([0-9]+)");
				Matcher contentLength = contentLengthFinder.matcher(contentAsString);
				if(contentLength.find()){
					synchronized(WebCrawler.s_Stats){
						try{
							WebCrawler.s_Stats.m_SizePages.getAndAdd(Integer.parseInt(contentLength.group(1)));
						}catch(NumberFormatException e){
							System.out.println("Couldn't parse content length of page");
						}
						
					}
				}
				
				synchronized(WebCrawler.s_Stats){
					WebCrawler.s_Stats.m_AverageRTT.add(secondMeasure - firstMeasure);
				}
				System.out.println("Done reading: " + m_LinkToCrawl);
				//Bonus:
				if(WebCrawler.s_WriteToLogs){
					try{
						File file = new File(s_LogsPath, m_NameOfFile);
						FileWriter log = new FileWriter(file);
						log.append(contentAsString);
						log.close();
					}catch(IOException e){
						System.out.println("Couldn't write to file");
					}
				}

				//Analyze the page:
				if(!m_LinkToCrawl.startsWith("/")){
					m_LinkToCrawl = "/" + m_LinkToCrawl;
				}
				TaskOfAnalyzerThread analyzer = new TaskOfAnalyzerThread
						(contentAsString, m_Port, m_LinkToCrawl);
				WebCrawler.s_ThreadPoolOfAnalyzers.execute(analyzer);
				weAsClients.close();
			}
		} catch (Exception e1) {
			System.out.println("Couldn't scan port: " + m_Port);
			synchronized(WebCrawler.s_Stats){
				WebCrawler.s_Stats.m_PortsOpened.remove(Integer.valueOf(m_Port));
			}
			return;
		}
		WebCrawler.s_DownloadTasksWorking.addAndGet(-1);
	}
}