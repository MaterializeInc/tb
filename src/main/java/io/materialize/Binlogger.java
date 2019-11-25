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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.apache.kafka.connect.source.SourceRecord;

public class Binlogger implements Consumer<SourceRecord> {

    public void accept(SourceRecord s) {
        System.out.println(">>>> " + s.toString());
    }

    public static void main(String[] args) {

        ArgumentParser parser = ArgumentParsers.newFor("Binlogger").build().defaultHelp(true)
                .description("Streaming CDC out of database binlogs (currently supported: mysql and postgres");
        parser.addArgument("-t", "--type").choices("mysql", "postgres").setDefault("mysql")
                .help("Specify which database to binlog");
        // parser.addArgument("-o", "--output").help("Destination to stream output to");
        parser.addArgument("-p", "--port").help("Database port");
        parser.addArgument("-s", "--hostname").help("Database hostname");
        parser.addArgument("-d", "--database").help("Database");
        parser.addArgument("-u", "--user").help("User");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        String db = ns.get("database");
        String type = ns.get("type");
        String output = ns.get("output");
        String port = ns.get("port");
        String hostname = ns.get("hostname");
        String user = ns.get("user");

        System.out.println("DEBUG: starting binlogger in mode " + db + " and outputting to " + output);

        if (user == null) {
            System.out.println("user is null, defaulting to postgres");
            user = "postgres";
        }

        if (hostname == null) {
            System.out.println("host is null, defaulting to localhost");
            hostname = "localhost";
        }

        if (port == null) {
            System.out.println("port is null, defaulting to 5432");
            port = "5432";
        }

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
        b = b.with("offset.storage.file.filename", "/dev/null");
        b = b.with("plugin.name", "wal2json");

        Configuration config = b.build();
        System.out.println("config built: " + config.toString());
        Binlogger bl = new Binlogger();

        // Create the engine with this configuration ...
        EmbeddedEngine engine = EmbeddedEngine.create().using(config).notifying(bl::accept).build();

        // Run the engine asynchronously ...
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {

        }

        System.out.println("executor:" + executor.toString());
        System.out.println("running: " + engine.isRunning());
        System.out.println("engine: " + engine.toString());

    }
}
