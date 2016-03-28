import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticSums {
	
	public AtomicInteger m_NumVideos;
	public AtomicLong m_SizeVideos;
	
	public AtomicInteger m_NumImages;
	public AtomicLong m_SizeImages;
	
	public AtomicInteger m_NumPages;
	public AtomicLong m_SizePages;
	
	public AtomicInteger m_NumDocs;
	public AtomicLong m_SizeDocs;
	
	public AtomicLong m_NumInternalLinks;
	public AtomicLong m_NumExternalLinks;
	public ArrayList<String> m_ExternalLinks;
	
	public ArrayList<Long> m_AverageRTT; // in milliseconds
	public ArrayList<Integer> m_PortsOpened;
	
	
	public StatisticSums(){
		this.m_NumVideos = new AtomicInteger();
		this.m_NumImages = new AtomicInteger();
		this.m_NumPages = new AtomicInteger();
		this.m_NumDocs = new AtomicInteger();
		this.m_NumInternalLinks = new AtomicLong();
		this.m_NumExternalLinks = new AtomicLong();
		this.m_ExternalLinks = new ArrayList<String>();
		
		this.m_SizeVideos = new AtomicLong();
		this.m_SizeImages = new AtomicLong();
		this.m_SizePages = new AtomicLong();
		this.m_SizeDocs = new AtomicLong();
		
		this.m_AverageRTT = new ArrayList<Long>();
		this.m_PortsOpened = new ArrayList<Integer>();
		
		
	}

}
