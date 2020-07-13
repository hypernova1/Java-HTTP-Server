package org.sam.server;

import org.sam.server.constant.HttpStatus;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Response {

    private static final String DEFAULT_FILE = "static/index.html";
    private static final String FILE_NOT_FOUND = "static/404.html";
    private static final String METHOD_NOT_SUPPORTED = "static/not_supported";

    private ClassLoader classLoader = getClass().getClassLoader();

    private PrintWriter out;
    private BufferedOutputStream dataOut;
    private String path;
    private Map<String, Object> headers = new HashMap<>();

    private HttpStatus httpStatus;

    public Response(PrintWriter out, BufferedOutputStream dataOut, String path) {
        this.out = out;
        this.dataOut = dataOut;
        this.path = path;
    }

    public static Response create(PrintWriter out, BufferedOutputStream dataOut, String path) {
        return new Response(out, dataOut, path);
    }

    public void execute() throws IOException {
        if (this.path.endsWith("/")) {
            path = DEFAULT_FILE;
        }

        getFile(path, HttpStatus.OK);
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        byte[] fileData = new byte[fileLength];
        FileInputStream fis = new FileInputStream(file);
        fis.read(fileData);
        return fileData;
    }

    private void printHeader() {
        out.println("HTTP/1.1 " + httpStatus.getCode() + " " + httpStatus.getMessage());
        headers.keySet().forEach(key -> out.println(key + ": " + headers.get(key)));
    }

    private void getFile(String filePath, HttpStatus status) throws IOException {
        URL fileUrl = classLoader.getResource(filePath);
        if (fileUrl == null) {
            fileNotFound();
            return;
        }

        File file = new File(fileUrl.getFile());
        int fileLength = (int) file.length();
        byte[] fileData = readFileData(file, fileLength);
        httpStatus = status;

        headers.put("Server", "Java HTTP Server from sam : 1.0");
        headers.put("Date", LocalDateTime.now());
        headers.put("Content-Type", getContentMimeType());
        headers.put("Content-length", fileLength);

        printHeader();

        dataOut.write(fileData, 0, fileLength);
        out.println();
        out.flush();
        dataOut.flush();
    }

    public void fileNotFound() throws IOException {
        if (HttpServer.verbose) {
            System.out.println("File " + path + " not found");
        }

        getFile(FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public void methodNotImplemented() throws IOException {
        if (!HttpServer.verbose) {
            System.out.println("501 not implemented :" + path + "method");
        }

        getFile(METHOD_NOT_SUPPORTED, HttpStatus.NOT_IMPLEMENTED);
    }

    public String getContentMimeType() {
        if (httpStatus.equals(HttpStatus.NOT_FOUND) || httpStatus.equals(HttpStatus.NOT_IMPLEMENTED)) return "text/html";
        if (this.path.endsWith(".html")) return "text/html";
        return "text/plain";
    }

    private void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public Object getHeader(String key) {
        return headers.get(key);
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

}
