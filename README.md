# Crawler
WebCrawler made as a Computer Networks project.
*****************Lab 2******************
Submitting: 
Yotam Schreiber 
Asher Holder 


***Comments****
We added a "logs" folder - will be used by one of the bonuses

***Classes***
	
	*WebServer - This class contains the main method, its constructor reads the config file and creates the thread-pool of (10) workers.
	Its main method runs the server, meaning - creates the server (our computer with given port out of config.ini) and client connection for each client.
	
	*ThreadPool - This class has a queue field of runnable tasks, and an array of Threads of size maxThreads.
	Its execute method enqueues the runnable task for its workers (threads) to dequeue them and make them run.
	
	*ThreadInPool - its run method dequeues from taskqueue field and then makes it run. 
	
	*TaskFromClientThread - This class is the runnable task that is ultimately sent to the tasks queue object.
	Its run method creates the request with the server's client-connection and then
	the server's response using the newly created HTTP request.
	
	*HTTPRequest - Receives the client's connection, reads the header values and HTTP method. In case that the method is "POST"
	we read the content of its message and parse it.
	
	*HTTPResponse - Receives the request of client if it's valid and returns an http response, else
	it sends a response explaining (according to the http protocol) what went wrong. If we were asked by the form given in the
	UI for different options it changes the way the crawler will act. Either way it receives a domain and a port, if it doesn't get it in the right
	format it will return an html message accordingly. If user decided to disrespect robots.txt, WebCrawler will be notified.
	
	*ServerUtils - This class contains enums, global variables (most of them String vars.), and created exceptions that fit our needs.
	
	*WebCrawler - This class object will first read robots.txt, after that it will run the crawler, 
				  checks when it ends and contains many static fields that are used in other classes. 
			      Significant static fields are: 
					-2 ThreadPool objects - one that contains the threads that run analytics of given html pages, and the other one contains
					 threads that download a given link GET content.
					-A statistics field that carries the stats. asked in the exercise.
					-booleans that let us know in which mode we are crawling (disrespecting robots, writing to log files[bonus]...)
	
	*TaskOfDownloadersThread - this class implements Runnable, each object of the tasksQueue (downloader threadPool) is of this class.
							   this runnable does a GET to the given link according to the received port and domain.
	
	*TaskOfAnalyzerThread - this class implements Runnable, each object of the tasksQueue (analyzer threadPool) is of this class.
							this runnable analyzes each link in the content read from TaskOfDownloaderThread.
							It classifies a link as another link to crawl into or as an image, video or document depending on config.ini.
							
	*StatisticSums - the object created in WebCrawler from this class contains fields that allow us to display the statistics asked in the exercise.
	
	*ThreadsManager - this class implements Runnable. After executing the first TaskOfDownloaderThread in WebCrawler we create a thread 
					  that sleeps for a few seconds, then checks 1000000 times if the conditions to finish crawling are fulfilled, if they were
					  correct for all times, we then proceed to the last step of the exercise - creating a statistics html page according to 
					  given format. A link to the statistics html page will appear in the default page (index.html) after refreshing the page
					  (this will happen after console message "YOU JUST FINISHED CRAWLING!".
	
