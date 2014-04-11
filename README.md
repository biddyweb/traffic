Traffic
========
*By aiguha and dgattey*

A project for CS032 to draw maps to screen and give directions. Instantiated by
map data in the form of a ways file, nodes file, and index file, as well as an
optional traffic server. The server can run a large number of clients with one
copy of data in memory.

###Backend Project Components
1. Autocorrect: dgattey
2. Stars: aiguha
3. Bacon: aiguha

###Known Bugs
None at this time!

###How to Run
- **Creating the executables:** ant create_exec && ant jar
- **Running server:** bin/trafficServer info/ways.tsv info/nodes.tsv
 info/index.tsv localhost 9999 10000
- **Running client:** bin/trafficClient localhost 10000 (with
   --debug flag if you want errors to print)

##Design Details
We took our frontend package from Maps and made it the **client** package.
 The backend package became the **server** package. Because of our earlier
separation, we were able to simply change the HubController and add a
**core** server package and everything else basically stayed the same.
The **main** package has a Main class that given the appropriate number of
flags and the correct arguments, starts up either a ClientApp or a ServerApp.
Given our interfaces, everything is pretty seamless.

We have a server, client, data, and main package on the top level. Data is shared
by both server and client, so there are connections there. But nothing in the
client or server packages import the other's code.

###Client
The client simply spawns a GUI and attempts a connection. It starts a timer that
attempts a connection every few seconds. If it can't connect, the bar at bottom
will be red, all traffic data disappears, and the route disappears. If there's a
connection, the bottom bar is green and everything works perfectly. Routing works
by clicking two points or entering four streets and pressing the Route button.
If nothing is found, the appropriate status will be shown. Otherwise, it'll move
you to the start location of the route (and highlight it). Traffic is shown on
the map from a range of yellow to red, red being worst traffic. White is no traffic.
As you zoom out, small roads (under 3 pixels) will disappear for quicker drawing.
Anything outside the bounds of the current view won't even be drawn. Chunks load
themselves when the view bounds get close enough to another chunk. Everything
goes through the HubController which makes a CommController to communicate with
the server. We used Callables to return a value from that thread, and an
ExecutorService to run it.

###Server
The server consists of five main packages. Four former backend packages: io, autocorrect,
kdtree and graph, which manage File IO, autocompletion, nearest neighbor searches
and shortest path calculations. These remained almost completely unchanged from Maps,
as they were already prepped for multithreading. The graph package involved minor additions
to enable realtime traffic-based route calculation. The fifth package is core, which
sets up the multithreaded server and handles network communication with all clients
and the traffic server.

The server starts handling each client on a new thread, allowing the main thread
to stay open for command-line interaction from the administrator (if any). As per
our protocol, every request from the client is made on a new socket (except traffic
requests) and responded to on the same one. Thus, one client typically has about four sockets
connecting to the server. This strategy eliminates the bottleneck of writing multiple responses
to one socket concurrently.

When a new socket connects, the request header is read and it is dispatched to one
of five response methods in ResponseController, which uses ACController, KDTreeController,
GraphController and TrafficController to construct responses.

The server, through TrafficController, is set up to continually try connecting to the traffic
server, and constantly read traffic data while connected. When traffic data is unavailable, the
server can still perform normally, including route calculation.


###Connections
Our client, server, and traffic bot all can be connected together in any order.
The client has a timer that attempts connection every few seconds and updates the
UI accordingly. Therefore, it doesn't matter if the client starts before or after
a server. Similarly, if the server can't send to a request, it removes it from its
pool and forgets about it. Additionally, the server attempts reconnection to the
traffic server if there's no connection every few seconds, so it can die and be
restarted and the server will keep accepting data. The command line interface
notifies you about connection status when you run 'status'. If there's no
connection, it saves last known traffic data for use when routing. Since the
client has no use for the data after a lost connection, it clears its traffic
data but keeps its map data.

##Optimizations
###Multithreading
1. Initial loading of all nodes (for KDTree)
2. Predictive paging for GUI
3. Route finding for GUI
4. 20 chunks loading at a time, no more

###Other
1. Only having one request for a route at a time
2. Canceling of threads on server and client
3. Reusing objects as much as possible (HashMaps and HashSets)
4. Each request runs on its own thread

###Communication Protocol
####Objects

#####Simple Objects

- LatLongPoint
- Error Messages

These are denoted as **#[object_tag]: [fields...]** on a single line.

    #llp: 41.12 -71.21

#####Complex Objects
- List[String]
- MapWay
- List[MapWay]


These are denoted as **<[object_tag]: {optional length}:\n
 [fields...] >**

    <way:
    /w/0001
    Thayer Street
    /n/0002
    #llp: 41.12 -71.21
    /n/0003
    #llp: 47.50 -78.401
    >

Other objects are similarly constructed.

####Requests And Responses

All queries have the following structure:

    @q:query_name:
    [object...]
    @x

All responses have the following structure:

    @r:response_name:
    [object...]
    @x



##Testing
Test our code by running the following:
- **Server system tests:** ant server_test
- **Client system tests:** ant client_test
- **JUnit tests:** ant unit_test
- **More Server tests** start the server on port 9999. Once it's open, 
run ./badRequest.sh and it should print out responses and expected responses

###By Hand
We extensively tested the interactions between the client and server by running
netcat and tossing bad data at both sides. Additionally, we started a server and
connected five clients to it and did everything we could to make it mad. Lots of
long routes, concurrent requests, same location requests, etc. Additionally, we
tested closing the connection from the server to the client and vice versa in
the middle of a request. No matter what order we did things, nothing failed!
Additionally, we modified the traffic bot to send us information every x amount
of time, and tested it with requests every 0.0000001 seconds. The whole map
filled up, but even with crazy amounts of data coming in, routes were found and
reflected the amount of traffic at the moment at which the route was created.
