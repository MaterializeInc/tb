**t**ail **b**inlogs
====================

`tb` tails database binlogs. You point it at a database, and it streams every
write that is made to the database into an [Avro Object Container File][ocf]
per table.

You can tail these files using the [materialized][] `file/ocf` source type.
This lets you fire push-based events when something of interest happens from
your database in a lightweight de-coupled fashion, which would traditionally
require database triggers or something more involved.

[ocf]: https://avro.apache.org/docs/1.8.1/spec.html#Object+Container+Files
[materialized]: https://github.com/MaterializeInc/materialize

## Usage

The easiest way to get started (although note that your database must be
configured to satisfy the [requirements](#database-requirements)) is to
use the docker image with a mounted directory that `materialized` has access
to:

```bash
docker run --rm -v /tmp/tbshare:/tbshare \
    materialize/tb \
        -t postgres
            -p 5432
            -d <database name>
            -H "$POSTGRES_HOST"
            -u "$POSTGRES_USER"
            -P "$POSTGRES_PASSWORD"
            --dir /tbshare/data
            --save-file /tbshare/status
```

If you only want to monitor specific tables you can pass a whitelist flag like this:

```bash
    --whitelist schemaName1.tableName1,schemaName2.tableName2
```

And then, after starting `materialized`, running the appropriate create source
command:

```sql
CREATE SOURCE my_table
FROM AVRO OCF '/tbshare/data/tb.public.my_table'
WITH (tail = true) ENVELOPE DEBEZIUM;
```

After which the standard materialized experience will be available to you:

```sql
SHOW COLUMNS FROM my_table;
```

## Implementation

`tb` is built by [embedding debezium][ed]. Debezium is a distributed
fault-tolerant framework for tailing binlogs into Kafka, using the Kafka
connect framework. `tb` is intended to be a lightweight tool, suitable for
running on your laptop or against a single unreplicated database. The intent is
to enable a prototyping experience that doesn't require 6 containers tethered
together in an intricate `docker-compose` setup, while still being very easily
translatable to existing Kafka infrastructure.

The exact semantics of change data capture
vary based on exactly _which_ database you connect to, and what version of
database you're running. `tb` relies upon streaming replication, which is a
relatively new feature in various databases, so for best results use a recent
version of your favorite database. Streaming replication requires some
configuration, but care has been taken to keep this configuration similar to
best practices for database replication. If your database is up-to-date and
running in a replicated setup, it should be seamless to connect `tb` to it,
with minimal modification.

Currently, `tb` supports Postgres (10+ recommended) and MySQL.

[ed]: https://debezium.io/documentation/reference/0.10/operations/embedded.html

## Database Requirements

### Postgres requirements.

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

Tables should be created with their replica identity set to FULL.

```
psql> CREATE TABLE table (a int, b int);
psql> ALTER TABLE table REPLICA IDENTITY FULL;
```

The `alter-replica-identity.sh` script in the root of this repo will alter every
table in a DB you specify to have `FULL` replica identity.

### MySQL requirements.

You should be running MySQL with row-level binary logging. This means your
`my.cnf` file (OS homebrew default is at `/usr/local/etc/my.cnf`) should have
rows that looks like this:

```
log_bin           = mysql-bin
binlog_format     = row
```

## Build and Run.

To build and run `tb`, do the following:

1. [mvn clean && mvn install](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
2. Run the newly created .jar with `java -jar [path to jar] [args]`
