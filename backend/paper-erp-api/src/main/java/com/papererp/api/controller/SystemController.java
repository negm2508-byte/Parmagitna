package com.papererp.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/network-info")
    public ResponseEntity<Map<String, Object>> getNetworkInfo() {
        Map<String, Object> info = new HashMap<>();
        List<String> lanIps = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        lanIps.add(addr.getHostAddress());
                    }
                }
            }
            info.put("hostname", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }

        info.put("lanIps", lanIps);
        info.put("primaryLanUrl", lanIps.isEmpty() ? "http://localhost:5173" : "http://" + lanIps.get(0) + ":5173");
        info.put("backendLanUrl", lanIps.isEmpty() ? "http://localhost:8080" : "http://" + lanIps.get(0) + ":8080");
        info.put("serverStatus", "ONLINE");
        info.put("serverTime", OffsetDateTime.now());
        info.put("version", "1.0.0-SNAPSHOT");
        info.put("multiTenancyMode", "COMPOSITE_RLS_ZERO_TRUST");
        info.put("cloudflareTunnelCommand", "start_cloudflare_tunnel.bat");

        return ResponseEntity.ok(info);
    }
}
