### v1

```json
{
  "balance": 1000
}
```

- no isolation - decision are made on balance which is 
  in the middle of modifying by other transfer
- no idempotence - messages can be processed more than once

### v2

```json
{
  "balance": 1000,
  "pending": 0
}
```

- isolation fixed
- no idempotence - messages can be processed more than once

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
- some idempotency fixed but still transfer "001" 
  can be processed again after service restart
  if all messages from a single transfer are
  processed from kafka
  
### v4
```json
{
  "balance": 1000,
  "pending": [
    {
      "id": "001",
      "amount": 555
    }
  ],
  "outbox": []
}
```
- atomic save and publish - responses sent with outbox pattern

### v5
```json
{
  "balance": 1000,
  "pending": [
    {
      "id": "002",
      "amount": 555
    }
  ],
  "outbox": [],
  "processed": [
    "001"
  ]
}
```
- does not allow to process any transfer again

### v6
- mark failed transfers (amount too big) as processed -
  when service is restarted and messages are reprocessed
  then such message might have different answer than
  previous time but other side of transaction may be already reverted

## Running

1. Create `localdev` network
2. Run `start.sh` from `compose` dir
3. Start
  - bank
  - bank-outbox
  - transfers
4. Run `Send`