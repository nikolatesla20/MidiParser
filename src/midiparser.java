
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class midiparser
{
	
	int readcounter;
	byte [] filedata;
	short numtracks;
	float bpm;
	short ppq;
	int lasttrackheaderpos;
	long maxdivision;
	MidiOptions options;
	public final int NOTE_ON_MAX = 		159;
	public final int NOTE_ON_MIN = 		144;
	public final int MAX_CHANNELS = 	15;
	public final int NOTE_OFF_MAX =		143;
	public final int NOTE_OFF_MIN = 	128;
	public final int NOTE_ON =			90;
	public final int NOTE_OFF = 		80;
	public final float BPM_MICROSECONDS = 60000000.0f;
	
	midiparser(byte[] datain)
	{		
		readcounter = 0;
		lasttrackheaderpos = 0;
		maxdivision = 0;
		options = MidiOptions.TRACK_OFF_NOTES;
		filedata = datain;		
	}
	
	public void setOption(MidiOptions optionin)
	{
		options = optionin;
		
	}
	
	public float getBpm()
	{
		return bpm;
	}
	
	public short getPPQ()
	{
		return ppq;
	}
		
	public short getNumTracks()
	{
		return numtracks;
	}
		
	public long getMaxDivision()
	{
		// the smallest division of notes (8th, 16th, 32nd)
		return maxdivision;
	}
	
	public boolean parseHeader()
	{
		int headerpos = scandata("MThd",0);
		// make sure it's a type 1 midi file
		if (filedata[headerpos+9] != 1)
		{
			try {
				throw new Exception("can only use Type 1 MIDI Files, this is type " + new Integer(filedata[headerpos+9]).toString());				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();				
			}
			return false;
		}			
		ByteBuffer temp = ByteBuffer.allocate(2);
		temp.order(ByteOrder.BIG_ENDIAN);
		temp.put(filedata,headerpos+10,2);
		temp.rewind();
		numtracks = temp.getShort();
		temp.clear();
		temp.put(filedata,headerpos+12,2);
		temp.rewind();
		ppq = temp.getShort();
		return true;
		
	}
	
	
	public boolean parseTracks(miditrack[] tracksin)
	{		
		for (int tcounter=0; tcounter < numtracks; tcounter++)
		{
			int headerpos = scandata("MTrk",lasttrackheaderpos);
			lasttrackheaderpos = headerpos + 4;
			ByteBuffer temp = ByteBuffer.allocate(4);
			temp.order(ByteOrder.BIG_ENDIAN);
			temp.put(filedata,headerpos+4,4);
			temp.rewind();
			int tracklen = temp.getInt();  
			// move readcounter to starting position
			// so we can read var length items...
			readcounter = headerpos + 8;
			int startpos = readcounter;
			byte[] midievent = new byte[4];
			
			long timedelta = 0;
			
			for (;;)
			{
				// now we can start reading track events.
				timedelta = timedelta + ReadVarLen();
				
				int size=0;
				// get actual operation next
				if (filedata[readcounter] == -1)
				{
					// meta data
					size = filedata[readcounter+2] & 0xFF;
					// look for tempo settings
					if ((filedata[readcounter+1]==0x51) && (filedata[readcounter+2]==0x03))
					{
						// read tempo val
						temp.clear();
						temp.position(1);
						temp.put(filedata, readcounter+3,3);
						temp.rewind();						
						bpm = (BPM_MICROSECONDS / temp.getInt());										
					}			
					
					// check time signature if present
					if (filedata[readcounter+1]==88)
					{
						int checkval = filedata[readcounter+3] & 0xFF;
						int checkval2 = filedata[readcounter+4] & 0xFF;
						if ((checkval != 4) || (checkval2 != 2))
						{
							try {
								throw new Exception("Only 4/4 time allowed");								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}							
							return false;
						}
					}
					// get string
					if (filedata[readcounter+1] == 1)
					{						
						byte[] str = new byte[size];						
						int writecount = 0;						
						for (int c=readcounter+3; c < (readcounter+3+size); c++)
						{
							str[writecount] = filedata[c];
							writecount++;
						}						
						String tempstr = new String(str);
						if (tempstr != null) tracksin[tcounter].trackname = tempstr;						
					}					
					// move readcounter..
					readcounter += size + 3;			
					
				} else
				{
					int command = filedata[readcounter] & 0xFF;	
					// check for control change events, those only use 1 argument
					if ((command >= 192) && (command <= 223))
					{
						readcounter +=2;
						
					} else
					{
						// normal MIDI events
						// we can add these to their tracks!
						if ((timedelta != 0) && (timedelta > 1))
						{
							// figure out minimum note size
							long notedivision = ppq/timedelta;
							if (notedivision > maxdivision) maxdivision = notedivision;
						}											
						// all normal MIDI events
						// are 4 bytes long
						// we already have time delta above
											
						if ((command >= NOTE_ON_MIN) && (command <= NOTE_ON_MAX))
						{						
							// note on
							// get channel
							int channel = MAX_CHANNELS-(NOTE_ON_MAX-command);						
							// get note number
							int notenumber = filedata[readcounter+1] & 0xFF;												
							// get velocity
							int velocity = filedata[readcounter+2] & 0xFF;										
							// add to track array for this track
													
							tracksin[tcounter].AddEvent(timedelta, channel, NOTE_ON, notenumber, velocity);
								
						}					
						
						if ((command >= NOTE_OFF_MIN) && (command <= NOTE_OFF_MAX))
						{
							// note off
								
							int channel = MAX_CHANNELS-(NOTE_OFF_MAX-command);						
							// get note number
							int notenumber = filedata[readcounter+1] & 0xFF;					
							// get velocity
							int velocity = filedata[readcounter+2] & 0xFF;					
							// add to track array for this track
							if (options == MidiOptions.TRACK_OFF_NOTES)
							{
								tracksin[tcounter].AddEvent(timedelta, channel, NOTE_OFF, notenumber, velocity);
							}
								
						}
						
						readcounter += 3;			
					}
				}				
				if ((readcounter-startpos) >= tracklen) break;
			}		
				
		}
		
		return true;
	}
	
	
	int scandata(String datatofind, int startoffset)
	{
		int retval = 0;
		
		int scancounter = startoffset;
		int filelength = filedata.length;
		int datatofindlength = datatofind.length();
		int finallength = filelength-datatofindlength;
		
		ByteBuffer test = ByteBuffer.allocate(datatofindlength);
		
		byte[] comparearray = datatofind.getBytes();
		byte[] testarray = new byte[datatofindlength];
		
		for (scancounter=startoffset; scancounter < finallength; scancounter++)
		{
			
			test.put(filedata,scancounter,datatofindlength);
			test.rewind();
			
			test.get(testarray);
			
			boolean match = true;
			for (int p=0; p < comparearray.length; p++)
			{
				if (comparearray[p] != testarray[p])
				{
					match = false;
					break;
				}
			}
			
			if (match == true)
			{
				retval = scancounter;
				break;
			}			
			test.clear();
					
		}
		return retval;
				
	}
	
	byte getc()
	{
		
		byte tempval =  filedata[readcounter];
		readcounter++;	
		return tempval;
	}
		
	long ReadVarLen()
	{
	    long value;
	    byte c;
	    if ( ((value = getc()) & 0x80) != 0 )
	    {
	       value &= 0x7F;
	       do
	       {
	         value = (value << 7) + ((c = getc()) & 0x7F);
	       } while ((c & 0x80) !=0);
	    }
	    return(value);
	}
	
	
	public static enum MidiOptions
	{
		TRACK_OFF_NOTES,
		DO_NOT_TRACK_OFF_NOTES
	}
	
	
	
	
}