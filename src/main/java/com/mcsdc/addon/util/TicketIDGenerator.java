package com.mcsdc.addon.util;

import org.jetbrains.annotations.Nullable;

public class TicketIDGenerator {

    private static final int DEFAULT_PORT = 25565;

    private record ParsedIp(long value, int port) {}

    public static String generateTicketID(String ipAndPort) {
        ParsedIp parsed = parse(ipAndPort, false);
        if (parsed == null) return "";
        long combined = (parsed.value() << 16) | (parsed.port() & 0xFFFFL);
        return Long.toString(combined, 36).toUpperCase();
    }

    public static String decodeTicketID(String ticketID) {
        long combined = Long.parseLong(ticketID, 36);

        int port = (int) (combined & 0xFFFF);
        long ipValue = combined >> 16;

        StringBuilder ip = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            long byteVal = (ipValue >> (8 * i)) & 0xFF;
            ip.append(byteVal);
            if (i > 0) ip.append('.');
        }

        return ip + ":" + port;
    }

    public static boolean isValidIPv4WithPort(String ipAndPort) {
        return parse(ipAndPort, true) != null;
    }

    @Nullable
    private static ParsedIp parse(String ipAndPort, boolean requirePort) {
        if (ipAndPort == null || ipAndPort.isEmpty()) return null;

        String[] parts = ipAndPort.split(":");
        if (requirePort ? parts.length != 2 : parts.length > 2) return null;

        String ip;
        int port = DEFAULT_PORT;
        if (parts.length == 2) {
            ip = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (port < 0 || port > 65535) return null;
        } else if (parts.length == 1) {
            ip = parts[0];
        } else {
            return null;
        }

        String[] ipParts = ip.split("\\.");
        if (ipParts.length != 4) return null;

        long ipValue = 0;
        for (String segment : ipParts) {
            try {
                int byteVal = Integer.parseInt(segment);
                if (byteVal < 0 || byteVal > 255) return null;
                ipValue = (ipValue << 8) | byteVal;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return new ParsedIp(ipValue, port);
    }

    static void selfCheck() {
        String sample = "192.168.1.1:25565";
        String ticket = generateTicketID(sample);
        assert !ticket.isEmpty() : "encode failed";
        assert sample.equals(decodeTicketID(ticket)) : "round-trip failed";
        assert isValidIPv4WithPort(sample);
        assert !isValidIPv4WithPort("192.168.1.1");
        assert generateTicketID("not-an-ip").isEmpty();
    }

    public static void main(String[] args) {
        selfCheck();
    }
}
