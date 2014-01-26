import java.util.ArrayList;



public class miditrack
{
	
	public String trackname;
	
		
	ArrayList<midievent> eventlist;
	
	private int eventcounter;
	private long totaloffset;
		
	miditrack()
	{
		eventlist = new ArrayList<midievent>();		
	}
	
	public void init()
	{
		eventcounter = 0;
		totaloffset = 0;
	}
	
	public void clear()
	{
		eventlist.clear();
	}
	
	public long getNextNoteTime()
	{
		// return the overall offset		
		long tempval = eventlist.get(eventcounter).deltatime;
		eventcounter++;
		if (eventcounter > eventlist.size()-1) return -1;
		totaloffset += tempval;
		return tempval;
	}
	
	public boolean hasEvents()
	{
		if (eventlist.size() > 0)
		{
			return true;
		} else
		{
			return false;
		}
	}
	
	public void AddEvent(long deltatime, int channel, int command, int noteval, int velocity)
	{	
		midievent tempevent = new midievent();
		tempevent.deltatime = deltatime;
		tempevent.channel = channel;
		tempevent.command = command;
		tempevent.noteval = noteval;
		tempevent.velocity = velocity;		
		eventlist.add(tempevent);	
	
	}
	
	
	public class midievent
	{
		public long deltatime;
		public int channel;
		public int command;
		public int noteval;
		public int velocity;	
		
	};
	
	
		
	
}