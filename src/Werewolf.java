/******************
 * Werewolf.java
 * Main code file for The Werewolf Game bot, based on pIRC Bot framework (www.jibble.org)
 * Coded by Mark Vine
 * All death/character/other description texts written by Darkshine
 * 31/5/2004 - 4/6/2004
 * v0.99b
 * 
 * Modified as MMBot by Araver (12.2010 - 01.2011)
 * v0.6
 * 
 *****************/
package org.jibble.pircbot.llama.werewolf;

import java.io.*;
import java.util.*;


import org.jibble.pircbot.*;
import org.jibble.pircbot.llama.werewolf.objects.*;
import java.text.DecimalFormat;
public class Werewolf extends PircBot
{
	Vector players,		//Vector to store all the players in the game
		priority,		//Vector to store players for the next round, once the current game is full
		votes,			//A Vector to hold the votes of the villagers
		wolves, 		//A Vector to store the wolf/wolves
		wolfVictim;		//A Vector to hold the wolves choices (in the case there are 2 wolves that vote differently
	
	final int JOINTIME = 60,	//DEPRECATED: time (in seconds) for people to join the game (final because cannot be altered)
		MINPLAYERS = 5,			//Minimum number of players to start a game
		MAXPLAYERS = 12,		//Maximum number of players allowed in the game
		TWOWOLVES = 8,			//Minimum number of players needed for 2 wolves
		
		//Final ints to describe the types of message that can be sent (Narration, game, control, notice), so
		//they can be coloured accordingly after being read from the file
		NOTICE = 1, NARRATION = 2, GAME = 3, CONTROL = 4;
		
	int dayTime = 90,			//time (in seconds) for daytime duration (variable because it changes with players)
		nightTime = 60,			//time (in seconds) for night duration
		voteTime = 30,			//time (in seconds) for the lynch vote
		joinTime = 60,			//MMBot: time (in seconds) for joining.
		seer,					//index of the player that has been nominated seer
		toSee = -1,				//index of the player the seer has selected to see. If no player, this is -1
		roundNo;				//holds the number of the current round
	int[] notVoted,				//holds the count of how many times players have not voted successively
		wasVoted;				//holds the count of how many votes one person has got.

	
	boolean connected = false,	//boolean to show if the bot is connected
		playing = false,		//boolean to show if the game is running
		day = false,			//boolean to show whether it's day or night
		gameStart = false,		//boolean to show if it's the start of the game (for joining)
		firstDay,				//boolean to show if it's the first day (unique day message)
		firstNight,				//boolean to show if it's the first night (unique night message)
		tieGame = true,			//boolean to determine if there will be a random tie break with tied votes.
		timeToVote,				//boolean to show whether it's currently time to vote
		debug,					//boolean to show if debug mode is one or off (Print to log files)
		doBarks,				//boolean to show if the bot should make random comments after long periods of inactivity
		rosterDisplay,			//MMBot: boolean to show roster while voting and at beginning of day.
		changeVote;				//MMBot: boolean to show if votes can be changed (true=on / false=off)
	
		
	boolean[] wolf,				//array for checking if a player is a wolf
		dead,					//array for checking if a player is dead
		voted,					//array to check which players have already voted
		abstain,				//MMBot: 1 if the vote is to abstain.
		finalvote;				//MMBot: 1 if the vote is final.
	
	Timer gameTimer,			//The game Timer (duh)
		reminderTimer,			//Reminder Timer for intermediate day/night messages
		idleTimer;				//timer to count idle time for random comments
	
	String name,			//The bot's name
		network,			//The network to connect to
		gameChan,			//The channel the game is played in
		ns,					//The nickname service nick
		command,			//The command the bot sends to the nickservice to identify on the network
		gameFile,			//Specifies the file name to read the game texts from.
		helpFile,			//MMBot: Specifies the file name to read the help/command texts from.
		role,
		oneWolf, manyWolves;
	long delay;				//The delay for messages to be sent
	
	//MMBot: Modifications
	String [] voteColors;   		//MMBot: Nice colors for different voted people :D
	boolean testing=false;			//MMBot: disable nick changes checks
	boolean isFirstDay=false;		//MMBot: Is game starting with first day?
	boolean storeTexts=false;		//MMBot: Read texts from file and cache them
	Hashtable cachedGameFile,		//MMBot: Game commands cached from game file
			cachedHelpFile;			//MMBot: Help commands cached from help file
	boolean firstDayTieisRandomLynch; //MMBot: First day tie is random lynch or no lynch
		
	
	//MMBot: Intermediary Timer types
	final int NOTIMER=0, JOINTIMER= 1, DAYTIMER = 2, VOTETIMER = 3, NIGHTTIMER = 4;	
	int interTimerType=NOTIMER;		//MMBot: Intermediate timers (reminders)
	int remainingTime=0;			//MMBot: Time from this reminder to event
	int reminderTime=60;			//MMBot: interval between reminders - from .ini
	
	//MMBot: 4-Spy Game
	final int SPYPLAYERS = 5; 	//MMBot: No of players for 4-Spy Game
	boolean spyMafia=false;		//MMBot: Boolean to show if spyMafia is selected.
	int[] roles;				//MMBot: Keep roles tidier
	int[] toSpy;				//MMBot: Need to keep all spy attempts in an array
	boolean[] lynched,			//MMBot: to show if a player was lynched 
								//(role is revealed in nightpost and voting roster)
		inactive;				//MMBot: to show if a player was remove from being inactive for 2 days in a row
								//(role is not revealed in post or voting roster)
	
	final int BADDIE=0,	SANE=1, INSANE=2, NAIVE=3, PARANOID=4;
	
	
	
	public Werewolf()
	{
		
		this.setLogin("MMBot");
		this.setVersion("Mafia Game Bot v0.6 for #MafiaManiac (based on Werewolf Game Bot)");
		this.setMessageDelay(100);
		
		String filename = "mmbot.ini",
			lineRead = "";
			gameFile = "mafia.txt";
			helpFile = "help.txt";
		FileReader reader;
		BufferedReader buff;
		
		try
		{
			reader = new FileReader(filename);
			buff = new BufferedReader(reader);
			
			while(!lineRead.startsWith("botname"))
				lineRead = buff.readLine();
			name = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("network"))
				lineRead = buff.readLine();
			network = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("channel"))
				lineRead = buff.readLine();
			gameChan = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("nickservice"))
				lineRead = buff.readLine();
			ns = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			
			while(!lineRead.startsWith("nickcmd"))
				lineRead = buff.readLine();
			command = lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length());
			
			while(!lineRead.startsWith("debug"))
				lineRead = buff.readLine();
				
			String onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				debug = true;
			else if (onoff.equalsIgnoreCase("off"))
				debug = false;
			else
			{
				System.out.println("Unknown debug value, defaulting to on.");
				debug = true;
			}
			
			while(!lineRead.startsWith("testing"))
				lineRead = buff.readLine();
				
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				testing = true;
			else if (onoff.equalsIgnoreCase("off"))
				testing = false;
			else
			{
				System.out.println("Unknown testing value, defaulting to on.");
				testing = true;
			}
			
			while(!lineRead.startsWith("storetexts"))
				lineRead = buff.readLine();
				
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				storeTexts = true;
			else if (onoff.equalsIgnoreCase("off"))
				storeTexts = false;
			else
			{
				System.out.println("Unknown storeTexts value, defaulting to off.");
				storeTexts = false;
			}
			
			while(!lineRead.startsWith("delay"))
				lineRead = buff.readLine();
				
			delay = Long.parseLong(lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length()));
			this.setMessageDelay(delay);
			
			while(!lineRead.startsWith("daytime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				dayTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad day time value; defaulting to 90 seconds");
				dayTime = 90;
			}
				
			while(!lineRead.startsWith("nighttime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				nightTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad night time value; defaulting to 45 seconds");
				nightTime = 45;
			}
				
			while(!lineRead.startsWith("votetime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				voteTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad vote time value; defaulting to 30 seconds");
				voteTime = 30;
			}
			//MMBot - Customized jointime
			while(!lineRead.startsWith("jointime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				joinTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad jointime value; defaulting to 60 seconds");
				joinTime = 60;
			}
			
			//MMBot - Customized remindertime
			while(!lineRead.startsWith("remindertime"))
				lineRead = buff.readLine();
			try
			{
				int tmp = Integer.parseInt(lineRead.substring(lineRead.indexOf("=") + 2, lineRead.length()));
				reminderTime = tmp;
			}
			catch(NumberFormatException nfx)
			{
				System.out.println("Bad remindertime value; defaulting to 60 seconds");
				reminderTime = 60;
			}
			
			while(!lineRead.startsWith("tie"))
				lineRead = buff.readLine();
				
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				tieGame = true;
			else if (onoff.equalsIgnoreCase("off"))
				tieGame = false;
			else
			{
				System.out.println("Unknown vote tie value, defaulting to on.");
				tieGame = true;
			}
			
			while(!lineRead.startsWith("idlebarks"))
				lineRead = buff.readLine();
			
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				doBarks = true;
			else if (onoff.equalsIgnoreCase("off"))
				doBarks = false;
			else
			{
				System.out.println("Unknown vote tie value, defaulting to off.");
				doBarks = true;
			}	
			//MMBot
			while(!lineRead.startsWith("changevote"))
				lineRead = buff.readLine();
			
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				changeVote = true;
			else if (onoff.equalsIgnoreCase("off"))
				changeVote = false;
			else
			{
				System.out.println("Unknown change vote option, defaulting to off.");
				changeVote = false;
			}
			//MMBot			
			while(!lineRead.startsWith("rosterdisplay"))
				lineRead = buff.readLine();
			
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				rosterDisplay = true;
			else if (onoff.equalsIgnoreCase("off"))
				rosterDisplay = false;
			else
			{
				System.out.println("Unknown roster display option, defaulting to off.");
				rosterDisplay = false;
			}	
			
			//MMBot- Firstday tie = lynch or not?			
			while(!lineRead.startsWith("firstdaytielynch"))
				lineRead = buff.readLine();
			
			onoff = lineRead.substring(lineRead.lastIndexOf(" ") + 1, lineRead.length());
			if(onoff.equalsIgnoreCase("on"))
				firstDayTieisRandomLynch = true;
			else if (onoff.equalsIgnoreCase("off"))
				firstDayTieisRandomLynch = false;
			else
			{
				System.out.println("Unknown first day lynch option, defaulting to tie option.");
				firstDayTieisRandomLynch = tieGame;
			}	
			
		}
		catch(FileNotFoundException fnfx)
		{
			System.err.println("Initialization file " + filename + " not found.");
			fnfx.printStackTrace();
			System.exit(1);
		}
		catch(IOException iox)
		{
			System.err.println("File read Exception");
			iox.printStackTrace();
			System.exit(1);
		}
		catch(Exception x)
		{
			System.err.println("Other Exception caught");
			x.printStackTrace();
			System.exit(1);
		}
		
		if(debug)
		{
			this.setVerbose(true);
			try
			{
				File file = new File("MMBotOut.log");
				if(!file.exists())
					file.createNewFile();
				PrintStream fileLog = new PrintStream(new FileOutputStream(file, true));
				System.setOut(fileLog);
				System.out.println((new Date()).toString());
				System.out.println("Starting log....");
				
				File error = new File("MMBotErr.log");
				if(!file.exists())
					file.createNewFile();
				PrintStream errorLog = new PrintStream(new FileOutputStream(error, true));
				System.setErr(errorLog);
				System.err.println((new Date()).toString());
				System.err.println("Starting error log....");
				
			}
			catch(FileNotFoundException fnfx)
			{
				fnfx.printStackTrace();
			}
			catch(IOException iox)
			{
				iox.printStackTrace();
			}
		}
		//MMBot init colors for voting - 12 at the most :D
		voteColors= new String[12];
		int i=0;
		voteColors[i++]=Colors.BLUE;
		voteColors[i++]=Colors.TEAL;
		voteColors[i++]=Colors.PURPLE;
		voteColors[i++]=Colors.BROWN;
		
		voteColors[i++]=Colors.RED;
		voteColors[i++]=Colors.GREEN;
		voteColors[i++]=Colors.YELLOW;
		voteColors[i++]=Colors.MAGENTA;
		
		voteColors[i++]=Colors.DARK_BLUE;
		voteColors[i++]=Colors.OLIVE;
		voteColors[i++]=Colors.DARK_GREEN;
		voteColors[i++]=Colors.BLACK;
		
		if(storeTexts) cacheTextFiles();
		connectAndJoin();
		startIdle();
	}
	//MMBot: Refresh cache texts from Game Files
	protected void refreshCacheGameFile()
	{
		FileReader reader;
		BufferedReader buff;
		
		try
		{
			reader = new FileReader(gameFile);
			buff = new BufferedReader(reader);
			String lineRead = "";
			String commandRead = "";

			while(buff.ready())
			{
				commandRead = buff.readLine();
		
				if(!buff.ready()) break;
				
				Vector texts = new Vector(1, 1);
				lineRead = buff.readLine();
					
				while(!lineRead.equals(""))
				{
					texts.add(lineRead);
					lineRead = buff.readLine();
				}
				cachedGameFile.put(commandRead,texts);
			}
			buff.close();
			reader.close();
		
		}
		catch(Exception x)
		{
			x.printStackTrace();
		}

	}
	//MMBot: Cache texts from Help and Game Files
	protected void cacheTextFiles()
	{
		//cachedGameFile
		FileReader reader;
		BufferedReader buff;
		cachedGameFile = new Hashtable();
		cachedHelpFile = new Hashtable();
		
		refreshCacheGameFile();
		
		//cachedHelpFile;
		try
		{
			reader = new FileReader(helpFile);
			buff = new BufferedReader(reader);
			String lineRead = "";
			String commandRead = "";

			while(buff.ready())
			{
				commandRead = buff.readLine();
		
				if(!buff.ready()) break;
				
				Vector texts = new Vector(1, 1);
				lineRead = buff.readLine();
					
				while(!lineRead.equals(""))
				{
					texts.add(lineRead);
					lineRead = buff.readLine();
				}
				cachedHelpFile.put(commandRead,texts);
			}
			buff.close();
			reader.close();
		}
		catch(Exception x)
		{
			x.printStackTrace();
		}
	}
	//overloaded method that calls the method with 2 player names
	protected String getFromFile(String text, String player, int time, int type)
	{
		return getFromFile(text, player, null, time, type);
	}
	//MMBot: Add colors and formating to game texts
	protected String fineTuneGameTexts(String toSend, String player, String player2, int time, int type)
	{
		switch(type)
		{
			case NOTICE:	//no colour formatting
				toSend = toSend.replaceAll("PLAYER", player);
				toSend = toSend.replaceAll("PLAYR2", player2);
				toSend = toSend.replaceAll("TIME", "" + time);
				toSend = toSend.replaceAll("ISAWOLF?", role);
				if(wolves != null && !wolves.isEmpty())
					toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
				toSend = toSend.replaceAll("BOTNAME", this.getNick());
				toSend = toSend.replaceAll("ROLE", role);						
				return toSend;
				//break;
			
			case NARRATION:	//blue colour formatting
				toSend = toSend.replaceAll("PLAYER",
					Colors.DARK_BLUE + Colors.BOLD + player + Colors.NORMAL + Colors.DARK_BLUE);
				toSend = toSend.replaceAll("PLAYR2",
					Colors.DARK_BLUE + Colors.BOLD + player2 + Colors.NORMAL + Colors.DARK_BLUE);
				toSend = toSend.replaceAll("TIME",
					Colors.DARK_BLUE + Colors.BOLD + time +	Colors.NORMAL + Colors.DARK_BLUE);
				if(wolves != null && !wolves.isEmpty())
					toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
				toSend = toSend.replaceAll("BOTNAME", this.getNick());
				toSend = toSend.replaceAll("ROLE", role);
				return Colors.DARK_BLUE + toSend;
				//break;
				
			case GAME:		//red colour formatting
				toSend = toSend.replaceAll("PLAYER",
					Colors.BROWN + Colors.UNDERLINE + player + Colors.NORMAL + Colors.RED);
				toSend = toSend.replaceAll("PLAYR2",
					Colors.BROWN + Colors.UNDERLINE + player2 + Colors.NORMAL + Colors.RED);
				toSend = toSend.replaceAll("TIME",
					Colors.BROWN + Colors.UNDERLINE + time + Colors.NORMAL + Colors.RED);
				if(wolves != null && !wolves.isEmpty())
					toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
				toSend = toSend.replaceAll("BOTNAME", this.getNick());
				toSend = toSend.replaceAll("ROLE", role);
				return Colors.RED + toSend;
				//break;
				
			case CONTROL:	//Green colour formatting
				toSend = toSend.replaceAll("PLAYER",
					Colors.DARK_GREEN + Colors.UNDERLINE + player + Colors.NORMAL + Colors.DARK_GREEN);
				toSend = toSend.replaceAll("PLAYR2",
					Colors.DARK_GREEN + Colors.UNDERLINE + player2 + Colors.NORMAL + Colors.DARK_GREEN);
				toSend = toSend.replaceAll("TIME",
					Colors.DARK_GREEN + Colors.UNDERLINE + time + Colors.NORMAL + Colors.DARK_GREEN);
				if(wolves != null && !wolves.isEmpty())
					toSend = toSend.replaceAll("WOLF", (wolves.size() == 1 ? oneWolf : manyWolves));
				toSend = toSend.replaceAll("BOTNAME", this.getNick());
				toSend = toSend.replaceAll("ROLE", role);
				return Colors.DARK_GREEN + toSend;
				//break;
				
			default:
				return null;
		}
	}
	//a couple of game string need 2 player names, so this is the ACTUAL method
	protected String getFromFile(String text, String player, String player2, int time, int type)
	{
		
		FileReader reader;
		BufferedReader buff;
		String toSend="";
		Vector texts;
		
		try
		{
			if(storeTexts==false)
			{
				reader = new FileReader(gameFile);
				buff = new BufferedReader(reader);
				String lineRead = "";
	
				while(!lineRead.equals(text))
				{
					lineRead = buff.readLine();
				}
				
				texts = new Vector(1, 1);
				lineRead = buff.readLine();
					
				while(!lineRead.equals(""))
				{
					texts.add(lineRead);
					lineRead = buff.readLine();
				}
				
				buff.close();
				reader.close();

			}
			else
			{//read from cache Hashtable.
				texts = (Vector)cachedGameFile.get(text);	
			}
			
			int rand = (int) (Math.random() * texts.size());
			toSend = (String)texts.get(rand);
			
			if(texts.size() > 0)
			{
				return fineTuneGameTexts(toSend,player, player2, time, type);
			}
		}
		catch(Exception x)
		{
			x.printStackTrace();
		}
		
		return null;
	}
	//MMBot: Read help lines from file "help.txt"
	protected String getHelpFromFile(String text)
	{
		FileReader reader;
		BufferedReader buff;
		String toSend="";
		Vector texts;
		
		try
		{
			if(storeTexts==false)
			{
				reader = new FileReader(helpFile);
				buff = new BufferedReader(reader);
				String lineRead = "";
	
				while(!lineRead.equals(text))
				{
					lineRead = buff.readLine();
				}
				
				texts = new Vector(1, 1);
				lineRead = buff.readLine();
					
				while(!lineRead.equals(""))
				{
					texts.add(lineRead);
					lineRead = buff.readLine();
				}
				
				buff.close();
				reader.close();
			}
			else //read from cache
			{
				texts = (Vector)cachedHelpFile.get(text);	
			}
			
		
			if(texts.size() > 0)
			{
				int rand = (int) (Math.random() * texts.size());
				toSend = (String)texts.get(rand);
				
				toSend=toSend.replaceAll("!start",
						Colors.BROWN+"!start"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!spymafia",
						Colors.BROWN+"!spymafia"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll("!wolfgame",
						Colors.BROWN+"!wolfgame"+Colors.DARK_GREEN );	
				toSend=toSend.replaceAll("!star-trek",
						Colors.BROWN+"!star-trek"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!rules",
						Colors.BROWN+"!rules"+Colors.DARK_GREEN );	
				toSend=toSend.replaceAll("!howto",
						Colors.BROWN+"!howto"+Colors.DARK_GREEN );
				
				toSend=toSend.replaceAll("NAIVE",
						Colors.MAGENTA+"NAIVE"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll("INSANE",
						Colors.MAGENTA+"INSANE"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll(" SANE",
						Colors.MAGENTA+" SANE"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll("PARANOID",
						Colors.MAGENTA+"PARANOID"+Colors.DARK_GREEN);
				
				
				boolean publicCommandHelp=toSend.contains("HOWTO ");
				boolean privateCommandHelp=toSend.contains("HOWTOPRV ");
				boolean genericCommandHelp=toSend.contains("HOWTOGEN ");
				
				if(publicCommandHelp)
				{
					toSend=toSend.replaceAll("HOWTO ",
							Colors.BROWN);
					toSend=toSend.replaceAll("Channel OPs only",
								Colors.RED+"Channel OPs only"+Colors.DARK_GREEN);
				}
				
				//quit, stop, daytime (x), nighttime (x), votetime (x),
				//jointime (x), remindertime (x), tie (on/off), changevote (on/off), 
				//roster (on/off), shush or speak
				toSend=toSend.replaceAll("!quit",
						Colors.BROWN+"!quit"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!stop",
						Colors.BROWN+"!stop"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll("!daytime",
						Colors.BROWN+"!daytime"+Colors.DARK_GREEN );	
				toSend=toSend.replaceAll("!nighttime",
						Colors.BROWN+"!nighttime"+Colors.DARK_GREEN);
				toSend=toSend.replaceAll("!votetime",
						Colors.BROWN+"!votetime"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!jointime",
						Colors.BROWN+"!jointime"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!remindertime",
						Colors.BROWN+"!remindertime"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!tie",
						Colors.BROWN+"!tie"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!firstdaytielynch",
						Colors.BROWN+"!firstdaytielynch"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!changevote",
						Colors.BROWN+"!changevote"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!roster",
						Colors.BROWN+"!roster"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!shush",
						Colors.BROWN+"!shush"+Colors.DARK_GREEN );
				toSend=toSend.replaceAll("!speak",
						Colors.BROWN+"!speak"+Colors.DARK_GREEN );
				
				
				if(privateCommandHelp)
				{
					toSend=toSend.replaceAll("- ",
							"- " + Colors.DARK_GREEN);
					toSend=toSend.replaceAll("HOWTOPRV ",
							Colors.BROWN);
					toSend=toSend.replaceAll("OPTIONS",
							Colors.BLUE+"OPTIONS"+Colors.BROWN);
					toSend=toSend.replaceAll("GAMENAME",
							Colors.BLUE+"GAMENAME"+Colors.BROWN);
					toSend=toSend.replaceAll("!COMMAND",
							Colors.BLUE+"!COMMAND"+Colors.BROWN);
					toSend=toSend.replaceAll(" COMMAND",
							Colors.BLUE+" COMMAND"+Colors.BROWN);
					toSend=toSend.replaceAll("<player>",
							Colors.BLUE+"<player>"+Colors.BROWN);
					toSend=toSend.replaceAll("<number>",
							Colors.BLUE+"<number>"+Colors.BROWN);
					toSend=toSend.replaceAll("<number in roster>",
							Colors.BLUE+"<number in roster>"+Colors.BROWN);
					toSend=toSend.replaceAll(" x ",
							Colors.BLUE+"x"+Colors.BROWN);
					toSend=toSend.replaceAll("(on/off)",
							Colors.BLUE+"on/off"+Colors.BROWN);
					toSend=toSend.replaceAll("<botname>",
							Colors.RED+this.name+Colors.BROWN);
				}
				else
				{
					//Blue replacement
					toSend=toSend.replaceAll("OPTIONS",
							Colors.BLUE+"OPTIONS"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("GAMENAME",
							Colors.BLUE+"GAMENAME"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("!COMMAND",
							Colors.BLUE+"!COMMAND"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll(" COMMAND",
							Colors.BLUE+" COMMAND"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("<player>",
							Colors.BLUE+"<player>"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("<number in roster>",
							Colors.BLUE+"<number in roster>"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("<number>",
							Colors.BLUE+"<number>"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll(" x ",
							Colors.BLUE+"x"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("(on/off)",
							Colors.BLUE+"on/off"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("<botname>",
							Colors.RED+this.name+Colors.DARK_GREEN);
				}

				if(genericCommandHelp)
				{
					toSend=toSend.replaceAll("HOWTOGEN ",
							Colors.DARK_GREEN);
				
					//PrivateMSG colors
					//kill <player> , spy <player> (or see <player>), alive or role.
					toSend=toSend.replaceAll("kill ",
							Colors.BROWN+"kill "+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("spy ",
							Colors.BROWN+"spy "+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("or see ",
							"or "+Colors.BROWN+"see "+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("killno",
							Colors.BROWN+"killno"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("spyno",
							Colors.BROWN+"spyno"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("or seeno",
							"or "+Colors.BROWN+"seeno"+Colors.DARK_GREEN );	
					toSend=toSend.replaceAll("alive",
							Colors.BROWN+"alive"+Colors.DARK_GREEN);
					toSend=toSend.replaceAll("role",
							Colors.BROWN+"role"+Colors.DARK_GREEN );
					
					//join, vote <player> , voteno <number in roster>, 
					//removevote, abstain, finalvote, nocontest, showvotes ,
					toSend=toSend.replaceAll("join,",
							Colors.BROWN+"join"+Colors.DARK_GREEN+"," );
					toSend=toSend.replaceAll(", vote ",
							", "+Colors.BROWN+"vote "+Colors.DARK_GREEN );
					toSend=toSend.replaceAll(" voteno",
							Colors.BROWN+" voteno"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("removevote",
							Colors.BROWN+"removevote"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("abstain",
							Colors.BROWN+"abstain"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("finalvote ",
							Colors.BROWN+"finalvote "+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("finalvoteno",
							Colors.BROWN+"finalvoteno"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("finalvote,",
							Colors.BROWN+"finalvote"+Colors.DARK_GREEN+"," );
					toSend=toSend.replaceAll("nocontest",
							Colors.BROWN+"nocontest"+Colors.DARK_GREEN );
					toSend=toSend.replaceAll("showvotes",
							Colors.BROWN+"showvotes"+Colors.DARK_GREEN );
				}	
			}
			else toSend = "Sorry, someone forgot that text message: "+text
				+"Please report it to an OP. Thanks!";
			
			return toSend;

		}
		catch(Exception x)
		{
			x.printStackTrace();
		}
		
		return null;
	}
	//MMBot: JOIN
	protected void actionJoin(String sender, String message)
	{
		if(isInChannel(sender))
		{
			if(!isNameAdded(sender)) //has the player already joined?
			{

				if(players.add(sender)) //add another one
				{
					this.setMode(gameChan, "+v " + sender);
					
					this.sendMessage(gameChan, getFromFile("JOIN", sender, 0, NARRATION));
					
					this.sendNotice(sender,
						getFromFile("ADDED", null, 0, NOTICE));
					if( 
							!
							//spyMafia only needs exactly SPYPLAYERS=5
							(
									(spyMafia==true && players.size()<SPYPLAYERS)
									||
							//for non-spyMafia if there are less than MAXPLAYERS player joined
									(spyMafia==false && players.size() < MAXPLAYERS) 
							)	
						) 
						//Stop joining if maximum players have been reached
					{
							reminderTimer.cancel();
							gameTimer.cancel();
							reminderTimer=new Timer();
							gameTimer=new Timer();
							interTimerType=NOTIMER;
							remainingTime=0;
							this.sendNotice(gameChan,
									getFromFile("GAME-IS-FULL", null, 0, NOTICE));
							gameTimer.schedule(new WereTask(), 500);
					}
				}
				else	//let the user know the adding failed, so he can try again
					this.sendNotice(sender,
							getFromFile("ADD-FAILED", null, 0, NOTICE));
				
				
				//edited out priority lists.
				/*else 
				if(priority.size() < MAXPLAYERS) //if play list is full, add to priority list
				{
					if(priority.add(sender))
						this.sendNotice(sender,
							"Sorry, the maximum players has been reached. You have been placed in the " +
							"priority list for the next game.");
					else
						this.sendNotice(sender,
							"Could not add you to priority list. Please try again.");
				}
				else //if both lists are full, let the user know to wait for the current game to end
				{
					this.sendNotice(sender,
						"Sorry, both player and priority lists are full. Please wait for the " +
						"the current game to finish before trying again.");
				}*/
			}
			else //if they have already joined, let them know
			{
				this.sendNotice(sender,
						getFromFile("ALREADY-PLAYING", sender, null, 0, NOTICE));
			}
		}
	}
	//MMBot: verify sender and choice for voting
	protected boolean verifyVote(String sender, int senderPos, String choice, int choicePos)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return false;
		}
		
		if (choicePos<0||choicePos>=players.size()) //not a valid choice 
		{
			this.sendNotice(sender,
					getFromFile("CHOICE-NOT-PLAYING", sender, null, 0, NOTICE));
			return false;
		}
		
		if (dead[senderPos])//cannot vote if dead
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-VOTE-DEAD", sender, null, 0, NOTICE));
			return false;
		}
			
		if (dead[choicePos])//cannot vote dead people
		{
			//CANNOT-VOTE-DEAD-PEOPLE
			this.sendNotice(sender,
					getFromFile("CANNOT-VOTE-DEAD-PEOPLE", sender, null, 0, NOTICE));
			return false;
		}
		
		if( changeVote==false && hasVoted(senderPos))
		{
			this.sendNotice(sender,
							getFromFile("ALREADY-VOTED", sender, null, 0, NOTICE));
			return false;
		}
		
		if( changeVote==true && finalvote[senderPos])
		{
			this.sendNotice(sender,
						getFromFile("ALREADY-FINALIZED-VOTE", sender, null, 0, NOTICE));
			return false;
		}
		return true;
	}
	//MMBot: actual vote placed
	protected void placeVote(String sender, int senderPos, String choice, int choicePos, boolean isFinalVote)
	{
		if(verifyVote(sender,senderPos,choice,choicePos)==false) return;
		
		//placeVote(sender, senderPos, choice, getPlayerPosition (choice), isFinalVote);
		
		Vote vote = new Vote(sender, choice);
			
		//Need to check if vote already exists and remove if necessary
		for(int k = 0 ; k < votes.size() ; k++)
		{
			if(sender.equalsIgnoreCase(((Vote)votes.get(k)).getName()))
			{
				votes.remove(k);
				//break;
			}
		}
		
		while(!votes.add(vote));
						
		voted[senderPos] = true;
		//negate any previous abstain
		abstain[senderPos]=false;
		notVoted[senderPos] = 0;
		
		this.sendMessage(gameChan,
							getFromFile("HAS-VOTED", sender, choice, 0, NARRATION));
		if (isFinalVote)
		{
			finalvote[senderPos]=true;
			this.sendMessage(gameChan,
					getFromFile("HAS-FINALIZED", sender, null, 0, NARRATION));
		}
		
		printRoster(true);
		if(allVotesFinal())
		{
			endDayEarlier();
			return;
		}

	}
	//MMBot: VOTE	
	protected void actionVote(String sender, int senderPos, String message)
	{
		if(changeVote==false && hasVoted(senderPos))//if player is unable to change vote
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-VOTED", sender, null, 0, NOTICE));
			return;
		}
		
		try
		{
			String choice = message.substring(message.indexOf(" ") + 1, message.length());
			choice.trim();
			//remove any case errors in writing the name.
			choice = correctNamePlaying(choice);	
			placeVote(sender, senderPos, choice, getPlayerPosition (choice), false);
		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("VOTE-INSTRUCTIONS", sender, null, 0, NOTICE));
			x.printStackTrace();
		}
		
	}
	//MMBot: VOTENO
	protected void actionVoteNo(String sender, int senderPos, String message)
	{
		if(changeVote==false && hasVoted(senderPos))//if player is unable to change vote
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-VOTED", sender, null, 0, NOTICE));
			return;
		}
		try
		{
			String choice = message.substring(message.indexOf(" ") + 1, message.length());
			choice.trim();
			int choiceInt = Integer.parseInt(choice);
			//humans start at 1 :facepalm:
			choiceInt--; 
			
			choice = getPlayerName(choiceInt);
			placeVote(sender, senderPos, choice, choiceInt, false);
		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("VOTE-INSTRUCTIONS", sender, null, 0, NOTICE));
			x.printStackTrace();
		}
	}
	//MMBot: FINALVOTE
	protected void actionFinalVote(String sender, int senderPos, String message)
	{
		if(changeVote==false && hasVoted(senderPos))//if player is unable to change vote
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-VOTED", sender, null, 0, NOTICE));
			return;
		}
		
		try
		{
			boolean rosterVote=false;
			boolean actualVote=false;
			if (message.toLowerCase().startsWith("finalvoteno "))
				rosterVote=true;
			
			//if another parameter then actual vote follows
			if (message.toLowerCase().startsWith("finalvote "))
				actualVote=true;
			
			String choice = message.substring(message.indexOf(" ") + 1, message.length());
			choice.trim();
			int choicePos=-1;
			
			if(rosterVote)
			{
				choicePos = Integer.parseInt(choice);
				//humans start at 1 :facepalm:
				choicePos--;
				choice = getPlayerName(choicePos);
				placeVote(sender, senderPos, choice, choicePos, true);
			}
			else if(actualVote)
			{
				choice=correctNamePlaying(choice);
				choicePos=getPlayerPosition(choice);
				placeVote(sender, senderPos, choice, choicePos, true);
			}
			else
			{
				//just finalize, no actual vote
				//if(verifyVote(sender,senderPos,choice,choicePos)==false) return;
		
				if (senderPos<0) //Sender is not playing 
				{
					this.sendNotice(sender,
							getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
					return;
				}
				
				if (dead[senderPos])//cannot vote if dead
				{
					this.sendNotice(sender,
							getFromFile("CANNOT-VOTE-DEAD", sender, null, 0, NOTICE));
					return;
				}
				
				if(!hasVoted(senderPos))
				{
					this.sendNotice(sender,
						getFromFile("HAVE-NOT-VOTED-YET", sender, null, 0, NOTICE));
					return;
				}
				
				finalvote[senderPos]=true;
				
				this.sendMessage(gameChan,
						getFromFile("HAS-FINALIZED", sender, null, 0, NARRATION));
				printRoster(true);	
				if(allVotesFinal())
				{
					endDayEarlier();
				}
				return;
			}
		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("VOTE-INSTRUCTIONS", sender, null, 0, NOTICE));
			x.printStackTrace();
		}
	}
	//MMBot: NOCONTEST
	protected void actionPleadNoContest(String sender, int senderPos, String message)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		
		if (dead[senderPos])//cannot vote if dead
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-VOTE-DEAD", sender, null, 0, NOTICE));
			return;
		}
		this.sendMessage(gameChan,
				getFromFile("PLAYER-HAS-PLEADED-NO-CONTEST", sender, null, 0, NOTICE));

		//rig votes
		votes = new Vector(1, 1);
		
		Vote riggedVote;
		
		for(int j = 0 ; j < players.size() ; j++)
		{
			if(players.get(j) != null && !dead[j])
			{
				riggedVote = new Vote((String)players.get(j), sender);
				abstain[j]=false;
				finalvote[j]=true;
				voted[j]=true;
				notVoted[j]=0;
				votes.add(riggedVote);
			}
		}
		printRoster(true);
		endDayEarlier();		
	}
	//MMBot: REMOVEVOTE
	protected void actionRemoveVote(String sender, int senderPos)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		
		if (dead[senderPos])//cannot vote if dead
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-VOTE-DEAD", sender, null, 0, NOTICE));
			return;
		}
		if(changeVote==false)		
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-REMOVE-VOTE", sender, null, 0, NOTICE));
			return;
		}
		if(!hasVoted(senderPos))		
		{
			this.sendNotice(sender,
					getFromFile("HAVE-NOT-VOTED-BEFORE", sender, null, 0, NOTICE));
			return;
		}
		
		if(finalvote[senderPos])
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-FINALIZED-VOTE", sender, null, 0, NOTICE));
			return;
		}
		
		try
		{
			
			//Need to check and remove if vote already exists
			for(int k = 0 ; k < votes.size() ; k++)
			{
				if(sender.equalsIgnoreCase(((Vote)votes.get(k)).getName()))
				{
					//thisVote = (Vote)votes.get(k);
					votes.remove(k);
					voted[senderPos] = false;
					this.sendMessage(gameChan,
						getFromFile("HAS-REMOVED-VOTE", sender, null, 0, NARRATION));
					printRoster(true);
					return;
				}
			}
			
			//not voted, then check for abstain
			if(abstain[senderPos])
			{
				abstain[senderPos]=false;
				voted[senderPos]=false;
				this.sendMessage(gameChan,
					getFromFile("HAS-REMOVED-ABSTAIN", sender, null, 0, NARRATION));
										printRoster(true);
										return;
			}
			
			//redundant
			this.sendNotice(sender,
					getFromFile("HAVE-NOT-VOTED-BEFORE", sender, null, 0, NOTICE));
			return;
										
		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("VOTE-INSTRUCTIONS", sender, null, 0, NOTICE));
			x.printStackTrace();
		}
	}
	//MMBot: ABSTAIN
	protected void actionAbstain(String sender, int senderPos)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		
		if (dead[senderPos])//cannot vote if dead
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-VOTE-DEAD", sender, null, 0, NOTICE));
			return;
		}
		
		if(changeVote==false && hasVoted(senderPos))
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-VOTED", sender, null, 0, NOTICE));
			return;			
		}
		
		if(finalvote[senderPos])
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-FINALIZED-VOTE", sender, null, 0, NOTICE));
			return;
		}
		
		if(abstain[senderPos])
		{
			this.sendNotice(sender,
					getFromFile("ALREADY-ABSTAIN", sender, null, 0, NOTICE));
			return;
		}
		try
		{
			if(changeVote==true) 
			{
				//Need to check and remove if vote already exists
				for(int k = 0 ; k < votes.size() ; k++)
				{
					if(sender.equalsIgnoreCase(((Vote)votes.get(k)).getName()))
					{
						votes.remove(k);
						voted[senderPos] = false;
					}
				}
			}
			
			abstain[senderPos]=true;
			voted[senderPos] = true;
			//WARNING: abstain does not count as no vote=inactivity
			notVoted[senderPos] = 0;
			this.sendMessage(gameChan,
						getFromFile("HAS-ABSTAINED", sender, null, 0, NARRATION));
			printRoster(true);
			
			if(allVotesFinal())
			{
				endDayEarlier();
			}
			return;
			
		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("VOTE-INSTRUCTIONS", sender, null, 0, NOTICE));
			x.printStackTrace();
		}	
	}
	//MMBot: SPY / SEE
	protected void actionSpy(String sender, int senderPos, String message)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		
		try
		{
			//check if sender is seer
			if(spyMafia==false)
			{
				if(seer!=senderPos)
				{
					this.sendNotice(sender,
							getFromFile("NOT-SEER", null, 0, NOTICE));
					return;
				}
			}
			else //spyMafia==true
			{
				if(roles[senderPos]==BADDIE)
				{
						this.sendNotice(sender, getFromFile("CANNOT-SPY-WOLF", null, 0, NOTICE));
						return;
				}
			}
			
			if (dead[senderPos])//cannot spy if dead
			{
				this.sendNotice(sender,
						getFromFile("SEER-DEAD", sender, null, 0, NOTICE));
				return;
			}
			
			//get choice	
			boolean rosterSpy=false;
			if (message.toLowerCase().startsWith("spyno")
					||message.toLowerCase().startsWith("seeno"))
				rosterSpy=true;
			
			String choice = message.substring(message.indexOf(" ") + 1, message.length());
			choice.trim();
			
			int choicePos=-1;
			
			if(rosterSpy)
			{
				choicePos = Integer.parseInt(choice);
				//humans start at 1 :facepalm:
				choicePos--;
				choice = getPlayerName(choicePos);
			}
			else 
			{
				choice=correctNamePlaying(choice);
				choicePos=getPlayerPosition(choice);
			}
			
			if (choicePos<0||choicePos>=players.size()) //not a valid choice 
			{
				this.sendNotice(sender,
						getFromFile("CANNOT-SPY-CHOICE", sender, null, 0, NOTICE));
				return;
			}
			
			if (dead[choicePos])//cannot spy dead people
			{
				this.sendNotice(sender,
						getFromFile("CANNOT-SPY-DEAD-PEOPLE", sender, null, 0, NOTICE));
				return;
			}
			
			if(spyMafia==false &&choicePos==senderPos)
			{
				this.sendNotice(sender, 
						getFromFile("CANNOT-SPY-YOURSELF", null, 0, NOTICE));
				return;
			}
			
			if(spyMafia==false)
			{
				toSee=choicePos;
			}
			else //SpyMafia
			{
				toSpy[senderPos]=choicePos;
			}

			this.sendNotice(sender, 
						getFromFile("WILL-SEE", choice, 0, NOTICE));									

		}
		catch(Exception x)
		{
			this.sendNotice(sender,
					getFromFile("CANNOT-SPY-CHOICE", null, 0, NOTICE));
			x.printStackTrace();
		}
	}
	//MMBot: Request ROLE reminder
	protected void requestRole(String sender, int senderPos)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		try
		{
			if(wolf[senderPos])
			{
				if(spyMafia==true)
				{
					this.sendNotice(sender,	getFromFile("W-ROLE", null, 0, NOTICE));
					return;
				}
				else if(wolves.size() == 1)
				{
					this.sendNotice(sender,	getFromFile("W-ROLE", null, 0, NOTICE));
					return;
				}
				else
				{
					for(int j = 0 ; j < wolves.size() ; j++)
					{
						if(!sender.equals(wolves.get(j)))
						{
							this.sendNotice(sender,
									getFromFile("WS-ROLE", (String) wolves.get(j), 0, NOTICE));
							return;
						}
					}
				}
			}
			else 
			{
				if(spyMafia==false)
				{
					if (senderPos == seer)
						this.sendNotice(sender,	getFromFile("S-ROLE", null, 0, NOTICE));
					else
						this.sendNotice(sender,	getFromFile("V-ROLE", null, 0, NOTICE));
				}
				else//spyMafia
				{
					this.sendNotice(sender,	getFromFile("S-EXT-ROLE", null, 0, NOTICE));
				}
			}
		}
		catch(Exception x)
		{	
			this.sendNotice(sender,
					getFromFile("CANNOT-SPY-CHOICE", null, 0, NOTICE));
			x.printStackTrace();
		}
	}
	//MMBot: KILL
	protected void actionKill(String sender,int senderPos, String message)
	{
		if (senderPos<0) //Sender is not playing 
		{
			this.sendNotice(sender,
					getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
			return;
		}
		
		boolean isWolf = false;
		
		if(spyMafia==false)
		{
			for(int i = 0 ; i < wolves.size() ; i++)
			{
				if(wolves.get(i).equals(sender))
				isWolf = true;
			}
		}
		else//spyMafia
		{
			if(roles[senderPos]==BADDIE)
				isWolf=true;
		}
		
		if(!isWolf)
		{
			this.sendNotice(sender, getFromFile("NOT-WOLF", null, 0, NOTICE));
			return;
		}
		
		try
		{
			if (dead[senderPos])//cannot kill if dead
			{
				this.sendNotice(sender,
						getFromFile("WOLF-DEAD", sender, null, 0, NOTICE));
				return;
			}
			
			//get choice (victim)
			boolean rosterChoice=message.startsWith("killno ");
			
			String choice = message.substring(message.indexOf(" ") + 1, message.length());
			choice.trim();
			int choicePos=-1;
			
			if(rosterChoice)
			{
				choicePos = Integer.parseInt(choice);
				//humans start at 1 :facepalm:
				choicePos--;
				choice = getPlayerName(choicePos);
			}
			else 
			{
				choicePos=getPlayerPosition(choice);
			}
			
			if(choicePos<0||choicePos>=players.size())
			{
				this.sendNotice(sender,										
					getFromFile("WOLF-WRONG-CHOICE", null, 0, NOTICE));
				return;
			}
			
			if(dead[choicePos])
			{
				this.sendNotice(sender,										
					getFromFile("CANNOT-KILL-DEAD", null, 0, NOTICE));
				return;
			}

			if(choicePos==senderPos)
			{
				this.sendNotice(sender,										
					getFromFile("CANNOT-KILL-YOURSELF", null, 0, NOTICE));
				return;
			}
			
			
			addKill(sender,choice);
		
							
			if(wolves.size() == 1||spyMafia==true)
			{
				this.sendNotice(sender,
					getFromFile("WOLF-CHOICE", correctNamePlaying(choice), 0, NOTICE));
			}
			else
			{	
				this.sendNotice(sender,
						getFromFile("WOLVES-CHOICE", choice, 0, NOTICE));
									
				for(int i = 0 ; i < wolves.size() ; i++)
				{
					if(!((String)wolves.get(i)).equals(sender))
						this.sendNotice((String)wolves.get(i),
								getFromFile("WOLVES-CHOICE-OTHER", sender, correctNamePlaying(choice), 0, NOTICE));
				}
			}
			
		}
		catch(Exception x)
		{
				x.printStackTrace();
				this.sendNotice(sender,										
						getFromFile("WOLF-WRONG-CHOICE", null, 0, NOTICE));
				
		}
	}
	//react on private message received
	protected void onPrivateMessage(String sender, String login, String hostname, String message)
	{
		message=Colors.removeFormattingAndColors(message);
		if(!playing)
		{
			if(!sender.startsWith("NickServ")&&
					   !sender.startsWith("WikkedWire")) 
				this.sendNotice(sender,
					getFromFile("NOGAME-IN-PROGRESS", sender, null, 0, NOTICE));
			return;
		}
		
		if(playing) //commands only work if the game is on
		{
			if(message.toLowerCase().equalsIgnoreCase("join")) //join the game
			{
	//JOIN
				if(gameStart)
				{
					actionJoin(sender, message);
				}
				else //if the game is already running, don't add anyone else to either list
				{
					this.sendNotice(sender,
							getFromFile("GAME-PLAYING", sender, null, 0, NOTICE));
				}
				return;
			}
			
			
			int senderPos=-1;
			if(isNamePlaying(sender))
			{
				senderPos = getPlayerPosition(sender);			
			}
			else
			{			
				if(!sender.startsWith("NickServ")&&
						   !sender.startsWith("WikkedWire")) 
					this.sendNotice(sender,
						getFromFile("NOT-PLAYING", sender, null, 0, NOTICE));
				//else don't spam them!
				return;
			}
				
			if(timeToVote) //commands for when it's vote time
			{
				if(message.toLowerCase().startsWith("vote ")) //vote to lynch someone
				{
	//VOTE
					actionVote(sender, senderPos, message.toLowerCase());
					return;
				}
				else if(message.toLowerCase().startsWith("voteno ")) //vote to lynch a number
				{
	//VOTE NO.
					actionVoteNo(sender, senderPos, message.toLowerCase());
					return;
						
				}
				else if(message.toLowerCase().startsWith("removevote")) //remove vote to lynch someone
				{
	//REMOVEVOTE
					actionRemoveVote(sender, senderPos);
					return;
				}
				else if(message.toLowerCase().startsWith("showvotes"))//display roster in private.
				{
	//SHOWVOTES
					try{
						if(rosterDisplay) printRoster(true);
						else 
						{
							this.sendNotice(sender,
									getFromFile("ROSTER-DISPLAY-OFF-NOTICE", sender, null, 0, NOTICE));
							return;
						}								
					}
					catch (Exception x)
					{
						this.sendNotice(sender,
								getFromFile("ROSTER-DISPLAY-PROBLEM", sender, null, 0, NOTICE));
						x.printStackTrace();
						return;
					}
				}
				else if(message.toLowerCase().startsWith("abstain")) 
				{
	//ABSTAIN
					actionAbstain(sender,senderPos);
					return;
				}
				else if(message.toLowerCase().startsWith("finalvote")) 
				{
	//FINALVOTE
					actionFinalVote(sender, senderPos, message);
					return;
				}
				else if(message.toLowerCase().startsWith("nocontest")) //plead no contest
				{
	//NOCONTEST			
					actionPleadNoContest(sender,senderPos,message);
					return;
				}
					
			}			
			else if(!day) //commands for the night
				{
					if(message.toLowerCase().startsWith("killno ")) //only wolves can kill someone. They may change their minds if they wish to.
					{
	//KILLNO
						actionKill(sender,senderPos,message.toLowerCase());
						return;
					}
					else if(message.toLowerCase().startsWith("kill ")) //only wolves can kill someone. They may change their minds if they wish to.
					{
	//KILL
						actionKill(sender,senderPos,message.toLowerCase());
						return;
					}
					else if(message.toLowerCase().startsWith("see")||
						message.toLowerCase().startsWith("spy")) 
					{
	//SPY / SEE
						actionSpy(sender, senderPos, message.toLowerCase());
						return;
					}					
			}
				
			//commands that work both day and night.
			if(message.toLowerCase().startsWith("alive"))
			{
	//ALIVE
				String names = getFromFile("ALIVE-TEXT", null, 0, NOTICE);
					
				for(int i = 0 ; i < players.size() ; i++)
					{
						if(!dead[i] && players.get(i) != null)
							names += (String)players.get(i) + " ";
					}
					
					this.sendNotice(sender, names);
					return;
			}
				
			if(message.toLowerCase().startsWith("role"))
			{
	//ROLE
				if(!gameStart)
				{
						requestRole(sender,senderPos);
						return;
				}
			}
		}
		
		//Generic Error message
		if(!sender.startsWith("NickServ")&&
				   !sender.startsWith("WikkedWire")) 
		this.sendNotice(sender,
				getFromFile("GENERIC-ERROR-PVM-MESSAGE", sender, null, 0, NOTICE));
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) 
	{
		message=Colors.removeFormattingAndColors(message);
		
		if(message.toLowerCase().startsWith("!quit"))
		{
			if(isSenderOp(sender))
			{
				if(playing)
					doVoice(false);
				this.setMode(gameChan, "-mN");
				this.partChannel(gameChan, 
						getHelpFromFile("MMBOT-OUT"));
						
						//"MMBot - Mafia Game Bot adapted by Araver for http://forum.mafiamaniac.net/."
						//+" Based on the Werewolf Game Bot v0.99 created by LLamaBoy." +
						//" Texts by Darkshine. Based on the PircBot at http://www.jibble.org/");
				this.sendMessage(sender, 
						//"OK, Boss, quitting now...");
						getHelpFromFile("MMBOT-OUT-ACKNOWLEDGE"));
				while(this.getOutgoingQueueSize() != 0);
				this.quitServer();
				System.out.println("Normal shutdown complete");
				for(int i = 0 ; i < 4 ; i++)
				{
					System.out.println();
					System.err.println();
				}
				System.out.close();
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException ix)
				{
					ix.printStackTrace();
				}
				System.exit(0);
			}
		}
		else if(message.toLowerCase().startsWith("!stop"))
		{
			if(isSenderOp(sender))
			{
				if(playing)
				{
					onRestart();
					this.sendNotice(gameChan,
							getFromFile("GAME-HAS-STOPPED", sender, null, 0, NOTICE));
				}
				else
				{
					this.sendNotice(sender,
							getFromFile("GAME-IS-NOT-ON", sender, null, 0, NOTICE));
				}	
					
			}
			//this.sendNotice(sender,
			//		getFromFile("Sorry, commmand not working right now.", sender, null, 0, NOTICE));
		}
		else if (message.toLowerCase().startsWith("!howto "))
		{
			//MMBot: Show Help Commands
			
			try
			{
				String command=message.substring(message.indexOf(" ") + 1, message.length());
				if(command.toLowerCase().startsWith("start"))
				{
					this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSTART"));
					this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSTARTDESCRIPTION"));
					
				}
				else if(command.toLowerCase().startsWith("wolfgame"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDWEREWOLF"));
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDWEREWOLFDESCRIPTION"));
				}	
				else if(command.toLowerCase().startsWith("spymafia"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSPYMAFIA"));
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSSPYMAFIADESCRIPTION "));
				}	
				else if(command.toLowerCase().startsWith("star-trek"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSTARTREK"));
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSTARTREKDESCRIPTION"));
				}	
				else if(command.toLowerCase().startsWith("rules"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDRULES"));
				}
				else if(command.toLowerCase().startsWith("quit"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDQUIT"));
				}
				else if(command.toLowerCase().startsWith("stop"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSTOP"));
				}
				else if(command.toLowerCase().startsWith("daytime"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDDAYTIME"));
				}	
				else if(command.toLowerCase().startsWith("nighttime"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDNIGHTTIME"));
				}	
				else if(command.toLowerCase().startsWith("votetime"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDVOTETIME"));
				}
				else if(command.toLowerCase().startsWith("jointime"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDJOINTIME"));
				}
				else if(command.toLowerCase().startsWith("join"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDJOIN"));
				}
				else if(command.toLowerCase().startsWith("remindertime"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDREMINDERTIME"));
				}
				else if(command.toLowerCase().startsWith("tie"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDTIE"));
				}	
				else if(command.toLowerCase().startsWith("firstdaytielynch"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDFIRSTDAYTIELYNCH"));
				}
				else if(command.toLowerCase().startsWith("changevote"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDCHANGEVOTE"));
				}
				else if(command.toLowerCase().startsWith("roster"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDROSTER"));
				}	
				else if(command.toLowerCase().startsWith("shush"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSHUSH"));
				}	
				else if(command.toLowerCase().startsWith("speak"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSPEAK"));
				}
				else if(command.toLowerCase().startsWith("voteno"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDVOTENO"));
				}
				else if(command.toLowerCase().startsWith("vote"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDVOTE"));
				}		
				else if(command.toLowerCase().startsWith("killno"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDKILLNO"));
				}
				else if(command.toLowerCase().startsWith("kill"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDKILL"));
				}
				else if(command.toLowerCase().startsWith("seeno"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSEENO"));
				}		
				else if(command.toLowerCase().startsWith("spyno"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSPYNO"));
				}		
				else if(command.toLowerCase().startsWith("see"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSEE"));
				}		
				else if(command.toLowerCase().startsWith("spy"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSPY"));
				}		
				else if(command.toLowerCase().startsWith("alive"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDALIVE"));
				}		
				else if(command.toLowerCase().startsWith("role"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDROLE"));
				}
				else if(command.toLowerCase().startsWith("removevote"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDREMOVEVOTE"));
				}
				else if(command.toLowerCase().startsWith("nocontest"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDNOCONTEST"));
				}
				else if(command.toLowerCase().startsWith("showvotes"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDSHOWVOTES"));
				}
				else if(command.toLowerCase().startsWith("finalvoteno"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDFINALVOTENO"));
				}
				else if(command.toLowerCase().startsWith("finalvote"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDFINALVOTE"));
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDFINALVOTENAME"));
				}
				else if(command.toLowerCase().startsWith("abstain"))
				{
						this.sendMessage(gameChan, Colors.DARK_GREEN +this.getHelpFromFile("COMMANDABSTAIN"));
				}
				else
					throw new Exception();
			}
			catch(Exception x)
			{
				this.sendMessage(gameChan, Colors.DARK_GREEN +"Command not recognized. Please provide a valid command to see help or use just !howto to see the list of commands");
			}
		}
		else if(message.toLowerCase().startsWith("!rules"))	
		{
			DecimalFormat twoPlaces = new DecimalFormat("0.00");
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Current set of rules:");
			
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Day length is " + 
					Colors.BROWN+ dayTime + Colors.DARK_GREEN + " seconds = "+ 
					Colors.BROWN+ twoPlaces.format(((double)dayTime/60))+Colors.DARK_GREEN +" minutes.");
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Night length is " + 
					Colors.BROWN+ nightTime + Colors.DARK_GREEN + " seconds = "+ 
					Colors.BROWN+ twoPlaces.format(((double)nightTime/60))+Colors.DARK_GREEN +" minutes.");
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch Vote length is " + 
					Colors.BROWN+ voteTime + Colors.DARK_GREEN + " seconds = "+ 
					Colors.BROWN+ twoPlaces.format(((double)voteTime/60))+Colors.DARK_GREEN +" minutes.");
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Join period length is " + 
					Colors.BROWN+ joinTime +Colors.DARK_GREEN +  " seconds = "+ 
					Colors.BROWN+ twoPlaces.format(((double)joinTime/60))+Colors.DARK_GREEN +" minutes.");
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Reminders are every " + 
					Colors.BROWN+ reminderTime + Colors.DARK_GREEN + " seconds = "+ 
					Colors.BROWN+ twoPlaces.format(((double)reminderTime/60))+Colors.DARK_GREEN +" minutes.");
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Random lynching tied players is " +
					Colors.BROWN+ (tieGame ? "on." : "off."));
			
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Random lynching tied players on first day is " +
					Colors.BROWN+ (firstDayTieisRandomLynch ? "on." : "off."));
			
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Changing votes is " + 
					Colors.BROWN+ (changeVote ? "allowed." : "not allowed."));
		
			this.sendMessage(gameChan, Colors.DARK_GREEN + "Roster display is " + 
					Colors.BROWN+ (rosterDisplay ? "on." : "off."));
			
			this.sendMessage(gameChan, Colors.BROWN + "Only OPs can change rules! Rules cannot be changed while a game is in progress!");
		}
		else if(message.toLowerCase().startsWith("!howto"))	
		{
			this.sendMessage(gameChan, Colors.DARK_GREEN + this.getHelpFromFile("STARTGAMECOMMANDS"));
			this.sendMessage(gameChan, Colors.DARK_GREEN + this.getHelpFromFile("CHANCOMMANDS"));
			this.sendMessage(gameChan, Colors.DARK_GREEN + this.getHelpFromFile("PRVCOMMANDS"));
			this.sendMessage(gameChan, Colors.DARK_GREEN + this.getHelpFromFile("HELPCOMMANDSTYPE"));
		}
		else if(!playing)
		{
			
			//Toggle through available games, re-read cache, prepare for gameStart.			
			if(message.toLowerCase().startsWith("!start"))
			{
				boolean refreshCache= storeTexts && !gameFile.equalsIgnoreCase("mafia.txt");
				gameFile = "mafia.txt";
				if(refreshCache) refreshCacheGameFile();
				spyMafia=false;
			}
			else if(message.toLowerCase().startsWith("!star-trek"))		
			{
				boolean refreshCache= storeTexts && !gameFile.equalsIgnoreCase("startrek.txt");
				gameFile = "startrek.txt";
				if(refreshCache) refreshCacheGameFile();
				message="!start";
				spyMafia=false;
			}
			else if(message.toLowerCase().startsWith("!wolfgame"))
			{
				boolean refreshCache= storeTexts && !gameFile.equalsIgnoreCase("wolfgame.txt");
				gameFile = "wolfgame.txt";
				if(refreshCache) refreshCacheGameFile();
				message="!start";
				spyMafia=false;
			}
			else if(message.toLowerCase().startsWith("!spymafia"))
			{
				boolean refreshCache= storeTexts && !gameFile.equalsIgnoreCase("mafia.txt");
				gameFile = "mafia.txt";
				if(refreshCache) refreshCacheGameFile();
				spyMafia=true;
				message="!start";
			}
				
			
			if(message.toLowerCase().startsWith("!start")) //initiates a game.
			{
				startGame(sender);
				
				//Get the strings to distinguish between a single wolf and 2 wolves in the texts.
				oneWolf = getFromFile("1-WOLF", null, 0, NOTICE);
				manyWolves = getFromFile("MANY-WOLVES", null, 0, NOTICE);
				
				if(players.add(sender))
				{
					this.setMode(gameChan, "+v " + sender);
					this.sendNotice(sender,
						getFromFile("ADDED", null, 0, NOTICE));
				}
				else
					this.sendNotice(sender,
						"Could not add you to player list. Please try again." +
						" (/msg " + this.getName() + " join.");
			}
			else if(message.toLowerCase().startsWith("!daytime ")) //alter the duration of the day
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							dayTime = time;
							this.sendMessage(gameChan, this.getFromFile("DAYCHANGE", null, time, CONTROL));
							 //"Duration of the day now set to " + Colors.DARK_GREEN + Colors.UNDERLINE + dayTime + Colors.NORMAL + " seconds");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendMessage(sender, "Please provide a valid value for the daytime length (!daytime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!nighttime ")) //alter the duration of the night
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							nightTime = time;
							this.sendMessage(gameChan, this.getFromFile("NIGHTCHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the night time length (!nighttime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!votetime ")) //alter the time for a vote
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							voteTime = time;
							this.sendMessage(gameChan, this.getFromFile("VOTECHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the Lynch Vote time length (!votetime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!jointime ")) //alter the time for a vote
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							joinTime = time;
							this.sendMessage(gameChan, this.getFromFile("JOINCHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the join time length (!jointime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!remindertime ")) //alter the remindertime
			{
				if(isSenderOp(sender))
				{
					try
					{
						int time = (Integer.parseInt(message.substring(message.indexOf(" ") + 1, message.length())));
						if(time > 0)
						{
							reminderTime = time;
							this.sendMessage(gameChan, this.getFromFile("REMINDERCHANGE", null, time, CONTROL));
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the reminder interval (!remindertime <TIME IN SECONDS>)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!tie ")) //tie option
			{
				if(isSenderOp(sender))
				{
					try
					{
						String tie = message.substring(message.indexOf(" ") + 1, message.length());
						if(tie.equalsIgnoreCase("on"))
						{
							tieGame = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie activated");
						}
						else if(tie.equalsIgnoreCase("off"))
						{
							tieGame = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote tie deactivated");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the vote tie condition (!tie ON/OFF)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!firstdaytielynch ")) //first day tie option
			{
				if(isSenderOp(sender))
				{
					try
					{
						String tie = message.substring(message.indexOf(" ") + 1, message.length());
						if(tie.equalsIgnoreCase("on"))
						{
							firstDayTieisRandomLynch = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "First day tie will result in random lynch!");
						}
						else if(tie.equalsIgnoreCase("off"))
						{
							firstDayTieisRandomLynch = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "First day tie will result in no lynch!");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the first day tie condition (!firstdaytielynch ON/OFF)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!testing ")) //testing environment option
			{
				if(isSenderOp(sender))
				{
					try
					{
						String testingMode = message.substring(message.indexOf(" ") + 1, message.length());
						if(testingMode.equalsIgnoreCase("on"))
						{
							testing = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Testing environment activated ;)");
						}
						else if(testingMode.equalsIgnoreCase("off"))
						{
							testing = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Testing environment deactivated ;)");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for testing (!testing ON/OFF)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!shush")) //stop idle barks
			{
				if(isSenderOp(sender))
				{
					if(doBarks)
					{
						doBarks = false;
						idleTimer.cancel();
						idleTimer = null;
						this.sendMessage(gameChan, Colors.DARK_GREEN + "I won't speak any more.");
					}
					else
						this.sendNotice(sender, Colors.DARK_GREEN + "I'm already silent. :P");
				}
			}
			else if(message.toLowerCase().startsWith("!speak")) //enable idle barks
			{
				if(isSenderOp(sender))
				{
					if(!doBarks)
					{
						doBarks = true;
						startIdle();
						this.sendMessage(gameChan, Colors.DARK_GREEN + "Thank you master for giving back my voice!");
					}
					else
						this.sendNotice(sender, Colors.DARK_GREEN + "I'm already speaking and stuff. :P");
				}
			}
			else if(message.toLowerCase().startsWith("!roster ")) //rosterDisplay on/off.
			{
				if(isSenderOp(sender))
				{
					try
					{
						String roster = message.substring(message.indexOf(" ") + 1, message.length());
						if(roster.equalsIgnoreCase("on"))
						{
							rosterDisplay = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Roster Display activated");
						}
						else if(roster.equalsIgnoreCase("off"))
						{
							rosterDisplay = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Roster Display deactivated");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the Display Roster option (!roster ON/OFF)");
					}
				}
			}
			else if(message.toLowerCase().startsWith("!changevote ")) //rosterDisplay on/off.
			{
				if(isSenderOp(sender))
				{
					try
					{
						String roster = message.substring(message.indexOf(" ") + 1, message.length());
						if(roster.equalsIgnoreCase("on"))
						{
							changeVote = true;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote changing activated");
						}
						else if(roster.equalsIgnoreCase("off"))
						{
							changeVote = false;
							this.sendMessage(gameChan, Colors.DARK_GREEN + "Vote changing deactivated");
						}
						else
							throw new Exception();
					}
					catch(Exception x)
					{
						this.sendNotice(sender, "Please provide a valid value for the Change Vote option (!changevote ON/OFF)");
					}
				}
			}
			else 
			{
				DecimalFormat twoPlaces = new DecimalFormat("0.00");
			if(message.toLowerCase().startsWith("!daytime"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Day length is " + 
						Colors.BROWN+ dayTime + Colors.DARK_GREEN + " seconds = "+ 
						Colors.BROWN+ twoPlaces.format(((double)dayTime/60))+Colors.DARK_GREEN +" minutes.");
			else if(message.toLowerCase().startsWith("!nighttime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Night length is " + 
						Colors.BROWN+ nightTime + Colors.DARK_GREEN + " seconds = "+ 
						Colors.BROWN+ twoPlaces.format(((double)nightTime/60))+Colors.DARK_GREEN +" minutes.");
			else if(message.toLowerCase().startsWith("!votetime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch Vote length is " + 
						Colors.BROWN+ voteTime + Colors.DARK_GREEN + " seconds = "+ 
						Colors.BROWN+ twoPlaces.format(((double)voteTime/60))+Colors.DARK_GREEN +" minutes.");
			else if(message.toLowerCase().startsWith("!jointime"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Join period length is " + 
						Colors.BROWN+ joinTime +Colors.DARK_GREEN +  " seconds = "+ 
						Colors.BROWN+ twoPlaces.format(((double)joinTime/60))+Colors.DARK_GREEN +" minutes.");
			else if(message.toLowerCase().startsWith("!remindertime"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Reminders are every " + 
						Colors.BROWN+ reminderTime + Colors.DARK_GREEN + " seconds = "+ 
						Colors.BROWN+ twoPlaces.format(((double)reminderTime/60))+Colors.DARK_GREEN +" minutes.");
			else if(message.toLowerCase().startsWith("!tie"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Lynch vote tie is " +
						Colors.BROWN+ (tieGame ? "on." : "off."));
			else if (message.toLowerCase().startsWith("!firstdaytielynch"))
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Random lynching tied players on first day is " +
						Colors.BROWN+ (firstDayTieisRandomLynch ? "on." : "off."));
			else if(message.toLowerCase().startsWith("!changevote"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Changing votes is " + 
						Colors.BROWN+ (changeVote ? "allowed." : "not allowed."));
			else if(message.toLowerCase().startsWith("!roster"))	
				this.sendMessage(gameChan, Colors.DARK_GREEN + "Roster display is " + 
						Colors.BROWN+ (rosterDisplay ? "on." : "off."));
			}
		}
		else if(message.indexOf("it") != -1 && message.indexOf(this.getNick()) != -1 && day &&
			message.indexOf("it") < message.indexOf(this.getNick())) //when it looks like the bot is accused, send a reply
		{
			this.sendMessage(gameChan, "Hey, " + sender + "! I didn't kill anyone!");
		}
		else
		{
			if(playing)
			{
				if(!gameStart)
				{
					for(int i = 0 ; i < players.size() ; i++)
					{
						if(dead[i] && sender.equals(players.get(i)))
							this.setMode(gameChan, "-v " + sender);
					}
					
					/*Not Working
					String check = getFromFile("SEER-SEE", "QZ", 0, 0);
					int ind =  message.indexOf(check.substring(0, check.indexOf("QZ") - 2));
					
					if(ind > 0)
					{
						if(message.indexOf(this.getName()) > 0)
						{
							if(ind > message.indexOf(this.getName())) //check if the player fabricated a quote to incriminate the bot :)
								this.kick(gameChan, sender,
									getFromFile("CHEAT-KICK", sender, 0, NOTICE));
						}
						else
							this.kick(gameChan, sender,
								getFromFile("CHEAT-KICK", sender, 0, NOTICE));
					}
					*/
				}
			}
		}
	}
	//take ghosts their voice after last line
	protected void onAction(String sender, String login, String hostname, String target, String action)
	{
		if(playing)
		{
			if(!gameStart)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(dead[i] && sender.equals(players.get(i)))
						this.setMode(gameChan, "-v " + sender);
				}
			}
		}
	}
	//to check if the sender of a message is op, necessary for some options
	protected boolean isSenderOp(String nick) 
	{
		User users[] = this.getUsers(gameChan);
			
		for(int i = 0 ; i < users.length ; i++)
		{
			if(users[i].getNick().equals(nick))
				if(users[i].isOp())
					return true;
				else
					return false;
		}
		return false;
	}
	//if the player changed their nick and they're in the game, changed the listed name
	protected void onNickChange(String oldNick, String login, String hostname, String newNick)
	{
		if(testing) return;
		if(playing)
		{
			if(isNameAdded(oldNick))
			{
				for(int i = 0 ; i < players.size() ; i++)
				{	
					if(oldNick.equals((String) players.get(i)))
					{
						String old = (String)players.set(i, newNick);
						
						if(!dead[i])
							this.sendMessage(gameChan, Colors.DARK_GREEN +
								old + " has changed nick to " + players.get(i) + "; Player list updated.");
						break;
					}
				}
				for(int i = 0 ; i < priority.size() ; i++)
				{	
					if(oldNick.equals((String) priority.get(i)))
					{
						players.set(i, newNick);
						this.sendMessage(gameChan, Colors.DARK_GREEN +
							newNick + " has changed nick; Priority list updated.");
						break;
					}
				}
				
								
				//This doesn't seem to work at times...
				//TODO - fix votes to mean No on roster instead of name - no change needed then.
				if(timeToVote) //if the player changes nick during the vote period, update any votes against him
				{
					for(int i = 0 ; i < votes.size() ; i++)
					{
						if(((String)((Vote)votes.get(i)).getVote()).equalsIgnoreCase(oldNick))
						{
							Vote newVote = new Vote((String)votes.get(i), newNick);
							votes.set(i, newVote);
						}
					}
				}
			}
		}
	}
	//if a player leaves while the game is on, remove him from the player list
	//and if there is a priority list, add the first person from that in his place.
	protected void onPart(String channel, String sender, String login, String hostname) 
	{
		if(testing) return;//ignore for testing purposes.
		
		if(playing)
		{
			if(!priority.isEmpty())
			{
				String newPlayer = (String) priority.get(0);
				String role = "";
				//priority.remove(0);
				
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals((String)players.get(i)) && !dead[i])
					{
						//if player that left was not dead otherwise no backup needed.
						priority.remove(0);
						
						if(spyMafia==false)
						{
						//resend role to new player
							if(i == seer)
								role = getFromFile("ROLE-SEER", null, 0, NOTICE);
							else if(wolf[i])
								role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
							else
								role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
						}
						else
						{
							if(wolf[i])
								role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
							else
								role = getFromFile("ROLE-SEER", null, 0, NOTICE);
						}
						//install backup						
						players.set(i, newPlayer);
						
						//Maybe redundant, but backup needs to replace old player in all votes for him
						if(timeToVote)
						{
							for(int k = 0 ; k < votes.size() ; k++)
							{
								if(((String)((Vote)votes.get(k)).getVote()).equalsIgnoreCase(sender))
								{
									String votingplayer = ((String)((Vote)votes.get(k)).getName());
									
									votes.remove(i); 		//if the player leaves, no votes for him
									Vote replacedvote = new Vote(votingplayer, newPlayer);
									votes.add(replacedvote);
								}
							}
						}
						
						this.sendNotice(newPlayer,
							getFromFile("FLEE-PRIORITY-NOTICE", sender, 0, NOTICE));
						this.sendMessage(gameChan,
							getFromFile("FLEE-PRIORITY", sender, newPlayer, 0, CONTROL));
						if(day || timeToVote)
							this.setMode(gameChan, "+v " + newPlayer);
						
						if(spyMafia==false)
						{
							if(i == seer)
								this.sendNotice(newPlayer,
									getFromFile("S-ROLE", null, 0, NOTICE));
							else if(wolf[i])
								if(wolves.size() == 1)
									this.sendNotice(newPlayer,
										getFromFile("W-ROLE", null, 0, NOTICE));
								else
									this.sendNotice(newPlayer,
										getFromFile("WS-ROLE", null, 0, NOTICE));
							else
								this.sendNotice(newPlayer,
									getFromFile("V-ROLE", null, 0, NOTICE));
						}
						else//SpyMafia
						{
							if(roles[i]!=BADDIE)
								this.sendNotice(newPlayer,
									getFromFile("S-ROLE", newPlayer, 0, NOTICE));
							else //wolf
							{
								this.sendNotice(newPlayer,
										getFromFile("W-ROLE", newPlayer, 0, NOTICE));
							}
						}
						break;
					}
				}
			}
			else//no backups available - kill fleeing person
				if(!gameStart)
			{
				int index = -1;
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals((String)players.get(i)))
					{
						index = i;
						players.set(i, null);
						if(!dead[i])
						{
							if(wolf[i])
							{
								for(int j = 0 ; j < wolves.size() ; j++)
								{
									if(((String)wolves.get(j)).equals(sender))
										wolves.remove(j);
								}
								
								this.sendMessage(gameChan,
									getFromFile("FLEE-WOLF", sender, 0, NARRATION));
							}
							else
								this.sendMessage(gameChan,
									getFromFile("FLEE-VILLAGER", sender, 0, NARRATION));
								
							dead[i] = true;
								
							if(wolfVictim != null)
							{
								for(int j = 0 ; j < wolfVictim.size() ; j++)
								{
									Vote oldVictim = (Vote) wolfVictim.get(j);
									if(sender.equalsIgnoreCase(oldVictim.getVote()))
										wolfVictim.remove(j);
									//ignore wolves' that leave
									if(sender.equalsIgnoreCase(oldVictim.getName()))
										wolfVictim.remove(j);
								}
							}
					
							checkWin();
						}
					}
				}
				if(timeToVote)
				{
					for(int i = 0 ; i < votes.size() ; i++)
					{
						if(((String)((Vote)votes.get(i)).getVote()).equalsIgnoreCase(sender))
						{
							String votingplayer = ((String)((Vote)votes.get(i)).getName());
							//update situation for player voting player since his vote left.
							for(int j = 0 ; j < players.size() ; j++)
							{
								if (players.get(j)!=null && ((String)players.get(j)).equalsIgnoreCase(votingplayer))
								{
									finalvote[j]=false;
								}
							}
							
							votes.remove(i); 		//if the player leaves, no votes for him
						}
					}
				}
			}
			else if(gameStart)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(sender.equals(players.get(i)))
					{
						players.remove(i);
						this.sendMessage(gameChan,
								getFromFile("FLEE", sender, 0, NARRATION));
					 	break;
					}
				}
			}
		}
		else
		{
			if(priority == null || priority.isEmpty());
			else
			{
				for(int i = 0 ; i < priority.size() ; i++)
				{
					if(sender.equals((String)priority.get(i)))
					{
						priority.remove(i);
						this.sendMessage(gameChan, Colors.DARK_GREEN + Colors.UNDERLINE +
							sender + Colors.NORMAL + Colors.DARK_GREEN +
							", a player on the priority list, has left. Removing from list...");
					}
				}
			}
		}
	}
	//React to topic changes
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed)
	{
		if(playing)
		{
			this.sendMessage(gameChan, "Hey! Can't you see we're trying to play a game here? >:(");
		}
	}
	//update player list if a person quits the server
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
	{
		this.onPart(gameChan, sourceNick, sourceLogin, sourceHostname);
	}
	//reconnect if disconnected
	protected void onDisconnect()
	{
		connected = false;
		connectAndJoin();
	}
	//connect and join channel
	protected void connectAndJoin()
	{
		while(!connected) //keep trying until successfully connected
		{
			try
			{
				this.setName(name);
				this.connect(network);
				Thread.sleep(1000);		//allow it some time before identifying
				if(!ns.equalsIgnoreCase("none"))
				{
					this.sendMessage(ns, command);		//identify with nickname service
					Thread.sleep(2000);		//allow the ident to go through before joining
				}
				this.joinChannel(gameChan);
				this.setMode(gameChan, "-mN");
				connected = true;
			}
			catch(IOException iox)
			{
				System.err.println("Could not connect/IO ");
			}
			catch(NickAlreadyInUseException niux)
			{
				System.err.println("Nickname is already in use. Choose another nick");
				System.exit(1);
			}
			catch(IrcException ircx)
			{
				System.err.println("Could not connect/IRC");
			}
			catch(InterruptedException iex)
			{
				System.err.println("Could not connect/Thread");
			}
		}
	}
	//IDLETIMER
	protected void startIdle()
	{
		if(doBarks)
		{
			idleTimer = new Timer();
			//TIMER - chat
			//trigger a chat every 60-240 mins
			idleTimer.schedule(new WereTask(), ((int)(Math.random() * 7200) + 3600) * 1000);
		}
	}
	//Reconnect if kicked, update list if anyone else is kicked
	protected void onKick(String channel, String kickerNick, String kickerLogin,
		String kickerHostname, String recipientNick, String reason)
	{
		if(recipientNick.equals(this.getName()))
			this.joinChannel(channel);
		else
			this.onPart(gameChan, recipientNick, null, null);
	}
	//Spam people about game
	protected void onJoin(String channel, String sender, String login, String hostname)
	{
		if(!sender.equals(this.getNick()))
		{
			if(gameStart)
				this.sendNotice(sender,
					getFromFile("GAME-STARTED", null, 0, NOTICE));
			else if(playing)
				this.sendNotice(sender,
					getFromFile("GAME-PLAYING", null, 0, NOTICE));
		}
	}
	//MMBot: Stops game and reinits variables.
	protected void onRestart()
	{
		//after !stop, act like the first time you Join
		reminderTimer.cancel();
		gameTimer.cancel();
		reminderTimer=new Timer();
		gameTimer=new Timer();
		interTimerType=NOTIMER;
		remainingTime=0;
		
		if(playing)
			doVoice(false);
		
		playing = false;
		gameStart = false;
		day=false;
		timeToVote = false;
		startIdle();
	}
	//JOINPOST + JOINTIMER
	protected void startGame(String sender)
	{	
		if(doBarks)
			idleTimer.cancel();	//don't make comments while the game's on.
		
		this.setMode(gameChan, "+mN");
		
		roundNo = 0;
		if(priority == null || priority.isEmpty())
			players = new Vector(MINPLAYERS, 1);
		else
		{
			players = new Vector(MINPLAYERS, 1);
			
			this.sendMessage(gameChan, Colors.DARK_GREEN +
				"");
			for(int i = 0 ; i < priority.size() ; i++)
			{
				if(sender.equals((String)priority.get(i)))
					priority.remove(i);
				else
				{
					if(players.add((String)priority.get(i)))
					{
						this.setMode(gameChan, "+v " + (String)priority.get(i));
						this.sendMessage(gameChan,
							getFromFile("JOIN", sender, 0, NARRATION));
					}
					else
						this.sendNotice(gameChan, "Sorry, you could not be added. Please try again.");
				}
			}
		}
			
		priority = new Vector(1, 1);
		wolves = new Vector(1, 1);
		
		playing = true;
		day = false;
		timeToVote = false;
		gameStart = true;
		firstDay = true;
		firstNight = true;
		toSee = -1;
		
		//this was stupid.
		//for(int i = 0 ; i < players.size() ; i++)
		//{
		//	toSpy[i]=-1;
		//}
		
		this.sendMessage(gameChan, 
			getFromFile("STARTGAME", sender, joinTime, NARRATION));
			
		this.sendNotice(gameChan,
			getFromFile("STARTGAME-NOTICE", sender, 0, NOTICE));
		
		while(this.getOutgoingQueueSize() != 0);
		
		gameTimer = new Timer();
		reminderTimer = new Timer();
		//Half-time reminder
		interTimerType=JOINTIMER;
		if(joinTime>60) //60sec=1min
		{
			remainingTime=joinTime-60;
			reminderTimer.schedule(new WereTask(), 60000);
		}
		else
		{	
			interTimerType=NOTIMER;
			remainingTime=0;
			gameTimer.schedule(new WereTask(), joinTime * 1000);
		}
		
		//TIMER joining
		//gameTimer.schedule(new WereTask(), joinTime * 1000);
	}
	//DAY/VOTE/NIGHT starting
	protected void playGame()
	{
		String nicks = "",
			modes = "";
		int count = 0;
		
		if(playing)
		{
			if(timeToVote)
			{
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(!dead[i])
					{
						if(notVoted[i] == 2)
						{
							dead[i] = true;
							inactive[i] = true;
							
							this.sendMessage(gameChan,
								getFromFile("NOT-VOTED", (String)players.get(i), 0, NARRATION));
							this.sendNotice((String) players.get(i),
								getFromFile("NOT-VOTED-NOTICE", null, 0, NOTICE));
								
							this.setMode(gameChan, "-v " + players.get(i));
						}
					}
				}
				
				if(checkWin())
					return;
				//Voting starts - tell rules
				if(changeVote==false)
				{
				this.sendMessage(gameChan,
					getFromFile("VOTETIME", null, voteTime, GAME));
				}
				else 
				{
				this.sendMessage(gameChan,
					getFromFile("VOTETIMECHANGEALLOWED", null, voteTime, GAME));
				}	
				
				//tell tie rules
				if(firstDay&&firstDayTieisRandomLynch) //first day
				{
					this.sendMessage(gameChan,
							getFromFile("TIEFIRSTDAY-RANDOMLYNCH", null, 0, GAME));
				}
				else if(firstDay&&!firstDayTieisRandomLynch) //first day nolynch
					{
						this.sendMessage(gameChan,
								getFromFile("TIEFIRSTDAY-NOLYNCH", null, 0, GAME));
					}
				else if(tieGame)
				{
					this.sendMessage(gameChan,
							getFromFile("TIEANYDAY-RANDOMLYNCH", null, 0, GAME));
				}
				else
				{
					this.sendMessage(gameChan,
							getFromFile("TIEANYDAY-NOLYNCH", null, 0, GAME));
				}
				
				if(spyMafia)
				{//special abstain rule
					this.sendMessage(gameChan,
							getFromFile("MAJORITY-ABSTAIN-WINS", null, (countAlivePlayers()+1)/2, GAME));
				}
				
				//Flush queue
				while(this.getOutgoingQueueSize() != 0);
				
				//Half-time reminder
				interTimerType=VOTETIMER;
				if(voteTime>60) //60sec=1min
				{
					remainingTime=voteTime-60;
					reminderTimer.schedule(new WereTask(), 60000);
				}
				else
				{
					interTimerType=NOTIMER;
					remainingTime=0;
					gameTimer.schedule(new WereTask(), voteTime * 1000);
				}
				
				//TIMER - vote end
				//gameTimer.schedule(new WereTask(), voteTime * 1000);
				
				
			}
			else if(day) //Discuss but don't vote
			{
				if(spyMafia==false)
				{
					if(toSee != -1)
					if(!dead[seer] && !dead[toSee])
					{
						if(wolf[toSee])
							role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
						else
							role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
						
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE", (String) players.get(toSee),toSee, NOTICE));
					}
					else if(dead[seer])
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE-KILLED", (String) players.get(toSee), 0, NOTICE));
					else
						this.sendNotice((String)players.get(seer),
							getFromFile("SEER-SEE-TARGET-KILLED", (String) players.get(toSee), 0, NOTICE));
				}
				else
				{
					//Spy Mafia personal touch
					for(int i = 0 ; i < players.size() ; i++)
					{
						if(players.get(i)!=null&&!wolf[i]&&!dead[i])
						{
							//if spy was targeted
							if(toSpy[i]!=-1)
							{
							 if(dead[toSpy[i]])
							 {
								 this.sendNotice((String)players.get(i),
									getFromFile("SEER-SEE-TARGET-KILLED", (String) players.get(toSpy[i]), 0, NOTICE));
							 }
							 else
							 {
								 switch(roles[i])
								 {
								 	case SANE: 		
								 		if(roles[toSpy[i]]==BADDIE)
								 			role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
								 		else
								 			role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
								 		this.sendNotice((String)players.get(i),
											getFromFile("SEER-SEE", (String) players.get(toSpy[i]), 
													0, NOTICE));
								 			break;
									case INSANE: 
										if(roles[toSpy[i]]!=BADDIE)
								 			role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
								 		else
								 			role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
										this.sendNotice((String)players.get(i),
												getFromFile("SEER-SEE", (String) players.get(toSpy[i]), 
														0, NOTICE));
									 			break;
									case NAIVE: 	
										role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
										this.sendNotice((String)players.get(i),
													getFromFile("SEER-SEE", (String) players.get(toSpy[i]), 0, NOTICE));
										break;
									case PARANOID: 
										role = getFromFile("ROLE-WOLF", null, 0, NOTICE);
										this.sendNotice((String)players.get(i),
											getFromFile("SEER-SEE", (String) players.get(toSpy[i]), 0, NOTICE));
										break;
								 }
							 }
							}
							
						}
					}
					
					//end
					for(int i = 0 ; i < players.size() ; i++)
					{
						toSpy[i]=-1; //reset
					}
				}
				//let people discuss
				doVoice(true);
				
				this.sendNotice(gameChan, 
						getFromFile("SEPARATOR", null, 0, CONTROL));
				this.sendNotice(gameChan, 
						"---------- Day "+(isFirstDay ? roundNo/2+1 : roundNo/2) +" starts!   ---------- ");
				this.sendNotice(gameChan, 
						getFromFile("SEPARATOR", null, 0, CONTROL));
				
				this.sendMessage(gameChan,
					getFromFile("DAYTIME", null, dayTime, GAME));
				//Print initial day-roster
				printRoster(false);
				//tell tie rules
				if(firstDay&&firstDayTieisRandomLynch) //first day
				{
					this.sendMessage(gameChan,
							getFromFile("TIEFIRSTDAY-RANDOMLYNCH", null, 0, GAME));
				}
				else if(firstDay&&!firstDayTieisRandomLynch) //first day nolynch
					{
						this.sendMessage(gameChan,
								getFromFile("TIEFIRSTDAY-NOLYNCH", null, 0, GAME));
					}
				else if(tieGame)
				{
					this.sendMessage(gameChan,
							getFromFile("TIEANYDAY-RANDOMLYNCH", null, 0, GAME));
				}
				else
				{
					this.sendMessage(gameChan,
							getFromFile("TIEANYDAY-NOLYNCH", null, 0, GAME));
				}
				
				if(spyMafia)
				{//special abstain rule
					this.sendMessage(gameChan,
							getFromFile("MAJORITY-ABSTAIN-WINS", null, (countAlivePlayers()+1)/2, GAME));
				}
				//Flush messages
				while(this.getOutgoingQueueSize() != 0);
				
				//TIMER - day end &vote start.
				interTimerType=DAYTIMER;
				if(dayTime>60) //60sec=1min
				{
					remainingTime=dayTime-60;
					reminderTimer.schedule(new WereTask(), 60000);
				}
				else
				{
					interTimerType=NOTIMER;
					remainingTime=0;
					gameTimer.schedule(new WereTask(), dayTime * 1000);
				}

				
			}
			else if(!day)
			{
				//Silent night
				doVoice(false);
				
				roundNo++;//increase Round Number
				
				this.sendNotice(gameChan, 
						getFromFile("SEPARATOR", null, 0, CONTROL));
				this.sendNotice(gameChan, 
						"---------- Night "+(isFirstDay ? roundNo/2+1 : roundNo/2+1) +" starts! ----------");
				this.sendNotice(gameChan, 
						getFromFile("SEPARATOR", null, 0, CONTROL));
				//print to see alive players
				printRoster(false);
				
				if(firstNight)
				{
					firstNight = false;
					this.sendMessage(gameChan,
						getFromFile("FIRSTNIGHT", null, 0, NARRATION));
				}
				else
				{
					this.sendMessage(gameChan, Colors.DARK_BLUE +
						getFromFile("NIGHTTIME", null, 0, NARRATION));
					//remind everybody their roles
					remindRoles();
				}
				
				if(spyMafia==true)
					{
					this.sendMessage(gameChan,
						getFromFile("WOLF-INSTRUCTIONS", null, nightTime, GAME));
					}
				else
				if(wolves.size() == 1)
					this.sendMessage(gameChan,
						getFromFile("WOLF-INSTRUCTIONS", null, nightTime, GAME));
				else
					this.sendMessage(gameChan,
						getFromFile("WOLVES-INSTRUCTIONS", null, nightTime, GAME));
						
				if(spyMafia==true ||!dead[seer])
					this.sendMessage(gameChan,
						getFromFile("SEER-INSTRUCTIONS", null, nightTime, GAME));
				
				while(this.getOutgoingQueueSize() != 0);
				
				//TIMER - night end
				interTimerType=NIGHTTIMER;
				if(nightTime>60) //60sec=1min
				{
					remainingTime=nightTime-60;
					reminderTimer.schedule(new WereTask(), 60000);
				}
				else
				{
					interTimerType=NOTIMER;
					remainingTime=0;
					gameTimer.schedule(new WereTask(), nightTime * 1000);
				}
				//gameTimer.schedule(new WereTask(), nightTime * 1000);
				}
		}
	}
	//method to batch voice/devoice all the users on the player list.
	protected void doVoice(boolean on)
	{
		String nicks = "",
			modes = "";
		int count = 0;
		
		if(on)
			modes = "+";
		else
			modes = "-";
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			try
			{
				if(!dead[i] && players.get(i) != null)
				{
					nicks += players.get(i) + " ";
					modes += "v";
					count++;
					if(count % 4 == 0)
					{
						this.setMode(gameChan, modes + " " + nicks);
						nicks = "";
						
						if(on)
							modes = "+";
						else
							modes = "-";
						
						count = 0;
					}
				}
			}
			catch(NullPointerException npx)
			{
				System.out.println("Could not devoice, no dead array");
			}
		}
		
		this.setMode(gameChan, modes + " " + nicks); //mode the stragglers that dont make a full 4
	}
	//checks if the name who tries to join is in the channel
	protected boolean isInChannel(String name)
	{
		User[] users = this.getUsers(gameChan);
		
		for(int i = 0 ; i < users.length ; i++)
		{
			if(users[i].getNick().equals(name))
				return true;
		}
		
		return false;
	}
	//method to go through the player and priority lists, to check if the player has already joined the game
	protected boolean isNameAdded(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{	
			if(name.equals((String) players.get(i)))
				return true;
		}
		for(int i = 0 ; i < priority.size() ; i++)
		{	
			if(name.equals((String) players.get(i)))
				return true;
		}
		
		return false;
	}
	//MMBot: count how many players are alive
	protected int countAlivePlayers()
	{
		int counter=0;
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null)
			{
				if(!dead[i])
					counter++;
			}
		}
		
		return counter;
	}
	
	//MMBot: correct case errors for vote display purposes 
	protected String correctNamePlaying(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && name.equalsIgnoreCase((String) players.get(i)))
				return (String) players.get(i);
		}
		
		return null;
	}
	//go through the player list, check the player is in the current game
	protected boolean isNamePlaying(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && name.equalsIgnoreCase((String) players.get(i)))
				return true;
		}
		
		return false;
	}
	//MMBot - shortcut to convert a valid position on the roster into a name
	protected String getPlayerName(int choiceInt)
	{
		if(choiceInt>=0 && choiceInt<players.size())
			return (String)players.get(choiceInt);
		return null;	
	}
	//MMBot: Shortcut to get a player's position on the roster
	protected int getPlayerPosition(String playerName)
	{
		if (playerName==null) return -1;
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null)
			{
				if(((String)players.get(i)).equalsIgnoreCase(playerName))
				{
					return i;
				}
			}
		}
		return -1;
	}
	//MMBot: Shortcut to search for dead
	protected boolean notDead(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && name.equalsIgnoreCase((String) players.get(i)))
			{
				//found him
				if(dead[i]) 
					return false;
				else 
					return true;
			}
		}
		return false;//did not find him playing
	}
	//MMBot: Shortcut to see if a player name has voted
	//MMBot: Shortcut to find if a player has voted / abstained
	protected boolean hasVoted(String name)
	{
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(name.equals((String) players.get(i)))
				if(voted[i]||abstain[i])//redundant abstain check
					return true;
				else
					return false;
		}
		return false;
	}
	//MMBot: Shortcut to see if a player number has voted
	//MMBot: Shortcut to see if a player has voted / abstained
	protected boolean hasVoted(int i)
	{
		return (voted[i]||abstain[i]);
	}
	//MMBot: Shortcut to print Spy-Mafia Roles
			
	//MMBot: Strings for correct publishing of voting roster
	protected String roleString(int type)
	{
		switch(type)
		{
		//BADDIE=0,	SANE=1, INSANE=2, NAIVE=3, PARANOID=4;
		case BADDIE:	return "Baddie";
		case SANE: 		return "Sane Spy";
		case INSANE: 	return "Insane Spy";
		case NAIVE: 	return "Naive Spy";
		case PARANOID: 	return "Paranoid Spy";
		}
		return "";
	}
	
	//MMBot: Roster print No+PlayerName+(is DEAD/How did he die)+(Voting for..)+(isVoteFinal)
	protected void printRoster(boolean votesToo)
	{
		if(rosterDisplay==false) return;
		// Print roster w or w/o votes
		if(votesToo==false)
		{
			this.sendMessage(gameChan,Colors.RED +"Current roster:");
			String message="";
			for(int i = 0 ; i < players.size() ; i++)
			{
				if(players.get(i) != null)
				{			
					if(dead[i])
					{
						if(spyMafia==true) 
						{
							message =""+Colors.DARK_GRAY;
							message +=(i+1)+". "+players.get(i);
							if(lynched[i])//if lynched reveal role
							{
								message +=" - Lynched and found to be "+ Colors.BLUE+roleString(roles[i]);
							}//else killed at night, role is secret
							else if(inactive[i])
							{
								message +=" - Punished for being inactive by "+ Colors.BLUE+"Manfred";								
							}
							else
							{
								message +=" - Killed by "+Colors.RED + "Baddie";
							}
						}
						else
						{
							message =""+Colors.DARK_GRAY;
							message +=(i+1)+". "+players.get(i);
							message +=" - DEAD";
						}
					}
					else
					{
						message =""+Colors.DARK_GREEN;
						message +=(i+1)+". "+players.get(i);
					}					
					this.sendMessage(gameChan,message);
				}
				else
				{
					message =""+Colors.DARK_GRAY;
					message +=(i+1)+". "+ "<Player fled>";
					message +=" - DEAD";
					this.sendMessage(gameChan,message);
				}
				
			}
		}
		else
		{
			String message=""+Colors.RED+"Voting roster:";
			this.sendMessage(gameChan,message);
			message="";
			for(int i = 0 ; i < players.size() ; i++)
			{
				if(players.get(i) != null)
				{
					if(dead[i])
					{
						if(spyMafia==true) 
						{
							message =""+Colors.DARK_GRAY;
							message +=(i+1)+". "+players.get(i);
							if(lynched[i])//if lynched reveal role
							{
							message +=" - Lynched and found to be "+ Colors.BLUE+roleString(roles[i]);
							}
							else if(inactive[i])
							{
								message +=" - Punished for being inactive by "+ Colors.BLUE+"Manfred";								
							}//else killed at night, role is secret
							else
							{
								message +=" - Killed by "+Colors.RED + "Baddie";
							}
						}
						else
						{
							message =""+Colors.DARK_GRAY;
							message +=(i+1)+". "+players.get(i);
							message +=" - DEAD";
						}
					}
					else
					{	//alive
						message =""+Colors.DARK_GREEN;
						message +=(i+1)+". "+players.get(i);
						if (voted[i])
						{//search for vote.
							Vote thisVote = null;
							for(int k = 0 ; k < votes.size() ; k++)
							{
								if(((String)players.get(i)).equalsIgnoreCase(((Vote)votes.get(k)).getName()))
								{
									thisVote = (Vote)votes.get(k);
									break;
								}
							}
							if(thisVote!=null&&abstain[i]==false)
							{
								message+=" - Voting for ";
								
								//get player voted
								int j=0;
								for(j = 0 ; j < players.size() ; j++)
								{
									if(((String)players.get(j)).equalsIgnoreCase(thisVote.getVote()))
									{
										break;
									}
								}
								//add player color
								if(j>0)	message+=voteColors[j%12];
								else message+=voteColors[0];
								message+=thisVote.getVote();
								message+=Colors.DARK_GREEN;
								//mark final votes
								if(finalvote[i])
									message+=" - FINAL";
								
							}
							if(abstain[i])//player chose to abstain
							{
								message+=Colors.DARK_GREEN+" - Abstain"+Colors.DARK_GREEN;
								
								//mark final votes
								if(finalvote[i])
									message+=" - FINAL";
							}
						}
						
					}					
					
					this.sendMessage(gameChan,message);
				}
				else
				{
					message =""+Colors.DARK_GRAY;
					message +=(i+1)+". "+ "<Player fled>";
					message +=" - DEAD";
					this.sendMessage(gameChan,message);
				}
			}
		}
		//flush before doing something else.
		while(this.getOutgoingQueueSize() != 0);
	}
	//MMBot: End day earlier if all votes are final / someone pleads no contest
	
	//MMBot: end day early if all votes are final.
	protected void endDayEarlier()
	{
		//either: 
		//1. changeVote=false and all votes are in
		//2. changeVote=true and all votes are marked final
		if(changeVote)
		{
			this.sendMessage(gameChan,
					getFromFile("ALL-VOTES-FINAL", null, 0, NARRATION));
		}
		else
		{
			this.sendMessage(gameChan,
				getFromFile("ALL-VOTES-IN", null, 0, NARRATION));
		}
		
		//Stop the game timer
		gameTimer.cancel();
		gameTimer=null;
		//Stop the game timer
		reminderTimer.cancel();
		reminderTimer=null;
		//disregard reminders but don't stop them.
		interTimerType=NOTIMER;

		//restart game Timer and end day very soon
		gameTimer=new Timer();
		reminderTimer=new Timer();
		gameTimer.schedule(new WereTask(), 500);
	}
	
	//MMBot: check if all votes are final to end day early
	//MMBot: Check if allVotes are final
	protected boolean allVotesFinal()
	{
		if(changeVote)
		{
			for(int i = 0 ; i < players.size() ; i++)
			{
				if(players.get(i) != null)
				{
					if(!dead[i]&&finalvote[i]==false)
					{
						return false;
					}
				}
			}
			return true;
		}
		else // no vote changing allowed - all votes count as final
		{
			for(int i = 0 ; i < players.size() ; i++)
			{
				if(players.get(i) != null)//player has not fled
				{
					if(!dead[i]&&voted[i]==false)
					{
						return false;
					}
				}
				//else ignore player left
			}
			return true;
		}
	}
	
	//tallies votes and decide lynched person.
	protected void tallyVotes()
	{	
		this.sendMessage(gameChan,
			getFromFile("TALLY", null, 0, CONTROL));

		this.sendNotice(gameChan, 
				getFromFile("SEPARATOR", null, 0, CONTROL));
		this.sendNotice(gameChan, 
				"---------- Day "+(isFirstDay ? roundNo/2+1 : roundNo/2) +" ends!     ---------- ");
		this.sendNotice(gameChan, 
				getFromFile("SEPARATOR", null, 0, CONTROL));
		
		//SpyMafia
		//abstain majority rule
		if(spyMafia==true)
		{
			int countAbstains=0;
			int alivePlayers=0;
		
			for(int i = 0 ; i < players.size() ; i++)
			{
				if(players.get(i) != null)
				{
					if(!dead[i])
					{ 
						alivePlayers++;
						if(abstain[i])
							countAbstains++;
					}
				}
			}
			
			if(2*countAbstains >= alivePlayers)
			{//Abstains trump votes. No lynch today.
				this.sendMessage(gameChan, Colors.DARK_BLUE +
						getFromFile("ABSTAIN-NO-LYNCH", null, 0, NARRATION));
				firstDay=false;
				return;
			}
		}
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null)
			{
				if(voted[i] && !abstain[i])
				{
					Vote thisVote = null;
					
					for(int k = 0 ; k < votes.size() ; k++)
					{
						if(((String)players.get(i)).equals(((Vote)votes.get(k)).getName()))
						{
							thisVote = (Vote)votes.get(k);
							break;
						}
					}
					
					for(int j = 0 ; j < players.size() ; j++)
					{
						if(players.get(j) != null && thisVote.getVote() != null)
							if(thisVote.getVote().equalsIgnoreCase((String)players.get(j)))
								wasVoted[j]++;
					}
				}
				else if(!dead[i]&&!abstain[i])//not dead, not abstained, then penalty for inactiveness.
				{
					notVoted[i]++;
				}
			}
		}
		
		int majority = 0,	//holds the majority vote
			guilty = -1;	//holds the index of the person with the majority
		Vector majIndexes = new Vector(1, 1);	//Vector which holds the indexes of all those with a majority
		
		for(int i = 0 ; i < wasVoted.length ; i++) //loop once to find the majority
		{
			if(wasVoted[i] > majority)
				majority = wasVoted[i];
		}
		
		for(int i = 0 ; i < wasVoted.length ; i++) //loop again to count how many have a majority (tie case)
		{
			if(wasVoted[i] == majority)
				majIndexes.add(new Integer(i));
		}
		
		if(majIndexes.size() == 1) //only one with majority - guilty is obvious
		{
			guilty = Integer.parseInt(((Integer)majIndexes.get(0)).toString());
		}
		else if((!firstDay&&tieGame) || (firstDay &&firstDayTieisRandomLynch)) // if tie means lynch
			{
				if(majIndexes != null && majIndexes.size() != 0)
					{
						int rand = (int) (Math.random() * majIndexes.size());
						if (wasVoted[((Integer)majIndexes.get(rand)).intValue()] == 0) //if the majority was 0, no-one voted
							guilty = -1;
						else
						{
							guilty = ((Integer)majIndexes.get(rand)).intValue();
							this.sendMessage(gameChan,
								getFromFile("TIE", null, 0, CONTROL));
						}
					}
				else //no votes
					guilty = -10;
			}
		else //(firstday=TRUE OR tieGame=false) AND (firstday=false OR firstDaytie is no lynch)
			guilty = -11;
		
		if(guilty == -11)
		{
			this.sendMessage(gameChan, Colors.DARK_BLUE +
					getFromFile("TIE-NO-LYNCH", null, 0, NARRATION));
		}
		else if(guilty == -10)
		{
			this.sendMessage(gameChan, Colors.DARK_BLUE +
				getFromFile("NO-LYNCH", null, 0, NARRATION));
		}
		else if(guilty != -1)
		{
			String guiltyStr = (String) players.get(guilty);
			dead[guilty] = true;
			lynched[guilty]=true;
			
			if(guiltyStr == null) //if the guilty person is null, he left during the lynch vote.
			{
				this.sendMessage(gameChan,
					getFromFile("LYNCH-LEFT", null, 0, CONTROL));
				return;
			}
			if(spyMafia==false)
			{
				if(guilty == seer)
				{
					this.sendMessage(gameChan,
						getFromFile("SEER-LYNCH", guiltyStr, 0, NARRATION));
					role = getFromFile("ROLE-SEER", null, 0, NARRATION);
					this.sendMessage(gameChan,
							getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
				}
				else if(wolf[guilty])
				{
					if(wolves.size() != 1)
					{
						for(int i = 0 ; i < wolves.size() ; i++)
						{
							if(guiltyStr.equals((String)wolves.get(i)))
								wolves.remove(i);
						}
					}
					
					this.sendMessage(gameChan,
						getFromFile("WOLF-LYNCH", guiltyStr, 0, NARRATION));
					role = getFromFile("ROLE-WOLF", null, 0, NARRATION);
					this.sendMessage(gameChan,
							getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
				}
				else
				{
					this.sendMessage(gameChan,
						getFromFile("VILLAGER-LYNCH", guiltyStr, 0, NARRATION));
					role = getFromFile("ROLE-VILLAGER", null, 0, NARRATION);
					this.sendMessage(gameChan,
							getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
				}
			}
			else //spyMafia
			{
				if(wolf[guilty])
				{
					this.sendMessage(gameChan,
						getFromFile("WOLF-LYNCH", guiltyStr, 0, NARRATION));
					role = getFromFile("ROLE-WOLF", null, 0, NARRATION);
					this.sendMessage(gameChan,
							getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
				}
				else
				{
					this.sendMessage(gameChan,
						getFromFile("VILLAGER-LYNCH", guiltyStr, 0, NARRATION));
					role = roleString(roles[guilty]);
					this.sendMessage(gameChan,
							getFromFile("IS-LYNCHED", guiltyStr, 0, NARRATION));
				}
			}
			
			if(guilty != seer && guilty > -1 && !wolf[guilty])
				this.sendNotice(guiltyStr,
					getFromFile("DYING-BREATH", null, 0, NOTICE));
			else
				this.setMode(gameChan, "-v " + guiltyStr);
		}
		else //if guilty == -1
		{
			this.sendMessage(gameChan,
				getFromFile("NO-VOTES", null, 0, NARRATION));
		}

		firstDay=false;
		//print to see result
		printRoster(false);
	}
	//MMBot: Add a kill
	protected void addKill(String sender, String choice)
	{
		Vote victim;
		//search for and replace older kill choices in vector.	
		for(int i=0;i<wolfVictim.size();i++)
		{
			if (wolfVictim.get(i)!=null)
			{
				victim = (Vote)wolfVictim.get(i);
				if(victim.getName().equalsIgnoreCase(sender))
				{
					wolfVictim.remove(i);
				}
			}
		}
		
		//add fresh kill
		victim = new Vote(sender,choice);
		while(!wolfVictim.add(victim));
	}
	//MMBot: NIGHTPOST. Kill victim
	protected void wolfKill()
	{
		roundNo++;
		this.sendNotice(gameChan, 
				getFromFile("SEPARATOR", null, 0, CONTROL));
		this.sendNotice(gameChan, 
				"---------- Night "+(isFirstDay ? roundNo/2 : roundNo/2) +" ends!   ----------");
		this.sendNotice(gameChan, 
				getFromFile("SEPARATOR", null, 0, CONTROL));
		
		String victim = "";
		Vote victimVote;
		
		//no victims chosen.
		if(wolfVictim.isEmpty())
		{
			this.sendMessage(gameChan,
				getFromFile("NO-KILL", null, 0, NARRATION));
			return;
		}
		else if(wolfVictim.size() == 1)
		{
			victimVote = (Vote)wolfVictim.get(0);
		}
		else //2 victims
		{
			if(wolfVictim.get(0).equals(wolfVictim.get(1)))
			{
				victimVote = (Vote)wolfVictim.get(0);
			}
			else
			{
				int randChoice = (int) (Math.random() * wolfVictim.size());
				victimVote = (Vote)wolfVictim.get(randChoice);
			}
		}
		//get victim
		victim = victimVote.getVote();
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null && ((String)players.get(i)).equalsIgnoreCase(victim))
			{
				if(players.get(i) != null)
				{
					//use the name from the game list for case accuracy
					String deadName = (String) players.get(i);	
										
					dead[i] = true;		//make the player dead
					
					if(spyMafia==false)
					{
						if(i == seer)
						{
							this.sendMessage(gameChan,
									getFromFile("SEER-KILL", deadName, 0, NARRATION));
							role = getFromFile("ROLE-SEER", null, 0, NOTICE);
							this.sendMessage(gameChan, Colors.DARK_BLUE +
									getFromFile("IS-KILLED", deadName, 0, NARRATION));
						}
						else
						{
							this.sendMessage(gameChan, 
									getFromFile("VILLAGER-KILL", deadName, 0, NARRATION));
								role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
						}
					}
					else//spyMafia true. It cannot be the Baddie . Don't want to show the role
					{
						this.sendMessage(gameChan, 
								getFromFile("VILLAGER-KILL", deadName, 0, NARRATION));
						role = getFromFile("ROLE-VILLAGER", null, 0, NOTICE);
					}
					
					this.sendMessage(gameChan, Colors.DARK_BLUE +
						getFromFile("IS-KILLED", deadName, 0, NARRATION));
					
					this.setMode(gameChan, "-v " + victim);
				}
//				else
//				{
//					for(int j = 0 ; j < wolves.size() ; j++)
//					{
//						this.sendNotice((String)wolves.get(j), "The person you selected has left the game");
//					}
//				}
			}
		}
	}
	//MMBot: remind all roles at beginning of night.
	protected void remindRoles()
	{
		if(spyMafia==false)
		{
			//resend to wolves 
			if(players.size() < TWOWOLVES) //if there are less than TWOWOLVES players, only one wolf
			{
				this.sendNotice((String)wolves.get(0),
							getFromFile("W-ROLE",(String)wolves.get(0), 0, NOTICE));
			}
			else
			{ //check if wolf is alive
				for(int i = 0 ; i < wolves.size() ; i++)
				{
					for(int j = 0 ; j < players.size() ; j++)
					{
						if(players.get(j) != null 
								&&((String)players.get(j)).equalsIgnoreCase((String)wolves.get(i)) 
								&& !dead[j])
							{
								this.sendNotice((String)wolves.get(i),
									getFromFile("WS-ROLE", (String)(i == 0 ? wolves.get(1) : wolves.get(0)),
										0, NOTICE));						
							}
					}
					
				}
			}
			//resend to seer
			if(seer>=0&&!dead[seer])
			{
				this.sendNotice((String)players.get(seer),
					getFromFile("S-ROLE", (String)players.get(seer), 0, NOTICE));
			}
			
			//resend to other alive players
			for(int i = 0 ; i < players.size() ; i++)
			{
				try { if(i%2 == 0) Thread.sleep(200); }
				catch(Exception x) { x.printStackTrace(); }
				if(!wolf[i] && i != seer &&!dead[i])
					this.sendNotice((String)players.get(i),
						getFromFile("V-ROLE", (String)players.get(i), 0, NOTICE));
			}
		}
		else//spyMafia - way simpler
		{
			for(int i = 0 ; i < players.size() ; i++) //tell anyone that isnt a wolf that they are human
			{
				try { if(i%2 == 0) Thread.sleep(300); }
				catch(Exception x) { x.printStackTrace(); }
				if(roles[i]>0)
					this.sendNotice((String)players.get(i),
						getFromFile("S-EXT-ROLE", (String)players.get(i), 0, NOTICE));
				else //wolf
				{
					wolves.add(players.get(i));
					wolf[i] = true;
					this.sendNotice((String)players.get(i),
							getFromFile("W-ROLE", (String)players.get(i), 0, NOTICE));
				}
			}
		}
			
	}
	//MMBot: if game is full, randomly distributes roles.
	protected void setRoles()
	{
		if(players.size() < MINPLAYERS)		
		{
			//Not enough players
			this.setMode(gameChan, "-mN");
			doVoice(false);
			
			this.sendMessage(gameChan,
				getFromFile("NOT-ENOUGH", null, 0, CONTROL));
			playing = false;
			gameFile = "mafia.txt"; //reset the game file
			startIdle();
			
			return;
		}
		else if(spyMafia==false) //play normal game
		{
			int randWolf = (int) (Math.random() * players.size());
			wolves.add(players.get(randWolf));
			wolf[randWolf] = true;
			
			if(players.size() < TWOWOLVES) //if there are less than TWOWOLVES players, only one wolf
				this.sendNotice((String)players.get(randWolf),
					getFromFile("WOLF-ROLE", (String)players.get(randWolf), 0, NOTICE));
			else //otherwise, 2 wolves, and they know each other
			{
				boolean isWolf = true;
				
				while(isWolf) //to make sure the random number isn't the same again.
				{
					randWolf = (int) (Math.random() * players.size());
					
					if(!wolf[randWolf])
						isWolf = false;
						
				}
				wolves.add(players.get(randWolf));
				wolf[randWolf] = true;
				
				//pm both wolves and let them know who the other is
				//a bit ugly, but it does the job
				for(int i = 0 ; i < wolves.size() ; i++)
					this.sendNotice((String)wolves.get(i),
						getFromFile("WOLVES-ROLE", (String)(i == 0 ? wolves.get(1) : wolves.get(0)),
							0, NOTICE));
						
				this.sendMessage(gameChan,
					getFromFile("TWOWOLVES", null, 0, CONTROL));
			}
		}
		else//SpyMafia==true and at least 5 players.
		{
			//this is a redundant check in case hell breaks loose.
			if(players.size()!=5)
			{
				this.sendMessage(gameChan,
						getFromFile("UNKNOWNERROR", null, 0, CONTROL));
				playing = false;
				gameFile = "mafia.txt"; //reset the game file
				startIdle();
				return;
			}	
		}
		
		if(spyMafia==false) //plain mafia + werewolf
		{
			//Find a seer. He cannot be a wolf, obviously.
			boolean isWolf = true;
					
			while(isWolf)
			{
				seer = (int) (Math.random() * players.size());
						
				if(!wolf[seer])
					isWolf = false;
			}
			
			this.sendNotice((String)players.get(seer),
				getFromFile("SEER-ROLE", (String)players.get(seer), 0, NOTICE));
				
			for(int i = 0 ; i < players.size() ; i++) //tell anyone that isnt a wolf that they are human
			{
				try { if(i%2 == 0) Thread.sleep(300); }
				catch(Exception x) { x.printStackTrace(); }
				if(!wolf[i] && i != seer)
					this.sendNotice((String)players.get(i),
						getFromFile("VILLAGER-ROLE", (String)players.get(i), 0, NOTICE));
			}
		}
		else //SpyMafia
		{
			//set roles as permutation
			//Knuth's algorithm - step 2 - swapping
			int swapPosition=0;
			int tempSwap=0;
			for(int i = 0 ; i < players.size() ; i++)
			{
				//Random (i,n) = i+Random (0,n-i)
				swapPosition =i+(int)(Math.random() * (players.size()-i));
				//swapping
				tempSwap=roles[i];
				roles[i]=roles[swapPosition];
				roles[swapPosition]=tempSwap;
				//if(debug) System.err.println(" "+roles[i]);
			}
			//if(debug) System.err.println();
			
			//send notice
			
			for(int i = 0 ; i < players.size() ; i++) //tell anyone that isnt a wolf that they are human
			{
				try { if(i%2 == 0) Thread.sleep(300); }
				catch(Exception x) { x.printStackTrace(); }
				if(roles[i]!=BADDIE)
					this.sendNotice((String)players.get(i),
						getFromFile("SEER-EXT-ROLE", (String)players.get(i), 0, NOTICE));
				else //wolf
				{
					wolves.add(players.get(i));
					wolf[i] = true;
					this.sendNotice((String)players.get(i),
							getFromFile("WOLF-ROLE", (String)players.get(i), 0, NOTICE));
				}
			}
		}
	}
	//MMBot: post roles at the end of game.
	protected void printRoles()
	{
		String message="Roles were:";
		this.sendMessage(gameChan,message);
		
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(players.get(i) != null)
			{		
				message =""+Colors.DARK_GREEN;
				message +=(i+1)+". "+players.get(i)+ " - ";
				if(spyMafia==true) 
				{
					if(roles[i]==BADDIE)
						message+=Colors.RED;
					message += roleString(roles[i]);
				}	
				else
				{
					if(wolf[i])
					{
						message+=Colors.RED+getFromFile("ROLE-WOLF",null, 0, NARRATION);
					}
					else if(seer==i)
					{
						message+=getFromFile("ROLE-SEER",null, 0, NARRATION);
					}
					else
					message+=getFromFile("ROLE-VILLAGER",null, 0, NARRATION);
				}
									
				this.sendMessage(gameChan,message);
			}
			else
			{
				message =""+Colors.DARK_GRAY;
				message +=(i+1)+". "+ "<Player fled>";
				message +=" - DEAD";
				this.sendMessage(gameChan,message);
			}		
		}
	}
	//checks winning conditions and stops the game
	protected boolean checkWin()
	{
		int humanCount = 0,	//count how many humans are left
			wolfCount = 0;	//count how many wolves are left
			
		for(int i = 0 ; i < players.size() ; i++)
		{
			if(!wolf[i] && !dead[i] && players.get(i) != null)
				humanCount++;
			else if(wolf[i] && !dead[i])
				wolfCount++;
		}
		
		if(wolfCount == 0)	//humans win
		{
			playing = false;
			this.sendMessage(gameChan,
				getFromFile("VILLAGERS-WIN", null, 0, NARRATION));
			this.sendMessage(gameChan,
				getFromFile("CONGR-VILL", null, 0 , NARRATION));
			
			printRoles();
			
			doVoice(false);
			this.setMode(gameChan, "-mN");
			
			day = false;
			
			for(int i = 0 ; i < players.size() ; i++) 
			{
				dead[i] = false;
				lynched[i]=false; //redundant probably;
				inactive[i]=false;
			}
				
			gameTimer.cancel();		//stop the game timer, since someone won.
			gameTimer = null;
			reminderTimer.cancel();		//stop the game timer, since someone won.
			reminderTimer = null;
			interTimerType=NOTIMER;
			//reset the game file to default
			gameFile = "mafia.txt";
			
			//start the idling again
			startIdle();
			
			return true;
		}
		else if(wolfCount == humanCount)	//wolves win
		{
			playing = false;
			if(players.size() < TWOWOLVES)
			{
				String wolfPlayer = "";
				for(int i = 0 ; i < players.size() ; i++)
				{
					if(wolf[i])
						wolfPlayer = (String) players.get(i);
				}
				
				this.sendMessage(gameChan, 
					getFromFile("WOLF-WIN", wolfPlayer, 0, NARRATION));
				this.sendMessage(gameChan,
					getFromFile("CONGR-WOLF", wolfPlayer, 0 , NARRATION));
			}
			else
			{
				String theWolves = getFromFile("WOLVES-WERE", null, 0, CONTROL);
				
				for(int i = 0 ; i < wolves.size() ; i++)
				{
					if(wolves.get(i) != null)
						theWolves += (String) wolves.get(i) + " ";
				}
				
				this.sendMessage(gameChan, Colors.DARK_BLUE +
					getFromFile("WOLVES-WIN", null, 0, NARRATION));
				this.sendMessage(gameChan,
					getFromFile("CONGR-WOLVES", null, 0 , NARRATION));
				this.sendMessage(gameChan, theWolves);
			}
			
			printRoles();	
			doVoice(false);
			this.setMode(gameChan, "-mN");
			
			for(int i = 0 ; i < players.size() ; i++) 
			{
				dead[i] = false;
				lynched[i]=false; //redundant probably;
				inactive[i]=false;
			}
			
			day = false;
			
			gameTimer.cancel();		//stop the game timer, since someone won.
			gameTimer = null;
			reminderTimer.cancel();		//stop the game timer, since someone won.
			reminderTimer = null;
			interTimerType=NOTIMER;
			//reset the game file to default
			gameFile = "mafia.txt";
			
			//start the idling again
			startIdle();
			
			return true;
		}
		else	//No-one wins, game goes on
			return false;
	}
	//main starts constructor
	public static void main(String args[])
	{
		new Werewolf();
	}
	//Timer ruled tasks for join/day/vote/night/idle tasks
 	private class WereTask extends TimerTask
 	{
 		public void run()
 		{
 			if(interTimerType!=NOTIMER)
 			{ //just remind then exit
 				switch(interTimerType)
 				{
 				case JOINTIMER:
 					Werewolf.this.sendNotice(gameChan, 
 							Werewolf.this.getFromFile("REMINDER-JOINTIME", null, remainingTime, NOTICE));
 					if(remainingTime>60) //60sec=1min
					{
						remainingTime-=60;
						Werewolf.this.reminderTimer.schedule(new WereTask(), 60000);
					}
					else
					{	interTimerType=NOTIMER;
						Werewolf.this.gameTimer.schedule(new WereTask(), remainingTime* 1000);
						remainingTime=0;
					}

 					break;
 				case DAYTIMER:
 					Werewolf.this.sendNotice(gameChan, 
 							Werewolf.this.getFromFile("REMINDER-DAYTIME",null, remainingTime/60, NOTICE));
 					if(remainingTime>60) //60sec=1min
					{
						remainingTime-=60;
						Werewolf.this.reminderTimer.schedule(new WereTask(), 60000);
					}
					else
					{	interTimerType=NOTIMER;
						Werewolf.this.gameTimer.schedule(new WereTask(), remainingTime* 1000);
						remainingTime=0;
					}

 					break;
 				case VOTETIMER:
 					Werewolf.this.sendNotice(gameChan, 
 									Werewolf.this.getFromFile("REMINDER-VOTETIME", null, remainingTime/60, NOTICE));
 					if(remainingTime>60) //60sec=1min
					{
						remainingTime-=60;
						Werewolf.this.reminderTimer.schedule(new WereTask(), 60000);
					}
					else
					{	interTimerType=NOTIMER;
						Werewolf.this.gameTimer.schedule(new WereTask(), remainingTime* 1000);
						remainingTime=0;
					}

 					break;
 				case NIGHTTIMER:
 					Werewolf.this.sendNotice(gameChan, 
 							Werewolf.this.getFromFile("REMINDER-NIGHTTIME", null, remainingTime/60, NOTICE));
 					if(remainingTime>60) //60sec=1min
					{
						remainingTime-=60;
						Werewolf.this.reminderTimer.schedule(new WereTask(), 60000);
					}
					else
					{
						interTimerType=NOTIMER;
						Werewolf.this.gameTimer.schedule(new WereTask(), remainingTime* 1000);
						remainingTime=0;
					}
 					break;
 				default: 
 					break;
 				}
 				//reset timer type
 				return;//nothing to do but *yawn*
 			}
 			
 			if(playing)
 			{
	 			if(gameStart) //the start of the game
	 			{
		 			gameStart = false; //stop the joining
		 			
		 			wolfVictim = new Vector(1, 1);
		 			votes = new Vector(1, 1);
		 			
		 			voted = new boolean[players.size()];
		 			wolf = new boolean[players.size()];
		 			dead = new boolean[players.size()];
		 			notVoted = new int[players.size()];
		 			wasVoted = new int[players.size()];
		 			finalvote = new boolean[players.size()];
		 			abstain = new boolean[players.size()];
		 			roles = new int[players.size()];
		 			lynched = new boolean[players.size()];
		 			inactive = new boolean[players.size()];
		 			toSpy = new int[players.size()];
		 			
		 			for(int i = 0 ; i < players.size() ; i++)
		 			{
		 				voted[i] = false;	//initiate if people have voted to false
		 				wolf[i] = false;	//set up the wolf array. No one is a wolf at first.
		 				dead[i] = false;	//set up the dead array. No one is dead at first.
		 				notVoted[i] = 0;	//set up the not voted count. There are no non voters at first.
		 				wasVoted[i] = 0;	//set up the vote count. Nobody has any votes at first.
		 				finalvote[i] = false;
		 				abstain[i] = false;
		 				lynched[i] = false;
		 				inactive[i] = false;
		 				roles[i]=i;			//initial step
		 				toSpy[i]=-1;		//set up toSpy array for SpyMafia
		 			}
		 			
		 			Werewolf.this.sendMessage(gameChan, Colors.DARK_GREEN + "Joining ends.");
		 			Werewolf.this.printRoster(false);
		 			Werewolf.this.setRoles();
		 			
		 			//if there are 5 players, day comes first
		 			if(players.size() == 5&&spyMafia==false) 
		 			{
		 				day = true;
		 				isFirstDay=true;
		 			}
		 			else isFirstDay=false;
		 			
		 			//once everything is set up, start the game proper
					playGame();
		 		}
		 		else if(day) //the day ends
		 		{
		 			day = !day;
		 			
		 			timeToVote = true;
		 			
		 			playGame();
		 		
		 		}
		 		else if(timeToVote) //voting time begins
		 		{
		 			timeToVote = false;
		 			
		 			tallyVotes();
		 			
		 			votes = new Vector(1, 1);
		 			
		 			for(int i = 0 ; i < voted.length ; i++)
		 			{
		 				voted[i] = false;
		 			}
		 			
		 			for(int i = 0 ; i < wasVoted.length ; i++)
		 			{
		 				wasVoted[i] = 0;

		 			}
		 			for(int i = 0 ; i < abstain.length ; i++)
		 			{
		 				abstain[i]=false;

		 			}
		 			for(int i = 0 ; i < finalvote.length ; i++)
		 			{
		 				finalvote[i]=false;
		 			}
		 			
		 			toSee = -1;
		 			
		 			checkWin();
		 			playGame();
		 		}
		 		else if(!day) //the night ends
		 		{
		 			
		 			wolfKill();
		 			
		 			wolfVictim = new Vector(1, 1);
		 			
		 			day = !day;
		 			checkWin();
		 			
		 			playGame();	
		 		}
		 	}
		 	else //idle chat.
		 	{
		 		User[] users = Werewolf.this.getUsers(gameChan);
		 		String rand;
		 		do
		 		{	rand = users[((int)(Math.random() * users.length))].getNick();	}
		 		while(rand.equals(Werewolf.this.getNick()));
		 		
		 		String msg = Werewolf.this.getFromFile("BORED", rand, 0, Werewolf.this.NOTICE);
		 		if(msg != null)
		 			Werewolf.this.sendMessage(gameChan, msg);
		 		Werewolf.this.startIdle();
		 	}
 		}
 	}
}