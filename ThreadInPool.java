import java.util.concurrent.LinkedBlockingQueue;

public class ThreadInPool extends Thread {
	public LinkedBlockingQueue<Runnable> m_Queue;
	
	public ThreadInPool(LinkedBlockingQueue<Runnable> queue) {
		this.m_Queue = queue;
	}

	public void run() {
		
		while(true) {
			Runnable task;
			try {
				task = m_Queue.take();
				task.run();
			} catch (Exception e) {
				System.out.println("Thread in pool was interrupted");
			}
		}
	}
}
