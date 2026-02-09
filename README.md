# cypher-othello
This is a Java implementation of the board game Othello, a.k.a. Reversi (https://en.wikipedia.org/wiki/Reversi) where the logic of the computer players are done with Neo4j Cypher. The purpose is as a Cypher challenge where you can write Cypher implementations that compete with each other (or against a human).

Othello consists of a game board, which is a matrix with 64 (8*8) "cells". One player has black bricks and the other has white. In every turn you place a brick on an empty cell the board, and then you turn all oponent bricks that are between the brick you just placed and another of your bricks (vertically, horisontally or diogonally) over to match your color. For a valid move you need to turn at least one opponent brick. If there is no valid move the turn goes over to the opponent. If the opponent also doesn't have a valid move the game is over and the one with the most bricks with their color wins.\
There is an initial setup with four bricks at the center of the board, and Black player always starts.

The game is played against a Neo4j instance with a structure that looks like this:
```
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
|X|X|X|X|X|X|X|
O-O-O-O-O-O-O-O
```
The O's above reprenet nodes and the dashes, pipes and X's shows the relationships.\
Every cell in the matrix is represented by a node with the label **Cell** and the cells are connected horisontally, vertically and diagonally with relationships called **NEIGHBOR_OF**.
```
(:Cell)-[:NEIGHBOR_OF]->(:Cell)
```

The **NEIGHBOR_OF** relationships has one property called direction which can have four values:\
**1**: Downward to the right\
**2**: Vertical\
**3**: Downward to the left\
**4**: Horisontal

The **Cell** nodes has four properties:\
**row**: The row (y-coordinate) of the cell (0-7)\
**column**: The column (x-coordinate) of the cell (0-7)\
**state**: null if the cell is available, "X" if it is taken by Black or "O" if it is taken by white

**Note!!** that when the game is started it will clear everything from the instance it is connected against, so don't connect it to an instance where there is data you want to keep.

With the rules above and and this graph structure, we can devise the following Cypher query to determine if a move is valid ($symbol is the player who makes the move ('X' or 'Y') and $x/$y are the coordinates requested):
```
MATCH (c:Cell)
WHERE c.row = $y AND c.column = $x
WITH c,
CASE $symbol
  WHEN "X" THEN "O"
  ELSE "X"
END AS other
RETURN CASE
  WHEN c.state IS NOT NULL THEN false
  ELSE 
    EXISTS {
      p=(c)-[r]-(:Cell {state: other})
      ((:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: other})){0,5}
      (:Cell {state: other})-[{direction: r.direction}]-(:Cell {state: $symbol})
    }
END AS valid
```

There is an interface called Player with one method called getCypherForRound(). To implement a new player you create a class that implements this interface and you implement this method. The method should return the Cypher query as a string. This query must return one record with one entry called "cell", and this should point at the Cell node where you want to put your piece. In the Cypher query, black bricks are represted as "X" and white bricks as "O". Note that these queries will be run in read-only mode. If the player performs an invalid move (i.e. one that doesn't fullfil the query above) it will be disqualified and the game is over with the other player as the winner. Player class implementations must reside in the same package as the other classes (org.neo4j.othello).

The following parameters will be provided to the query:\
**$symbol**: The symbol used by this player (either "X" for Black or "O" for White) [String]\
**$apiKey**: An API key that one can be used to, for example, call AI procedures (supplied on the command line) [String]

There is an example Player included called Simple. This one looks for valid moves and randomly picks one of them. There is also a Player implementation that is handled a bit differently. It is called Human and allows you to click on the playing field to decide where to put your brick.

Compile the project with
```
mvn clean compile
```

And run it like this:
```
mvn exec:java -Dexec.args="Player_Black_ClassName Player_White_ClassName  DB_URI DB_USER DB_PWD DB_NAME API_KEY [TIME_LIMIT_S [BENCHMARK [FRAME_X FRAME_Y]]]"
```
**Player_Black_ClassName**: The class name (without package name) of the player implementation to play Black\
**Player_White_ClassName**: The class name (without package name) of the player implementation to play White\
**DB_URI**: The URI to the Neo4j instance to use, e.g. neo4j://localhost:7687 for a local instance or neo4j+s://xxxxxxxx.databases.neo4j.io for an Aura instance\
**DB_USER**: The Neo4j user (usually neo4j)\
**DB_PWD**: The password of that user\
**DB_NAME**: The name of the database to use (usually neo4j)\
**API_KEY**: An API key that one can be used to, for example, call AI procedures (will be passed to the queries, see above)\
**TIME_LIMIT_S**: A time limit, in seconds. If the query time exceeds this the player loses. However, the first move is never times as some warmup time might be needed. 0 or less to disable. Default -1.\
**BENCHMARK**: true to get a printout of all times taken for each move of each player after the game is over. Default false\
**FRAME_X/FRAME_Y**: Set to screen coordinates to force the frame to that position on the screen. Default center of screen.

For example:
```
mvn exec:java -Dexec.args="Player1 Player2 neo4j://localhost:7687 neo4j mypassword neo4j sk-proj-xxxxxxxxx 10 true"
```
