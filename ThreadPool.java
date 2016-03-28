import java.util.concurrent.LinkedBlockingQueue;

public class ThreadPool {
	public LinkedBlockingQueue<Runnable> m_TasksQueue;
	public ThreadInPool[] m_Threads;
	
	public ThreadPool(int i_maxThreads) {
		m_TasksQueue = new LinkedBlockingQueue<Runnable>();
		m_Threads = new ThreadInPool[i_maxThreads];
			
		for (int i = 0; i < i_maxThreads; i++) {
			m_Threads[i] = new ThreadInPool(m_TasksQueue);
		}
		
		for(ThreadInPool thread : m_Threads) {
			thread.start();
		}
	}
	
	public void execute(Runnable i_Task) {
		m_TasksQueue.add(i_Task);
	}
}
