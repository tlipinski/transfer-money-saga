# Transfer Money Saga (Process Manager)

The main purpose of this project was to see what requires special attention to when implementing Saga/Outbox patterns, what are the potential problems when such implementation is invalid or used in unstable environment. The project contains simplifications as well as intentional impediments and should not be treated as model implementation.
You'll find here concepts from event driven architecture like:

- events and commands
- Saga / Process manager
- outbox pattern
- "exactly once" delivery

You won't find:
- event sourcing
- CQRS

Technically concept presented here is not Saga but Process Manager (it is a state machine with persisted state).
This project started with simplistic approach and back then it might have been a Saga but at some point when
it got a bit more complex it somehow turned into Process Manager. Name Saga is still being used throughout the project although it may not be a proper name for implemented pattern.

## Running

1. Start up Couchbase and Kafka:
```shell
$ docker-compose -p tms-infra -f docker-compose-infra.yml up -d
```

2. Wait a bit for Couchbase to start and run: 
```shell
$ ./couchbase-init.sh
```
It creates all necessary buckets, collections and indexes.
`docker-compose-infra.yml` defines volume for Couchbase container
so this step is required to perform only once unless you remove the volume.

3. Open another terminal and enter sbt shell with `sbt` command and build projects with:
```
sbt> docker:stage
```

5. Go back to previous terminal and start services
```shell
$ docker-compose -p tms -f docker-compose.yml up
```

6. Sbt shell again - run transfers with:
```
sbt> test-runner/run 100 100 1500
```

It accepts 3 parameters:
- number of users involved in money transfers - at least 2
- transfers  - how many transfers will be made
- max amount of transfer - default user's balance is 1000 so setting this value higher than 1000
will make sagas more often to roll back because of user balances being too low

## Verification

Consistency of data can be checked by running
```shell
watch ./db
```

If there are 100 users in test and initial balance is 1000 then total balance of all users
must be equal to 100k. While the test is running it won't be true because there will be many
transfers still in progress but as soon as all events are produced and processed it will be consistent

While test is running:
```
=== BALANCES ===
Total:            79986
Pending trans.:   48
Processed trans.: 156

=== OUTBOX ===
Sent:     563
Unsent:   42

=== SAGAS ===
Completed:    3
Rolled back:  42
In progress:  55
```

After it completes:
```
=== BALANCES ===
Total:            100000
Pending trans.:   0
Processed trans.: 200

=== OUTBOX ===
Sent:     674
Unsent:   0

=== SAGAS ===
Completed:    58
Rolled back:  42
In progress:  0
```

### Chaos monkey

To prove delivery guarantees you can try messing around with containers with command like:
```shell
$ pumba restart -t 30s "re2:tms-.*"
```

It will restart all containers (including infra - Couchbase, Kafka and Zookeeper!) one after another
and the end result will be that all transfers will be processed and total balance for all user balances
will be consistent

## Project structure

### bank

Defines Balance aggregate for money transfer transactions processing. It holds lists of pending and processed transactions
without any size or time limits - it's only for simplicity.
Bank doesn't expose any HTTP API. It only consumes commands and sends replies using outbox.
`BlanceRepo` simulates failures randomly on reads and saves to test retries and eventually dead letter queue.

### transfers

Entry point for money transfer (with HTTP server). Defines steps for saga in a declarative way.
Saga intentionally defines transferring money to the user first (which always succeeds) before transferring money from 
the other user (may fail) just to simulate compensations. 
```scala
SagaDefinition.create[MoneyTransfer, BankEvent, BankCommand](
  Step(
    id = "credit",
    command = transfer => transfer.creditBalance,
    compensation = transfer => transfer.rejectBalanceCredit.some,
    handleResponse = {
      case (event: BalanceChanged, transfer: MoneyTransfer) if matches(event, transfer, _.credited) =>
        SagaForward
    }
  ),
  Step(
    id = "debit",
    command = transfer => transfer.debitBalance,
    compensation = transfer => transfer.rejectBalanceDebit.some,
    handleResponse = {
      case (event: BalanceChanged, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
        SagaForward

      case (event: BalanceNotChanged, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
        SagaRollback
    }
  ),
  Step(
    id = "approve-debit",
    command = transfer => transfer.approveBalanceDebit,
    compensation = _ => none,
    handleResponse = {
      case (event: BalanceApproved, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
        SagaForward
    }
  ),
  Step(
    id = "approve-credit",
    command = transfer => transfer.approveBalanceCredit,
    compensation = _ => none,
    handleResponse = {
      case (event: BalanceApproved, transfer: MoneyTransfer) if matches(event, transfer, _.credited) =>
        SagaForward
    }
  )
)
```

### necromant

Consumes dead letter queue and re-sends failed messages.

### outbox

Reads outbox messages from database and pushes them to Kafka

### saga (lib)

Defines:

- `SagaDefinition` - it's basically a list of saga steps with commands to send and event handling
- `Saga` - it's `SagaDefinition` with current `Stage`. Saga is rather simplified and it assumes that
only one command is sent at any given step. Only when saga rolls back it can cause more commands to be sent.
Additional saga data like user ids or transfer amount is immutable for the whole saga life span.

### consumer (lib)

Common message processing with fs2-kafka.

### database (lib)

All documents are stored in Couchbase. This library hides all details related to this specific database.
Probably it's not a good idea to make database client wrappers like this but it just makes this example
code a bit cleaner

### util (lib)

Absolute minimum of commonly used code.

## Improving Balance aggregate

I started with Balance aggregate which was doomed for failure but improving it step by step resulted
eventually in an implementation which allows surviving different kinds of failures.

###  v1
```json
{
  "balance": 1000
}
```

- no isolation between transfers - decision are made based on balance which is in the middle of modifying by other transfers
- no idempotency - messages can be processed more than once

### v2
```json
{
  "balance": 1000,
  "pending": 0
}
```

- transfers are isolated
- no idempotency - messages will be processed more than once

### v3
```json
{
  "balance": 1000,
  "pending": [
    {
      "id": "001",
      "amount": 555
    },
    {
      "id": "002",
      "amount": 666
    }
  ]
}
```
- transfers are isolated
- idempotency fixed to some point but still transfer "001" can be processed again in case service restarts
and some messages are being reprocessed
  
### v4
```json
{
  "balance": 1000,
  "pending": [
    {
      "id": "002",
      "amount": 555
    }
  ],
  "processed": [
    "001"
  ]
}
```
- does not allow to process any completed transfer again

### v5
Last detail is marking failed transfers ("transfer amount too big") as processed as well.
When service is restarted and some messages are being reprocessed
then some message might result in different answer than previously but other side of transfer may be already rejected:

Assuming initial balances are 1000
- `[u01]` apply transfer +1500 - applied
- `[u02]` apply transfer -1500 - failed because balance is too low
- `[u01]` reject transfer +1500
- ------------- restart ------------
- (reprocessing some messages for `u02`)
- `[u02]` (reprocess) apply transfer -1500 - applied because now balance might be high enough
- `[u01]` already rejected this transfer
