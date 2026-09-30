package com.example.modbus_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;

/** File mailbox for the external updater. This application never executes deployment commands. */
@Service
public class PanelUpdateService {
    private final Path directory;
    private final ObjectMapper mapper;

    public PanelUpdateService(@Value("${panel.updates.directory:${user.home}/.local/state/sauna-updater-small}") String directory,
                              ObjectMapper mapper) {
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    private JsonNode read(String name) throws IOException {
        Path path = directory.resolve(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 32768) {
            return mapper.createObjectNode();
        }
        JsonNode value = mapper.readTree(Files.readAllBytes(path));
        return value != null && value.isObject() ? value : mapper.createObjectNode();
    }

    private boolean enabled() {
        return !Files.isSymbolicLink(directory) && Files.isRegularFile(directory.resolve("ui-enabled"), LinkOption.NOFOLLOW_LINKS);
    }

    public synchronized ObjectNode status() throws IOException {
        ObjectNode result = mapper.createObjectNode();
        result.put("enabled", enabled());
        if (!enabled()) return result;
        JsonNode state = read("status.json");
        String status = state.path("status").asText("waiting");
        String tag = state.path("tag").asText("");
        long checkedAt = state.path("checked_at").asLong(0);
        boolean fresh = checkedAt > Instant.now().getEpochSecond() - 180;
        JsonNode request = read("request.json");
        if ("available".equals(status) && tag.equals(request.path("tag").asText()) && !tag.isEmpty()) status = "queued";
        result.put("status", status);
        result.put("version", tag);
        result.put("commit", state.path("commit").asText(""));
        result.put("ready", fresh);
        result.put("checkedAt", checkedAt);
        return result;
    }

    public synchronized ObjectNode request(String version) throws IOException {
        if (version == null || !version.matches("small-production-[1-9][0-9]*-[1-9][0-9]*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректная версия");
        }
        ObjectNode current = status();
        if (!current.path("enabled").asBoolean() || !current.path("ready").asBoolean()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Загрузчик обновлений недоступен");
        }
        if (!version.equals(current.path("version").asText()) ||
                !("available".equals(current.path("status").asText()) || "queued".equals(current.path("status").asText()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Версия уже изменилась. Обновите сведения");
        }
        Path temporary = Files.createTempFile(directory, ".request-", ".json",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            ObjectNode request = mapper.createObjectNode().put("tag", version);
            Files.write(temporary, mapper.writeValueAsBytes(request), StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temporary, directory.resolve("request.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return status();
    }
}
