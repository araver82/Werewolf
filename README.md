# WerewolfBot

An IRC bot for hosting a game of Den Mafia!

* The bot acts as a host in instances of the game: assigning roles to players, narrating what happens in the game during day/night phases, collecting people votes and computing the results.
* This is a fork of the [Werewolf IRC Game](http://javatwg.sourceforge.net/)
* For rules & background information see [Den Mafia's guides](http://mafiamaniac.net/index.php?showforum=3)

## Features

This version has 4 different games (Werewolf, Star Trek, 4-Spy Mafia) each with it's separate file containing messages:
* `!spymafia` (4-Spy Mafia)(NEW): 5 players, 4 types of spies
* `!start` (for Den Mafia): 5-12 players, baddie(s) vs vaniila + spy goodies
* `!wolfgame` (for Original Wolfgame): 5-12 players, 2nd wolf introduced at 8 players, Seer roles
* `!star-trek` (for StarTrek variant of the Original Wolfgame)

## Install

* Requires Java Runtime Enviroment 1.4+ and Internet Access
* Unzip the contents to a directory of your choice, keeping directory structure intact 

## Config

* Edit the `mmbot.ini` file for the following options:
** Give the bot a name
** Provide the address of the network to conenct to
** Provide the name of the channel the bot should join
** Provide the nick the bot should message to ident it's nick (For channel privledges, such as aop). If you don't want to reg the bot's nick, put `none` in this field.
** Provide the command used to authenticate the bot's nick. for example, if you would normally auth with `/msg Nickserv ident <pass>`, here you would put `ident <pass>`
** Determine whether debug/file logging is on or off. Logs are saved to files (e.g. `MMBotOut.log`, `MMBotErr.log`).
** Provide a message delay. This is to prevent the bot flooding itself of the server from send too many messages consecutively. 500 usually works, but try increasing it if you have problems.
** Give values for the various game durations, determine whether the vote tie is on or off by default and choose whether to enable or disable idle chat from the bot.
* The game reads texts from `.txt` files in the game dir (one for each type of game: `mafia.txt`, `startrek.txt`, `wolfgame.txt`). If you wish to edit the text, the headers should remain EXACTLY the same, and a blank line should be between each category of text.
* The game has help for everycommand, texts are taken from `help.txt`

## Run

Linux: use (note the '&' symbol at the end is to run in the background)
```sh
$ run.sh
```

Windows: use
```sh
$ run.bat
```

## License

This bot was developed under the [GNU Public License v2](http://jibble.org/licenses/gnu-license.php)

## Credits

* Coding (Den Mafia, Spy Mafia and bugfixing): *Araver*, tested by the [MM community](http://mafiamaniac.net) on `irc.wikkedwire.com` `#mafiamaniac`. Many thanks to *Maurice* (for `#aravice` testing ground), *GMaster479* and the folks at [wikkedwire.com](http://wikkedwire.com/)
* Original Coding The Werewolf Game IRC Bot (v 0.99b): Mark Vine alias LLamaBoy
* Texts/Descriptions Author and Ideas Man: Alex Denham alias Darkshine.
* Beta Testers: coolsi, Mas_Tnega, MrDictionary, Icepick, Long_Shoota, LowStream, VirulentVirus, danv2b, FIREFOX, alse, caine, NeoThermic, ScareyedHawk, AgentX, Pathy, DevilX4, WolfLord, CrazyPixie, Skorp, Xtreme, Zaptan , Deepsmeg and many more for over 100 hours of play testing on the UplinkIRC network (irc.uplinkcorp.net)
* Creator of the original PircBot framework: Jibbler aka Paul Mutton

## Changelog

Changes in gameplay:
* prints roster after joining, beginning of each night (to see alive players) and after lynch results.
* finish joining after MAXPLAYERS=12 (or 5 for SpyMafia) join
* added new vote tallying system (for SpyMafia ONLY): If at least half of the people alive are abstaining then no one gets lynched.
* added command abstain aka NO LYNCH
* added command finalvote - to mark vote as final and not changeable anymore
* check votes for final and end day early if all votes are final
* added commands voteno, killno, spyno, seeno, finalvoteno - to act on a roster position instead of typing a player name
* added first-day tie rule (separate from tie rule) and added messages on vote-starting on how ties are going
* added messages "Lynched and found to be ..." / "Killed by Baddies"
* added reminders for day/night "Vote will end in ..."
* added round number and daypost / nightpost / action result separator!
* added plead no contest ability
* added joinTime to the list of run-time customed timers (default 2 minutes now)
* added rosterDisplay
* added changeVote rule
* added removeVote possibility
* added possibility to invoke roster (seevotes)
* changed default text file to Mafia Goodie / Baddie / spy.
* See [changelog](WhatsNew.txt) for the history.