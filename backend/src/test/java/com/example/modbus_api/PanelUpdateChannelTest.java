package com.example.modbus_api;
import com.example.modbus_api.service.PanelUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;

class PanelUpdateChannelTest {
    @TempDir Path root;
    @Test void labAcceptsOnlyLabCandidate() throws Exception {
        Files.write(root.resolve("ui-enabled"),new byte[0]);
        Files.write(root.resolve("status.json"),("{\"status\":\"available\",\"tag\":\"small-lab-17-1\",\"checked_at\":"+Instant.now().getEpochSecond()+"}").getBytes("UTF-8"));
        PanelUpdateService lab=new PanelUpdateService(root.toString(),new ObjectMapper(),"lab");
        assertEquals("queued",lab.request("small-lab-17-1").path("status").asText());
        assertThrows(ResponseStatusException.class,()->lab.request("small-production-17-1"));
    }
    @Test void unknownChannelFailsClosed() {
        assertThrows(IllegalArgumentException.class,()->new PanelUpdateService(root.toString(),new ObjectMapper(),"anything"));
    }
}
