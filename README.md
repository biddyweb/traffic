Traffic
====

A project for CS032 to draw maps to screen, calculate shortest paths and give directions.

##IDEAS##
Executables have string pass through that show client or server, main takes that and deals appropriately

##Authors##
aiguha
dgattey

##Backend Project Components ##
1. Autocorrect: dgattey
2. Stars: aiguha
3. Bacon: aiguha


## Known Bugs ##
None at this time.

##Design Details##


##Optimizations##
Multi Threading
1. Initial loading
2. Predictive paging for GUI
3. Route finding for GUI

##Protocol for Sending/Receiving##

Dropping client on server as easy as socket.close() and happens if footer doesn't get sent
On client, get IOException, closes gracefully, error message shows in dialog box
Switch statement in Server and Client that correctly goes both directions

##How to Run Tests ##


