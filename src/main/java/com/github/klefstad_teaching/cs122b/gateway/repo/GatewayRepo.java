package com.github.klefstad_teaching.cs122b.gateway.repo;

import com.github.klefstad_teaching.cs122b.gateway.repo.entity.GatewayRequestObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GatewayRepo
{
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public GatewayRepo(NamedParameterJdbcTemplate template)
    {
        this.template = template;
    }

    /**
     * TA's Version of insertRequests
     */
//    public Mono<int[]> insertRequests(List<Object> requests)
//    {
//        return Mono.empty();
//    }

    /**
     * Phap's Version of insertRequests
     * To prevent blocking the gateway we should wrap our insert function into a Mono.fromCallable like so:
     * This will allow us to use the subscribe() function and have the database query run on a new thread.
     */
    public Mono<int[]> insertRequests(List<GatewayRequestObject> requests)
    {
        return Mono.fromCallable(() -> insertBatchRequestsToDB(requests));
    }

    private int[] insertBatchRequestsToDB(List<GatewayRequestObject> requests)
    {
        MapSqlParameterSource[] sources = requests
                .stream()
                .map(
                        request ->
                                new MapSqlParameterSource()
                                        .addValue("ipAddress", request.getIpAddress())
                                        .addValue("callTime", request.getCallTime())
                                        .addValue("path", request.getPath())
                )
                .toArray(MapSqlParameterSource[]::new);

        return this.template.batchUpdate(
                "INSERT INTO gateway.request (ip_address, call_time, path) VALUES (:ipAddress, :callTime, :path)",
                sources
        );
    }
}
