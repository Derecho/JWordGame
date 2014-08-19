# JWordGame

JWordGame is an IRC word game bot written in Java.

## Disclaimer
I've written this program with the goal of learning Java.
This was my first Java program and I was only starting to grasp the concepts of
object-oriented programming at the time, viewer discretion is advised.

Some parts of the code may be a little specific to the environment I am using it
in.

I do not plan on making large changes to this program anymore at this point in
time, pull requests are however welcome.

## Game description
This game works by setting and guessing certain words during regular IRC
conversation. Any sentence you type in a channel participating in the game can
cause a set word to be guessed. Both the guesser and setter of the word receive
points when this happens, and the guesser will be able to set a word of his own.

As a word setter, your goal is to mention your word as often as you can without
drawing suspicion. You can get one mention per sentence, actions (/me) are also
parsed for mentions. Mentioning your word as part of another word is valid. The
more mentions you manage to squeeze in before your word is guessed, the more
points you will get when this finally occurs. You can use the `listwords`
command to keep track of your current set words and it will also show you how
much mentions you have accumulated so far.

## Getting started
Compile the game by issuing `ant` and run it with:

    java -jar JWordGame.jar

The console output will show a randomly generated admin password you can use to
identify yourself as an admin. Send a PM to the bot with:

    admin <password>

Now you will probably want to start a game and have the bot join a channel:

    newgame <#initialchannel>
    join <#initialchannel>
 
 I recommend you to turn the autosave feature on. First you need to find out the
 ID of your newly created game with `listgames` after which you issue `autosave
 <gameid>`.

 You can get a list of the other admin commands by running `adminhelp`.
 Running `!wghelp` in a participating channel will explain how to sign up and
 inform users about the available commands.
