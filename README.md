`tb` `tails` database `binlogs`. You point it at a database, and it streams
every write that is made to the database into a file. You can tail this file
and process it with any unix tool such as `jq` or `awk`, or something more
full-fledged. This lets you fire push-based events when something of interest
happens from your database in a lightweight de-coupled fashion, which would traditionally require database
triggers or something more involved.

`tb` is built by
[embedding debezium](https://debezium.io/documentation/reference/0.10/operations/embedded.html).
Debezium is a distributed fault-tolerant framework for tailing binlogs into
Kafka, using the Kafka connect framework. `tb` is intended to be a lightweight
tool, suitable for running on your laptop or against a single
unreplicated database. The intent is to enable a prototyping experience that
doesn't require 6 containers tethered together in an intricate `docker-compose`
setup, while still being very easily translated to existing Kafka 
infrastructure.

The exact semantics of change data capture
vary based on exactly _which_ database you connect to, and what version of
database you're running. `tb` relies upon streaming replication, which is a
relatively new feature in various databases, so for best results use a recent
version of your favorite database. Streaming replication requires some 
configuration, but care has been taken to keep this configuration similar to
best practices for database replication. If your database is up-to-date and
running in a replicated setup, it should be seamless to connect `tb` to it,
with minimal modification.

Currently, `tb` supports Postgres, with work in progress to support MySQL.



## Postgres requirements.

You should be running Postgres 10+. Support for Postgres <=9 is possible, but
requires installing a plug-in such as `decoderbufs` or `wal2json` in the 
database. Postgres 10 onwards includes built-in streaming replication 
capabilities via `pgoutput`, which is what `tb` uses out-of-the-box.

You will have to set the Write-Ahead-Logging (WAL) level to logical. This requires a database
restart. Here are example instructions for OS X, assuming you run postgres
via `brew services`:

```
psql> ALTER SYSTEM set wal_level to logical;
bash> brew services restart postgres
```


## MySQL requirements.

You should be running MySQL with row-level binary logging. This means your 
`my.cnf` file (OS homebrew default is at `/usr/local/etc/my.cnf`) should have
rows that looks like this:

```
log_bin           = mysql-bin
binlog_format     = row
```
