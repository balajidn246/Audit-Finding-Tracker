package com.internship.tool.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class ClamAvScanner {
    @Value("${security.clamav.host:localhost}") private String host;
    @Value("${security.clamav.port:3310}") private int port;
    public void requireClean(byte[] content) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000); socket.setSoTimeout(120000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(new byte[]{'z','I','N','S','T','R','E','A','M',0});
            for (int offset = 0; offset < content.length; ) {
                int length = Math.min(64 * 1024, content.length - offset); out.writeInt(length); out.write(content, offset, length); offset += length;
            }
            out.writeInt(0); out.flush();
            ByteArrayOutputStream response = new ByteArrayOutputStream(); int next;
            while ((next = socket.getInputStream().read()) != -1 && next != 0) response.write(next);
            String result = response.toString(java.nio.charset.StandardCharsets.UTF_8);
            if (!result.endsWith("OK")) throw new IllegalArgumentException("The evidence file failed malware scanning");
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) { throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Evidence scanning is temporarily unavailable", ex); }
    }
}
