docker-compose -p couchbase -f couchbase.yml down
docker-compose -p couchbase -f couchbase.yml up -d
docker-compose -p kafka -f kafka.yml down
docker-compose -p kafka -f kafka.yml up -d
