

import java.io.*;



public class main
{
		
	static FileInputStream midifile;	
	static String filetoread = "C:\\users\\niko20\\Desktop\\Instant drum patterns\\drum patterns type 1\\260 patterns\\bosa.mid";	
	//static String filetoread = "C:\\users\\niko20\\Desktop\\hideaway.mid";
	static miditrack[] tracks;
	
	public static void main(String [] args)
	{	
		try {
			
			midifile = new FileInputStream(filetoread);			
			long filesize = new File(filetoread).length();			
			byte[] filedata = new byte[(int)filesize];
			
			try {
				midifile.read(filedata);				
				midifile.close();			
				// read the MIDI data
				midiparser myparser = new midiparser(filedata);	
				myparser.setOption(midiparser.MidiOptions.DO_NOT_TRACK_OFF_NOTES);
				if (myparser.parseHeader())
				{
					int numtracks = myparser.getNumTracks();
					tracks = new miditrack[numtracks];
					for (int j=0; j< numtracks; j++)
					{
						tracks[j] = new miditrack();
					}
					// we should create track objects ourself and pass them in					
					if (myparser.parseTracks(tracks))
					{
						System.out.print(new Float(myparser.getBpm()).toString() + "\n");
						System.out.print(new Integer(myparser.getNumTracks()).toString() + "\n");
						for (int j=0; j<numtracks;j++)
						{
							if (tracks[j].hasEvents()) System.out.print(tracks[j].trackname + "\n");
						}
						// Set drum resolution based on this value
						// for Electrum, multiply by 4 first
						long maxdivisions = myparser.getMaxDivision();
						System.out.print(new Long(myparser.getMaxDivision()).toString() + "\n");
						
						long divisionsperstep = myparser.getPPQ()/myparser.getMaxDivision();
						// use this value to determine which slots to set in the drum machine
						System.out.print("Each step will equal MIDI Divisions: \n");
						System.out.print(new Long(divisionsperstep).toString() + "\n");
						System.out.print("\n");
						
						int bars = 32;
						int totaldivisions = (int)(myparser.getMaxDivision() * 4) * bars;
						// because 4/4 time
						int divisionsperbar = (int)(myparser.getMaxDivision()*4);
						
						byte[] printdata = new byte[totaldivisions];
						//byte[][] printdata = new byte[32][divisionsperbar];
					
						// try to print out beat up to 2 bars long
						for (int j=0; j < numtracks ;j++)
						{
							if (tracks[j].hasEvents())
							{							
								for (int k=0; k< totaldivisions ;k++)
								{
									printdata[k] = '-';
								}
								
								// print 'er out
								tracks[j].init();
								
								// print up to 2 bars only
								// get value
								int lastlocation = 0;
								
								int barcount = 0;
								
								for (;;)
								{
									long temptime = tracks[j].getNextNoteTime();
									
									if (temptime == -1) break;									
									// set the proper location in the printdata array...
									int location = (int)(temptime / divisionsperstep);
																		
									//location = location + lastlocation;
									if (location < totaldivisions)
									{							
										printdata[location] = 'X';
									}
									// 	add one because we "took up" some offset already
									//lastlocation = location+1;
																
									
								}
								
								
							
								System.out.print("\t\t" + new String(printdata) + "\n");
									
																	
								// write to drum patterns...
								/*
								byte[][] patterns = new byte[32][divisionsperbar];
								
								int poscounter = 0;
								int startpos = 0;
								int barcounter = 0;
								
								//if (divisionsperbar == 32) divisionsperbar = 16;
								//if (divisionsperbar == 24) divisionsperbar = 12;
								
								
								for (int t=0; t < totaldivisions; t++)
								{
									patterns[barcounter][poscounter] = printdata[t];
									poscounter++;
									
									if (poscounter >= divisionsperbar)
									{
										poscounter = 0;
										barcounter++;
										if (barcounter > 31) break;
									}
									
								}
								
								
								for (int r=0; r<32;r++)
								{
									String outstring = new String(patterns[r]);
									System.out.print(tracks[j].trackname + ":\t" + outstring + "\n");
								}
								*/
								
							}
							
						}
						
					}
				}			
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
		
}