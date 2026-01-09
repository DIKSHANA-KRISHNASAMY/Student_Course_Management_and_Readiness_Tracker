package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple multipart form data parser for file uploads.
 */
public class MultipartParser {
    
    private Map<String, String> fields = new HashMap<>();
    private byte[] fileData = null;
    private String fileName = null;
    private String fileFieldName = null;
    
    public void parse(InputStream inputStream, String contentType) throws IOException {
        // Extract boundary from content type
        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring("boundary=".length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                break;
            }
        }
        
        if (boundary == null) {
            throw new IOException("No boundary found in content type");
        }
        
        byte[] data = inputStream.readAllBytes();
        String fullBoundary = "--" + boundary;
        String endBoundary = "--" + boundary + "--";
        
        String content = new String(data, StandardCharsets.ISO_8859_1);
        String[] parts = content.split(fullBoundary);
        
        for (String part : parts) {
            if (part.trim().isEmpty() || part.trim().equals("--")) continue;
            
            // Find the header/content split
            int headerEnd = part.indexOf("\r\n\r\n");
            if (headerEnd == -1) continue;
            
            String headers = part.substring(0, headerEnd);
            String body = part.substring(headerEnd + 4);
            
            // Remove trailing \r\n
            if (body.endsWith("\r\n")) {
                body = body.substring(0, body.length() - 2);
            }
            if (body.endsWith("--")) {
                body = body.substring(0, body.length() - 2);
                if (body.endsWith("\r\n")) {
                    body = body.substring(0, body.length() - 2);
                }
            }
            
            // Parse Content-Disposition header
            String fieldName = null;
            String filename = null;
            boolean isFile = false;
            
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-disposition:")) {
                    String disposition = line.substring("content-disposition:".length()).trim();
                    for (String param : disposition.split(";")) {
                        param = param.trim();
                        if (param.startsWith("name=")) {
                            fieldName = param.substring(6, param.length() - 1); // Remove quotes
                        } else if (param.startsWith("filename=")) {
                            filename = param.substring(10, param.length() - 1); // Remove quotes
                            isFile = true;
                        }
                    }
                }
            }
            
            if (fieldName != null) {
                if (isFile && filename != null && !filename.isEmpty()) {
                    this.fileFieldName = fieldName;
                    this.fileName = filename;
                    this.fileData = body.getBytes(StandardCharsets.ISO_8859_1);
                } else {
                    fields.put(fieldName, body.trim());
                }
            }
        }
    }
    
    public String getField(String name) {
        return fields.get(name);
    }
    
    public Map<String, String> getFields() {
        return fields;
    }
    
    public boolean hasFile() {
        return fileData != null && fileName != null && !fileName.isEmpty();
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public String getFileFieldName() {
        return fileFieldName;
    }
}
