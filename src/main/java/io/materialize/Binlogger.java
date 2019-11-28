package io.materialize;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import io.debezium.config.Configuration;
import io.debezium.config.Configuration.Builder;
import io.debezium.embedded.EmbeddedEngine;
import java.util.concurrent.Executors;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.apache.kafka.connect.source.SourceRecord;

public class Binlogger implements Consumer<SourceRecord> {
    BufferedWriter bw;

    public Binlogger(String s) {
        try {
            FileWriter f = new FileWriter(s);
            bw = new BufferedWriter(f);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    public void accept(SourceRecord s) {
        try {
            bw.write(s.toString());
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void main(String[] args) {

        ArgumentParser parser = ArgumentParsers.newFor("Binlogger").build().defaultHelp(true)
                .description("Streaming CDC out of database binlogs (currently supported: mysql and postgres");
        parser.addArgument("-t", "--type").choices("mysql", "postgres").setDefault("postgres")
                .help("Specify which database to binlog");
        // parser.addArgument("-o", "--output").help("Destination to stream output to");
        parser.addArgument("-p", "--port").help("Database port").setDefault("5432");
        parser.addArgument("-s", "--hostname").help("Database hostname").setDefault("localhost");
        parser.addArgument("-d", "--database").help("Database").setDefault("postgres");
        parser.addArgument("-u", "--user").help("User").setDefault("postgres");

        parser.addArgument("-f", "--file").help("file").setDefault("cdc.json");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String db = ns.get("database");
        String type = ns.get("type");
        String port = ns.get("port");
        String hostname = ns.get("hostname");
        String user = ns.get("user");
        String file = ns.get("file");

        Builder b = Configuration.create();

        if (type.equals("mysql")) {
            b = b.with("connector.class", "io.debezium.connector.mysql.MySqlConnector").with("name",
                    "my-sql-connector");
        } else if (type.equals("postgres")) {
            b = b.with("connector.class", "io.debezium.connector.postgresql.PostgresConnector").with("name",
                    "postgres-connector");
        }

        b = b.with("database.hostname", hostname);
        b = b.with("database.port", port);
        b = b.with("database.user", user);
        b = b.with("database.dbname", db);
        b = b.with("database.server.name", "binlogger");
        b = b.with("offset.storage.file.filename", "/dev/null"); // TODO: support resuming(#1).
        // Need a distinct pg_replication_slots name, "debezium" is already taken via
        // standard Materialize setup.
        b = b.with("slot.name", "tb_debezium");
        b = b.with("plugin.name", "pgoutput");

        Configuration config = b.build();
        Binlogger bl = new Binlogger(file);

        // Create the engine with this configuration ...
        EmbeddedEngine engine = EmbeddedEngine.create().using(config).notifying(bl::accept).build();
        engine.run();
    }
}
