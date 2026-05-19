package pt.premodern.eventmanager.persistence;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class JsonStorageService {
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(UUID.class, new UuidAdapter())
            .create();

    public void saveEvent(Event event, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(event, writer);
        }
    }

    public Event loadEvent(File file) throws IOException {
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Event event = gson.fromJson(reader, Event.class);
            relinkMatchesToPlayers(event);
            return event;
        }
    }

    private void relinkMatchesToPlayers(Event event) {
        if (event == null) {
            return;
        }
        Map<UUID, Player> playersById = event.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        for (Round round : event.getRounds()) {
            for (Match match : round.getMatches()) {
                if (match.getPlayer1() != null) {
                    match.setPlayer1(playersById.getOrDefault(match.getPlayer1().getId(), match.getPlayer1()));
                }
                if (match.getPlayer2() != null) {
                    match.setPlayer2(playersById.getOrDefault(match.getPlayer2().getId(), match.getPlayer2()));
                }
                if (match.getWinner() != null) {
                    match.setWinner(playersById.getOrDefault(match.getWinner().getId(), match.getWinner()));
                }
                if (match.getId() == null) {
                    match.setId(UUID.randomUUID());
                }
            }
        }
        event.getPlayers().forEach(player -> {
            if (player.getId() == null) {
                player.setId(UUID.randomUUID());
            }
            player.getOpponentsIds().removeIf(Objects::isNull);
        });
    }

    private static class UuidAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }
    }
}
