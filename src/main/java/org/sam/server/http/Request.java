package org.sam.server.http;

import org.sam.server.constant.ContentType;
import org.sam.server.constant.HttpMethod;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by melchor
 * Date: 2020/07/22
 * Time: 5:19 PM
 */
public interface Request {

    static HttpRequest create(InputStream in) {
        return new Request.UrlParser(in).createRequest();
    }

    String getPath();

    HttpMethod getMethod();

    String getParameter(String key);

    Map<String, String> getParameters();

    Set<String> getParameterNames();

    Set<String> getHeaderNames();

    String getHeader(String key);

    Map<String, Object> getAttributes();

    String getJson();

    Set<Cookie> getCookies();

    class UrlParser {
        protected String protocol;
        protected String path;
        protected HttpMethod method;
        protected Map<String, String> headers = new HashMap<>();
        protected Map<String, String> parameters = new HashMap<>();
        protected Map<String, Object> attributes = new HashMap<>();
        protected String json;
        protected Set<Cookie> cookies = new HashSet<>();
        protected Map<String, MultipartFile> files = new HashMap<>();

        public UrlParser(InputStream in) {
            parse(in);
        }

        private void parse(InputStream in) {
            try {
                String headersPart = "";
                int i;
                BufferedInputStream bis = new BufferedInputStream(in);
                StringBuilder sb = new StringBuilder();
                while ((i = bis.read()) != -1) {
                    char c = (char) i;
                    sb.append(c);
                    if (sb.toString().endsWith("\r\n\r\n")) {
                        headersPart = sb.toString().replace("\r\n\r\n", "");
                        break;
                    }
                }

                String[] headers = headersPart.split("\r\n");
                StringTokenizer parse = new StringTokenizer(headers[0]);
                String method = parse.nextToken().toUpperCase();
                String requestPath = parse.nextToken().toLowerCase();
                this.protocol = parse.nextToken().toUpperCase();
                String query = parsePathAndGetQuery(requestPath);

                parseHeaders(headers);
                parseMethod(method);

                if (!query.isEmpty()) {
                    this.parameters = parseQuery(query);
                }

                String contentType = this.headers.get("content-type") != null ? this.headers.get("content-type") : "";
                if (HttpMethod.get(method).equals(HttpMethod.POST) ||
                        HttpMethod.get(method).equals(HttpMethod.PUT) ||
                        ContentType.APPLICATION_JSON.getValue().equals(contentType)) {

                    String requestBody = "";
                    String boundary = null;
                    if (contentType.startsWith(ContentType.MULTIPART_FORM_DATA.getValue())) {
                        boundary = "--" + contentType.split("; ")[1].split("=")[1];
                        parseMultipartBody(bis, boundary);
                    } else {
                        sb = new StringBuilder();
                        while ((i = bis.read()) != -1) {
                            char c = (char) i;
                            sb.append(c);
                        }
                        requestBody = sb.toString();
                        parseRequestBody(requestBody);
                        return;
                    }

                    if (ContentType.APPLICATION_JSON.getValue().equals(contentType) && this.attributes == null) {
                        this.json = requestBody;
                    }
                    this.attributes = parseRequestBody(requestBody);
                }
            } catch (IOException e) {
                System.out.println("terminate thread..");
                e.printStackTrace();
            }
        }

        private void parseHeaders(String[] headers) {
            for (int i = 1; i < headers.length; i++) {
                int index = headers[i].indexOf(": ");
                String key = headers[i].substring(0, index).toLowerCase();
                String value = headers[i].substring(index + 2);
                if ("cookie".equals(key)) {
                    this.cookies = CookieStore.parseCookie(value);
                    continue;
                }
                this.headers.put(key, value);
            }
        }

        private void parseMethod(String method) {
            this.method = HttpMethod.get(method);
        }

        private String parsePathAndGetQuery(String requestPath) {
            int index = requestPath.indexOf("?");
            if (index != -1) {
                this.path = requestPath.substring(0, index);
                return requestPath.substring(index + 1);
            }
            this.path = requestPath;
            return "";
        }

        private Map<String, String> parseQuery(String parameters) {
            Map<String, String> map = new HashMap<>();
            String[] rawParameters = parameters.split("&");
            Arrays.stream(rawParameters).forEach(parameter -> {
                String[] parameterPair = parameter.split("=");
                String name = parameterPair[0];
                String value = null;
                if (parameterPair.length == 2) {
                    value = parameterPair[1];
                }
                map.put(name, value);
            });
            return map;
        }

        private Map<String, Object> parseRequestBody(String requestBody) {
            if (requestBody.startsWith("{") && requestBody.endsWith("}")) return null;
            Map<String, Object> map = new HashMap<>();
            String[] rawParameters = requestBody.split("&");
            Arrays.stream(rawParameters).forEach(parameter -> {
                String[] parameterPair = parameter.split("=");
                String name = parameterPair[0];
                String value = null;
                if (parameterPair.length == 2) {
                    value = parameterPair[1];
                }
                map.put(name, value);
            });
            return map;
        }

        private void parseMultipartBody(BufferedInputStream in, String boundary) throws IOException {
            int i;
            StringBuilder sb = new StringBuilder();
            while ((i = in.read()) != -1) {
                char c = (char) i;
                sb.append(c);
                if (sb.toString().contains(boundary + "\r\n")) {
                    lineParser(in, boundary);
                    sb.setLength(0);
                }
            }
        }

        private void lineParser(BufferedInputStream in, String boundary) throws IOException {
            StringBuilder sb = new StringBuilder();
            int i;
            int j = 0;
            int loopCnt = 0;
            String name = null;
            String value;
            String filename = null;
            String contentType = null;
            boolean isFile = false;
            int available = in.available();
            byte[] data = new byte[available];
            while ((i = in.read()) != -1) {
                data[j] = (byte) i;
                if (data[j] == '\n') {
                    data = Arrays.copyOfRange(data, 0, j);
                    String line = new String(data, StandardCharsets.UTF_8);
                    data = new byte[available];
                    j = 0;
                    if (loopCnt == 0) {
                        String[] split = line.split("\"");
                        name = split[1];
                        loopCnt++;
                        if (split.length == 5) {
                            filename = split[3];
                            isFile = true;
                        }
                        continue;
                    } else if (loopCnt == 1 && isFile) {
                        int index = line.indexOf(": ");
                        contentType = line.substring(index + 2);
                        byte[] fileData = parseFile(in, boundary);
                        FileOutputStream fos = new FileOutputStream("/Users/melchor/" + filename);
                        fos.write(fileData);
                        loopCnt = 0;
                        continue;
                    } else if (loopCnt == 1) {
                        value = line;
                    }

                    if (line.contains(boundary)) {
                        loopCnt = 0;
                    }
                    if (line.contains(boundary + "--")) return;

                }
                j++;
            }
        }

        private byte[] parseFile(BufferedInputStream in, String boundary) throws IOException {
            in.read();
            in.read();
            byte[] data = new byte[1024 * 8];
            int i;
            int fileLength = 0;
            while ((i = in.read()) != -1) {
                if (data.length == fileLength) {
                    byte[] temp = new byte[data.length * 2];
                    System.arraycopy(data, 0, temp, 0, data.length);
                    data = temp;
                }
                data[fileLength] = (byte) i;
                if (data[fileLength] == '\n') {
                    String content = new String(data, StandardCharsets.UTF_8);
                    String boundayByte = new String(boundary.getBytes(), StandardCharsets.UTF_8);
                    int index = content.indexOf(boundayByte);
                    if (index != -1) break;
                }
                fileLength++;
            }
            data = Arrays.copyOfRange(data, 0, fileLength - boundary.getBytes().length - 5);
            return data;
        }

        public HttpRequest createRequest() {
            Map<String, String> headers = this.headers;
            HttpMethod method = this.method;
            String path = this.path;
            Map<String, String> parameters = this.parameters;
            Map<String, Object> attributes = this.attributes;
            String json = this.json;
            Set<Cookie> cookies = this.cookies;

            String contentType = headers.get("content-type") != null ? headers.get("content-type") : "";
            if (contentType.startsWith(ContentType.MULTIPART_FORM_DATA.getValue())) {
                return new HttpMultipartRequest(path, method, headers, parameters, attributes, json, cookies, files);
            }
            return new HttpRequest(path, method, headers, parameters, attributes, json, cookies);
        }
    }
}
