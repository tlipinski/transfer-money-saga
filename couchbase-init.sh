USERNAME=Administrator
PASSWORD=password

cat <<INIT | docker exec -i tms-infra-couchbase-1 bash
couchbase-cli cluster-init --services data,index,query --cluster-username $USERNAME --cluster-password $PASSWORD

echo Wait...
sleep 10

couchbase-cli bucket-create --bucket money --bucket-type couchbase --bucket-ramsize 100 --cluster localhost:8091 --username $USERNAME --password $PASSWORD

echo Wait...
sleep 10

couchbase-cli collection-manage --create-collection _default.outbox --cluster localhost:8091 --bucket money --username $USERNAME --password $PASSWORD
cbq -u $USERNAME -p $PASSWORD --script="CREATE PRIMARY INDEX ON money._default.outbox"

couchbase-cli collection-manage --create-collection _default.balances --cluster localhost:8091 --bucket money --username $USERNAME --password $PASSWORD
cbq -u $USERNAME -p $PASSWORD --script="CREATE PRIMARY INDEX ON money._default.balances"

couchbase-cli collection-manage --create-collection _default.sagas --cluster localhost:8091 --bucket money --username $USERNAME --password $PASSWORD
cbq -u $USERNAME -p $PASSWORD --script="CREATE PRIMARY INDEX ON money._default.sagas"

cbq -u $USERNAME -p $PASSWORD --script="CREATE INDEX outbox_unsent ON money._default.outbox (timestamp,_sent)"
INIT

echo Done
