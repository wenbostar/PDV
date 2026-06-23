package PDVGUI.gui.utils;

import PDVGUI.gui.PDVMainClass;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.concurrent.Executors;

/**
 * Optional loopback HTTP control server that lets an external launcher (e.g. CasanovoGUI) drive a
 * running PDV de novo window. It is created only when PDV is launched with
 * {@code denovo-gui ... --port <p>}; when that flag is absent the server never starts, so default
 * PDV behaviour is unchanged.
 *
 * <p>It binds to {@code 127.0.0.1} only and exposes:</p>
 * <ul>
 *   <li>{@code GET /ready} &rarr; {@code 200} once the result table is fully loaded, else {@code 503}.</li>
 *   <li>{@code GET|POST /select?ref=<spectra_ref>} &rarr; {@code 200} selected, {@code 404} unknown
 *       ref, {@code 503} not ready, {@code 400} missing ref. {@code ref} is the verbatim mzTab
 *       {@code spectra_ref} (URL-encoded), e.g.
 *       {@code ms_run[1]:controllerType=0 controllerNumber=1 scan=1882}.</li>
 *   <li>{@code GET /shutdown} &rarr; {@code 200}; stops the server (not the GUI).</li>
 * </ul>
 */
public class PdvControlServer {

    private final PDVMainClass pdv;
    private final HttpServer server;

    /**
     * Create the server bound to the loopback address on {@code port}. Call {@link #start()} to begin
     * serving.
     *
     * @param pdv  the window to drive
     * @param port loopback TCP port
     * @throws IOException if the port cannot be bound
     */
    public PdvControlServer(PDVMainClass pdv, int port) throws IOException {
        this.pdv = pdv;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        this.server.createContext("/ready", this::handleReady);
        this.server.createContext("/select", this::handleSelect);
        this.server.createContext("/shutdown", this::handleShutdown);
        this.server.setExecutor(Executors.newFixedThreadPool(2));
    }

    /** Begin serving requests. */
    public void start() {
        server.start();
    }

    /** Stop serving requests immediately. Safe to call more than once. */
    public void stop() {
        server.stop(0);
    }

    private void handleReady(HttpExchange exchange) throws IOException {
        try {
            boolean ready = pdv.isReadyForSelect();
            respond(exchange, ready ? 200 : 503, ready ? "ready" : "loading");
        } catch (RuntimeException e) {
            // Never let an unexpected error escape: that would close the socket with no status,
            // leaving the client hanging until its read times out.
            respond(exchange, 500, "error: " + e.getMessage());
        }
    }

    private void handleSelect(HttpExchange exchange) throws IOException {
        try {
            String ref = queryParam(exchange.getRequestURI().getRawQuery(), "ref");
            if (ref == null || ref.isEmpty()) {
                respond(exchange, 400, "missing ref");
                return;
            }
            int status = pdv.selectBySpectraRef(ref);
            if (status == PDVMainClass.SELECT_OK) {
                respond(exchange, 200, "selected");
            } else if (status == PDVMainClass.SELECT_NOT_FOUND) {
                respond(exchange, 404, "unknown ref");
            } else {
                respond(exchange, 503, "not ready");
            }
        } catch (RuntimeException e) {
            // Same guard: turn any unexpected failure into a 500 rather than a dropped connection.
            respond(exchange, 500, "error: " + e.getMessage());
        }
    }

    private void handleShutdown(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "bye");
        stop();
    }

    /** Extract a single parameter from a raw (still percent-encoded) query string, URL-decoded. */
    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                try {
                    return URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return pair.substring(eq + 1);
                }
            }
        }
        return null;
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        try {
            os.write(bytes);
        } finally {
            os.close();
        }
    }
}
