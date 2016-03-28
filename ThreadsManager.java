public class ThreadsManager implements Runnable{

	public void run(){
		synchronized(this){
			while(true){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					System.out.println("Sleeping manager thread was interrupted at first");
				}
				int checker = 0;
				for(int i = 0; i < 1000000; i++){
					if(WebCrawler.s_ThreadPoolOfDownloaders.m_TasksQueue.isEmpty() 
							&& WebCrawler.s_ThreadPoolOfAnalyzers.m_TasksQueue.isEmpty() && 
							WebCrawler.s_AnalyzeTasksWorking.toString().equals("0") && 
							WebCrawler.s_DownloadTasksWorking.toString().equals("0")){
						
						checker++;
					}
				}
				if(checker == 1000000){
					
					if(WebCrawler.s_ThreadPoolOfDownloaders.m_TasksQueue.isEmpty() && 
							WebCrawler.s_ThreadPoolOfAnalyzers.m_TasksQueue.isEmpty()&& 
							WebCrawler.s_AnalyzeTasksWorking.toString().equals("0") && 
							WebCrawler.s_DownloadTasksWorking.toString().equals("0")){
						
						WebCrawler.doWhenCrawlerIsDone(WebCrawler.s_ClientSocket);
						WebCrawler.s_AlreadyRunning = false;
						return;
					}else{
						try {
							//sleep for 5 secs.
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							System.out.println("Sleeping manager thread was interrupted");
						}
					}
				}


			}
		}
	}

}
