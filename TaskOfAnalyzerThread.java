import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskOfAnalyzerThread implements Runnable{

	public String m_ContentToAnalyze;
	public int m_Port;
	public String m_LinkOfContent;


	public TaskOfAnalyzerThread(String i_ContentToAnalyze, int i_Port, String i_LinkOfContent){
		this.m_ContentToAnalyze = i_ContentToAnalyze;
		this.m_Port = i_Port;
		this.m_LinkOfContent = i_LinkOfContent;
		WebCrawler.s_AnalyzeTasksWorking.addAndGet(1);
	}

	public void run(){

		//Take care of images:
		Pattern imageFinder = Pattern.compile
				("(?i)<img[\\s\\S]+?src\\s*=\\s*\'?\"?([^\"\']+)\"?\'?([\\s\\S]*?)>");
		Matcher imageMatcher = imageFinder.matcher(m_ContentToAnalyze);
		if(imageMatcher.find()){
			imageMatcher.reset();
			findExtensionThenAnalyze(imageMatcher, 1, WebCrawler.s_ImageExtensions, "images");
		}



		//Take care of videos in html5(Bonus):
		Pattern rawVideoFinder = Pattern.compile("<video.*?>((.|[\\s])+)<\\/video>");
		Matcher rawVideoMatcher = rawVideoFinder.matcher(m_ContentToAnalyze);
		Pattern videoFinder = Pattern.compile
				("<source.*?src=\'?\"([^\\s\'\"@]+).*?type=\'?\"?([^\\s\'\"@]+).*?>");
		while(rawVideoMatcher.find()){
			Matcher videoMatcher = videoFinder.matcher(rawVideoMatcher.group(1));
			findExtensionThenAnalyze(videoMatcher, 1, WebCrawler.s_VideoExtensions, "video");
		}

		Pattern linkOrDocOrVidFinder = Pattern.compile
				("(?i)(<a[\\s\\S]*?href\\s*=\\s*\'?\"?(([^\"\'#@]+)\\.?([^\\s\'\"@]*)))"
						+ "|(location: \"?\'?([^\\s\"#\'@]+).*?)");
		Matcher linkAndDocMatcher = linkOrDocOrVidFinder.matcher(m_ContentToAnalyze);

		Pattern linkTagHrefFinder = Pattern.compile
				("<link[\\s]+?(rel\\s*=\\s*\'?\"?[^\"\']+"
						+ "\"?\'?)?[\\s\\S]*?href\\s*=\\s*\'?\"?([^@\"\']+)\"?\'?([\\s\\S]*?)>");
		Matcher linkTagHrefMatcher = linkTagHrefFinder.matcher(m_ContentToAnalyze);

		//Bonus
		if(WebCrawler.s_CheckLinksInHeadTag){
			while(linkTagHrefMatcher.find()){

				if(!linkTagHrefMatcher.group(2).contains(WebCrawler.s_DomainLink)){//might be relative
					if(WebCrawler.s_DomainLink.equals("127.0.0.1") || 
							WebCrawler.s_DomainLink.equals("localhost")){
						linkTagHrefMatcher.reset();
						findExtensionThenAnalyze(linkTagHrefMatcher, 2, WebCrawler.s_ImageExtensions, "images");
						break;
					}
					try {

						//Check for imgs, docs, videos:
						String hrefLink = linkTagHrefMatcher.group(2);
						boolean areWeDoneWithThisLoop = false;

						//It's a document, we don't crawl it but we analyze it:
						for(String docExt : WebCrawler.s_DocExtensions){
							if(hrefLink.toLowerCase().endsWith(docExt)){
								analyzerOfDocImgVid(hrefLink, "docs");
								areWeDoneWithThisLoop = true;
							}
						}
						if(areWeDoneWithThisLoop == true){
							continue;
						}
						//Videos in versions below html5:
						for(String vidExt : WebCrawler.s_VideoExtensions){
							if(hrefLink.toLowerCase().endsWith(vidExt)){
								analyzerOfDocImgVid(hrefLink, "video");
								areWeDoneWithThisLoop = true;
							}
						}
						if(areWeDoneWithThisLoop == true){
							continue;
						}

						//images
						for(String imgExt : WebCrawler.s_ImageExtensions){
							if(hrefLink.toLowerCase().endsWith(imgExt)){
								analyzerOfDocImgVid(hrefLink, "images");
								areWeDoneWithThisLoop = true;
							}
						}

						if(areWeDoneWithThisLoop == true){
							continue;
						}

						crawl(hrefLink);

					} catch (Exception e) {
						System.out.println
						("Couldn't crawl: " + "/" + linkTagHrefMatcher.group(2));
					}
				}else{ //not
					try {

						//Check for imgs, docs, videos:
						String hrefLink = linkTagHrefMatcher.group(2);

						boolean areWeDoneWithThisLoop = false;

						//It's a document, we don't crawl it but we analyze it:
						for(String docExt : WebCrawler.s_DocExtensions){
							if(hrefLink.toLowerCase().endsWith(docExt)){
								analyzerOfDocImgVid(hrefLink, "docs");
								areWeDoneWithThisLoop = true;
							}
						}
						if(areWeDoneWithThisLoop == true){
							continue;
						}
						//Videos in versions below html5:
						for(String vidExt : WebCrawler.s_VideoExtensions){
							if(hrefLink.toLowerCase().endsWith(vidExt)){
								analyzerOfDocImgVid(hrefLink, "video");
								areWeDoneWithThisLoop = true;
							}
						}
						if(areWeDoneWithThisLoop == true){
							continue;
						}

						//images
						for(String imgExt : WebCrawler.s_ImageExtensions){
							if(hrefLink.toLowerCase().endsWith(imgExt)){
								analyzerOfDocImgVid(hrefLink, "images");
								areWeDoneWithThisLoop = true;
							}
						}

						if(areWeDoneWithThisLoop == true){
							continue;
						}
						crawl(hrefLink);
					} catch (Exception e) {
						System.out.println
						("Couldn't crawl: " + linkTagHrefMatcher.group(2));
					}
				}

			}
		}
		while(linkAndDocMatcher.find()){	
			try {
				boolean areWeDoneWithThisLoop = false;
				String hrefLink = null;
				if(linkAndDocMatcher.group(1) == null){//not in <a>
					hrefLink = linkAndDocMatcher.group(6);
				}else{
					hrefLink = linkAndDocMatcher.group(2);
				}


				//It's a document, we don't crawl it but we analyze it:
				for(String docExt : WebCrawler.s_DocExtensions){
					if(hrefLink.toLowerCase().endsWith(docExt)){
						analyzerOfDocImgVid(hrefLink, "docs");
						areWeDoneWithThisLoop = true;
					}
				}

				if(areWeDoneWithThisLoop == true){
					continue;
				}

				//Videos in versions below html5:
				for(String vidExt : WebCrawler.s_VideoExtensions){
					if(hrefLink.toLowerCase().endsWith(vidExt)){
						analyzerOfDocImgVid(hrefLink, "video");
						areWeDoneWithThisLoop = true;
					}
				}

				if(areWeDoneWithThisLoop == true){
					continue;
				}

				//We now crawl with the href link
				crawl(hrefLink);

			} catch (Exception e) {
				System.out.println("Couldn't crawl this link");
			}
		}
		WebCrawler.s_AnalyzeTasksWorking.addAndGet(-1);
	}



	private void crawl(String i_Link) throws Exception{

		if(isInDomain(i_Link)){ 
			System.out.println("This is the link you are about to crawl: "+ i_Link);
			synchronized(WebCrawler.s_Stats){
				WebCrawler.s_Stats.m_NumInternalLinks.getAndAdd(1);
				WebCrawler.s_Stats.m_NumPages.getAndAdd(1);
			}

			Pattern parentFinder = Pattern.compile("(.*\\/)([^\\s\\/]*)");
			Matcher parentMatcher = parentFinder.matcher(this.m_LinkOfContent);

			if(parentMatcher.find()){
				if(i_Link.startsWith("/") && !this.m_LinkOfContent.equals("")){
					i_Link = i_Link.substring(1);
				}
				
				String processedLink;
				if(i_Link.startsWith("http")){
					processedLink = i_Link;
				}else{
					processedLink = parentMatcher.group(1) + i_Link;
				}
				
				Matcher pathOfProcessedLink = parentFinder.matcher(processedLink);
				if(pathOfProcessedLink.find()){
					String pathToPass = pathOfProcessedLink.group(1);
					WebCrawler.s_ThreadPoolOfDownloaders.execute
					(new TaskOfDownloadersThread(processedLink, m_Port, pathToPass));
				}
			}

		}else{
			System.out.println("Out of domain!: " + i_Link);
			synchronized(WebCrawler.s_Stats){
				WebCrawler.s_Stats.m_NumExternalLinks.getAndAdd(1);
				String domainToAdd = WebCrawler.domainFinder(i_Link);
				if(!WebCrawler.s_Stats.m_ExternalLinks.contains(domainToAdd)){
					WebCrawler.s_Stats.m_ExternalLinks.add(domainToAdd);
				}
			}
		}
	}


	private boolean isInDomain(String i_Link){

		if(i_Link.startsWith("/")){
			return true;
		}
		Pattern nameFinder = Pattern.compile("(?i)(https?:\\/\\/)?((www)?\\.?([a-zA-Z-_]+))");
		Matcher nameMatcher = nameFinder.matcher(WebCrawler.s_DomainLink);
		Matcher nameOnLinkMatcher = nameFinder.matcher(i_Link);

		if(nameMatcher.find() && nameOnLinkMatcher.find()){
			String nameOfDomain = nameMatcher.group(4);
			String nameOfLinkDomain = nameOnLinkMatcher.group(4);
			if(nameOfDomain.equalsIgnoreCase(nameOfLinkDomain)){

				return true;
			}
			if(nameOnLinkMatcher.group(1) == null && nameOnLinkMatcher.group(3) == null){
				//e.g index.html
				return true;
			}
		}
		if(WebCrawler.s_DomainLink.equals("127.0.0.1") || WebCrawler.s_DomainLink.equals("localhost")){
			nameOnLinkMatcher.reset();
			if(nameOnLinkMatcher.find()){
				if(nameOnLinkMatcher.group(1) != null || nameOnLinkMatcher.group(3) != null){
					return false;
				}
			}
			return true;
		}
		return false;
	}



	private void findExtensionThenAnalyze(Matcher i_Matcher, int i_Group, String[] i_Extensions, String i_LogName){

		while(i_Matcher.find()){
			String rawLink = null;
			System.out.println("Analyzer found: " + i_LogName + ": " + i_Matcher.group(i_Group));
			for(String extension : i_Extensions){
				if(i_Matcher.group(i_Group).contains(extension)){
					rawLink = i_Matcher.group(i_Group);
					break; // we found a link, no need to keep looking for extensions.
				}
			}
			if(rawLink != null){
				analyzerOfDocImgVid(rawLink, i_LogName);
			}
		}
	}

	//Helper Method for other classes too
	public static String httpMethodReader(String i_Method,String i_Link, int i_Port, String i_Domain) 
			throws UnknownHostException, IOException{

		Socket socket = new Socket(i_Domain, i_Port);
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter
				(socket.getOutputStream())));

		long firstMeasure = System.currentTimeMillis();
		out.print(i_Method + " " + i_Link + " HTTP/1.0" + ServerUtils.CRLF);
		out.println("HOST: " + WebCrawler.s_DomainLink + ServerUtils.CRLF);
		out.flush();

		InputStreamReader isr = new InputStreamReader(socket.getInputStream());
		BufferedReader br = new BufferedReader(isr);
		String currString;
		StringBuilder contentOfResponse = new StringBuilder();
		long secondMeasure = System.currentTimeMillis();
		while((currString = br.readLine()) != null){
			contentOfResponse.append(currString + "\n");
		}
		out.close();
		socket.close();
		synchronized(WebCrawler.s_Stats){
			WebCrawler.s_Stats.m_AverageRTT.add(secondMeasure - firstMeasure);
		}

		return contentOfResponse.toString();
	}

	private void analyzerOfDocImgVid(String i_Link, String i_Log){
		try{

			Pattern parentFinder = Pattern.compile("(.*\\/)([^\\s]*)");
			Matcher parentMatcher = parentFinder.matcher(this.m_LinkOfContent);

			if(parentMatcher.find()){
				if(i_Link.startsWith("/")){
					i_Link = i_Link.substring(1);
				}

				String processedLink;
				if(i_Link.startsWith("http")){
					processedLink = i_Link;
				}else{
					processedLink = parentMatcher.group(1) + i_Link;
				}
				
				String contentAsString = httpMethodReader("HEAD", processedLink, this.m_Port, WebCrawler.s_DomainLink);
				Pattern contentLengthFinder = Pattern.compile("(?i)Content-Length\\s*:\\s*([0-9]+)");
				Matcher contentLengthMatcher = contentLengthFinder.matcher(contentAsString);

				if(contentLengthMatcher.find()){
					try{
						int length = Integer.parseInt(contentLengthMatcher.group(1));
						//Statistics:
						synchronized(WebCrawler.s_Stats){
							if(i_Log.equals("video")){
								WebCrawler.s_Stats.m_NumVideos.addAndGet(1);
								WebCrawler.s_Stats.m_SizeVideos.addAndGet(length);
								System.out.println("this is the link: " + processedLink + " this is the curr amount of video bytes: " 
										+ WebCrawler.s_Stats.m_SizeVideos + 
										" This is the curr Amount of videos: " + WebCrawler.s_Stats.m_NumVideos);
							}else if(i_Log.equals("docs")){
								WebCrawler.s_Stats.m_NumDocs.addAndGet(1);
								WebCrawler.s_Stats.m_SizeDocs.addAndGet(length);
								System.out.println("this is the link: " + processedLink + " this is the curr amount of doc bytes: " 
										+ WebCrawler.s_Stats.m_SizeDocs + 
										" This is the curr Amount of docs: " + WebCrawler.s_Stats.m_NumDocs);
							}else if(i_Log.equals("images")){
								WebCrawler.s_Stats.m_NumImages.addAndGet(1);
								WebCrawler.s_Stats.m_SizeImages.addAndGet(length);
								System.out.println("this is the link: " + processedLink + " this is the curr amount of img bytes: " 
										+ WebCrawler.s_Stats.m_SizeImages + 
										" This is the curr Amount of imgs: " + WebCrawler.s_Stats.m_NumImages);
							}
						}
					}catch(NumberFormatException e){
						System.out.println(contentLengthMatcher.group(1) + "isnt a number");
					}
				}
				// Bonus:
				if(WebCrawler.s_WriteToLogs){

					//Characters that are not allowed:
					String link = processedLink.replaceAll("(<|>|:|\"|\\/|\\\\|\\||\\?|\\*)", "_"); 
					String nameOfFile = i_Log + "_" + link + ".txt";
					File file = new File(TaskOfDownloadersThread.s_LogsPath, nameOfFile);
					FileWriter fw = new FileWriter(file);

					fw.append(processedLink + "\n");
					fw.append(contentAsString);
					fw.close();
				}
			}
		}catch(Exception e){
			System.out.println("Couldn't retrieve info about: " + i_Link);
		}
	}

}
