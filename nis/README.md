Merge Thies' peer
-----------------

I wasn't sure if both of you will be ok with the changes, so I've put it on separate branch

* dropped NEMServer
* moved most of stuff from org.nem.NEM to org.nem.peer.PeerInitializer
* I think we shouldn't pollute web.xml with our stuff so I've changed reading of settings to JSON file peers-config.json
* I've used jetty's HttpClient for communication: http://www.eclipse.org/jetty/documentation/current/http-client.html
* Controllers in org.nem.nis.controller are handling the calls
* I have host running, which is added to peers-config.json, so you should be able to try it out, here's sample output
```
[INFO] Started Jetty Server
17:46:23,605  INFO PeerInitializer:51 - NIS settings:
17:46:23,617  INFO PeerInitializer:58 -   "myPort" = 7890
17:46:23,618  INFO PeerInitializer:67 -   "myAddress" = "localhost"
17:46:23,622  INFO PeerInitializer:77 -   "myPlatform" = "ZX Spectrum"
2014-02-19 17:46:23,623 [org.nem.peer.Node getNodeInfo] WARNING: node/info url: http://37.187.70.29:7890/node/info
2014-02-19 17:46:23,858 [org.nem.peer.Node getNodeInfo] WARNING: node/info response: {"port":"7890","shareAddress":true,"platform":"PC x64","protocol":1,"application":"NIS","scheme":"http","version":"0.1.0"}
2014-02-19 17:46:23,860 [org.nem.peer.Node extendNetworkBy] WARNING: node/info url: http://37.187.70.29:7890/node/info
2014-02-19 17:46:23,951 [org.nem.peer.Node extendNetworkBy] WARNING: peer/new response: {"error":1,"reason":"trust no one"}
```


Some configuration is in XML files, the rest of the stuff is annotation based.


DB setup
--------

For testing purposed I was using mariaDb (mysql descendant). I hadn't time, to try sqlite before pushing it.
```
Db settings are in:       src\main\webapp\WEB-INF\database.properties
hibernate db settings in: src\main\webapp\WEB-INF\application-context.xml

log4j:                    src\main\resources\log4j.properties
```

I've added mariadb connector in the repo, I couldn't found maven server with recent version.

mariadb setup
```
> mysql.exe -u root --password=rootpass

MariaDB [(none)]> CREATE DATABASE `nis`;
MariaDB [(none)]> GRANT CREATE, ALTER, INDEX, INSERT, SELECT, UPDATE, DELETE, DROP ON `nis`.* to 'nisuser'@'localhost' identified by 'nispass';

mysql.exe nis -u nisuser --password=nispass < nem-infrastracture-server\createTables.sql
```
(I know, I know, tables creation should rather be in code,
 also the sql, does not contain sensible primary keys now).


Running nis
-----------

```
mvn clean compile jetty:run
```

Flow
----

I've put main class as a bean inside application-context, and `init()` method inside this class has `@PostConstruct` annotation.
(I'm quite aware, that this is probably ugly way, I just wanted to get to more interesting things faster)

First thing `init()` does is call to `populateDb()`, which in turn calls `populateGenesisAccount()`, `populateGenesisBlock()` and `populateGenesisTxes()`.

Right after that `analyze()` from `AccountsState` is called, which does some initial (dummy) analysis.


The server itself should answer to /getInfo **POST** request on 7890 port
```
wget http://localhost:7890/getInfo --post-data="" -q -O -
```

This is done from `org.nem.nis.controller.InfoController`

Other
-----

Some other things you'll find there.
In `NcsMain`, there is big commented-out `unusedThread()` method, which was using
`NxtRequests` to communicate with NXT server running on `localhost:7874`.

The code won't compile/work now, due to differences in Account, Block, etc,
but you can take a look.


To Discuss
----------

**I've tried adding bouncycastle to pom.xml, but with that JETTY seems to be hanging for 2 minutes before starting**
No idea why...


In the block and transfer tables I've left "shortId" fields, that nxt uses (8 bytes of sha of block).

Do we want it need it?
*pro*: it be easier to find blocks/transactions in db
*con*: collisions might happen...

Maybe it'd be better to keep block height / transaction height in db?


I claimed many times on bct forums, that (since peer is sending us blocks in order)
in case of talking to peer, we can meet only two situations:
 * (verify and) append what he set us to the chain
 * we're on fork, if his chain is "better", drop our "tail", and append his

I was thinking, that when talking to peer, we should keep his chain on
a "VirtualChain", and if it passes verifications, and indeed is better than what we have now
it would be added to db.
Take a look at mentioned commented `unusedThread` and classes `org.nem.nis.virtual`.
(There's not much there, but I'm wondering, what will you think about the idea itself).


Fun
---

I've used 0x69 as network and 0x18 as version to get NEM at the beginning after base32 ;)

Sample from startup:
```
starting analysis...
analyzing block: 1311768467294899695
9
1 0 1 NEMLBAG7WBMB44MW3OUJCCZT3KXPHJLCGRRIGBD2K3DP5GVB
2 1 2 NEMMYOYNMMETMPY6CNVPXKO2LWNQLF2IXBHUTAAGLAGWALRP
3 2 3 NEMDHJ4U77NUSUC2VQL3UY2FJ2EW5J4DUFVJBFURH6ZI7DSR
4 3 4 NEMMBNE2MFCOQLLTK22DSOABYYD7PJXWPK4MBGCP743SEQ3G
5 4 5 NEMMSN6IWSEGCCI6HTWKEWGAAJATJQAN7PDYBK7ZL3RT5RCD
6 5 6 NEMFKKG5WD5H3WOZCSHJHTGUI6JSA3B5FYLNJF2HO6QXBUPN
7 6 7 NEMCXJINROIZJOFSA3DCTPY2IUMQKYZHOKSFEF3Q7OPHSQEA
8 7 8 NEMOZ6TSMHTGUTCJHJWWO4IPJGLP5VSHACT4BOBW5M4R35KJ
9 8 9 NEMDDIT4WCPIGKG7HYJWCZY4EEEU4BPCUUAU43EO7DOWURLE
```

