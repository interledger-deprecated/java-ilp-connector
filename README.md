# Java Interledger Connector [![join the chat on gitter][gitter-image]][gitter-url] [![circle-ci][circle-image]][circle-url] [![codecov][codecov-image]][codecov-url]

[gitter-image]: https://badges.gitter.im/interledger/java-ilp-connector.svg
[gitter-url]: https://gitter.im/interledger/java-ilp-connector
[circle-image]: https://circleci.com/gh/interledger/java-ilp-connector.svg?style=shield
[circle-url]: https://circleci.com/gh/interledger/java-ilp-connector
[codecov-image]: https://codecov.io/gh/interledger/java-ilp-connector/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/interledger/java-ilp-connector

A Java foundation for an Interledger Connector, patterned off of the Javascript 
Connector [ilp-connector](https://github.com/interledgerjs/ilp-connector).

* v0.2.0-SNAPSHOT Initial commit of Routing interfaces.
* v0.1.0-SNAPSHOT Initial commit of interfaces and abstract class to construct a Java Connector.
 
## TODO

- [x] Routing interfaces and abstractions.
- [] Database-backed routing table.
- [] Quoting interfaces and abstractions.
- [] Unit Tests
- [] Transition to hyperledger quilt project.
- [ ] Update gitter and other badges...

* The Issues List on [on Github](https://github.com/interledger/java-ilp-connector/issues).

## Architecture
This project is the basis for an Interledger Connector, but it is _not_ itself a complete Connector implementation. 
Instead, this project provides interfaces and abstract implementations that illustrate one way to organize a 
Connector in Java, including configuration, startup, ledger communication, routing, and quoting.

### ILP Plugin Architecture
This project relies on an ILP Plugin architecture where plugins can be developed and installed into this Connector. The primary example of such a plugin is the 
[Ledger Plugin](https://github.com/interledger/java-ilp-plugin/blob/master/src/main/java/org/interledger/plugin/lpi/LedgerPlugin.java) defined in the [java-ilp-plugin](https://github.com/interledger/java-ilp-plugin) project. Implementations of LedgerPlugin are 
intended to be run inside of the Connector's runtime, allowing a Connector to interact with any type of ledger implementation 
(i.e., [XRP Ledger](https://ripple.com/build/), Bitcoin, Ether, 
[FiveBells](https://github.com/interledgerjs/five-bells-ledger), 
[Chain Sequence](https://seq.com/), 
etc) 
using a standard interface. 

The following diagram provides a high-level view of this arrangement:

```text
 Events/Messages──────────────┬────────────────Events/Messages
     │                        │                       │
     │                        ▼                       │
     │         ┌────────────────────────────┐         │
     │     ┌───│         Connector          │───┐     │
     │     │   └────────────────────────────┘   │     │
     │     │                  │                 │     │
     │  g.usd.                │              g.jpy.   │
     │     │               g.eur.               │     │
     │     │                  │                 │     │
     │     │                  │                 │     │
     │     ▽                  ▽                 ▽     │
     │  ┌─────┐            ┌─────┐           ┌─────┐  │
     └──│LPI-1│            │LPI-2│           │LPI- │──┘
        └─────┘            └─────┘           └─────┘
           △                  △                 △
           │                  │                 │
           ▽                  ▽                 ▽
      ┌──────────┐       ┌──────────┐      ┌──────────┐
      │ Ledger 1 │       │ Ledger 2 │      │ Ledger 3 │
      └──────────┘       └──────────┘      └──────────┘
```

### Quoting
TBD.

### Routing
TBD.
 
## Usage

### Requirements
This project uses Maven to manage dependencies and other aspects of the build. 
To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

### Get the code

``` sh
git clone https://github.com/interledger/java-ilp-connector
cd java-ilp-connector
```

### Build the Project
To build the project, execute the following command:

```bash
$ mvn clean install
```

#### Checkstyle
The project uses checkstyle to keep code style consistent. All Checkstyle checks are run by default during the build, but if you would like to run checkstyle checks, use the following command:


```bash
$ mvn checkstyle:checkstyle
```

### Step 3: Extend
This project is meant to be extended with your own implementation. The following is a list of open-source 
implementations of an Interledger Connector, built upon this project:
 
 * [TBD]()

## Contributors

Any contribution is very much appreciated! 

[![gitter][gitter-image]][gitter-url]

## License
This code is released under the Apache 2.0 License. Please see [LICENSE](LICENSE) for the full text.