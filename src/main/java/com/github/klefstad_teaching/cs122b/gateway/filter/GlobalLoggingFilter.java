package com.github.klefstad_teaching.cs122b.gateway.filter;

import com.github.klefstad_teaching.cs122b.gateway.config.GatewayServiceConfig;
import com.github.klefstad_teaching.cs122b.gateway.repo.GatewayRepo;
import com.github.klefstad_teaching.cs122b.gateway.repo.entity.GatewayRequestObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered
{
    private static final Logger    LOG          = LoggerFactory.getLogger(GlobalLoggingFilter.class);
    private static final Scheduler DB_SCHEDULER = Schedulers.boundedElastic();

    private final GatewayRepo          gatewayRepo;
    private final GatewayServiceConfig config;

    // Queue to collect db objects that we will to insert into db as a batch
    // When the number of objects in the queue reaches a certain threshold (config.getMaxLogs()), we will
    // take all the objects in this queue put it into a db batch update.
    private final LinkedBlockingQueue<GatewayRequestObject> requests = new LinkedBlockingQueue<>();

    @Autowired
    public GlobalLoggingFilter(GatewayRepo gatewayRepo, GatewayServiceConfig config)
    {
        this.gatewayRepo = gatewayRepo;
        this.config = config;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();

        requests.add(new GatewayRequestObject()
                .setIpAddress(String.valueOf(request.getRemoteAddress().getAddress().getHostAddress()))
                .setCallTime(Timestamp.from(Instant.now()))
                .setPath(String.valueOf(request.getPath())));

        // When the number of objects in the queue exceeds (bc requests from multiple threads are added to the queue)
        // a certain threshold (config.getMaxLogs()), we will
        // take all the objects in this queue put it into a db batch update.
        if (requests.size() >= config.getMaxLogs()) {
            drainRequests();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder()
    {
        return -1;
    }

    public void drainRequests()
    {
        List<GatewayRequestObject> drainRequest = new ArrayList<>();

        // Drain all the requests into the newly created list
        // making the queue empty for new incoming requests to be stored
        requests.drainTo(drainRequest);

        // take the drainRequest and insert it into db as a db batch update
        // use a Mono call repo.insertBatchRequestsToDB for the job of inserting batch into db
        gatewayRepo.insertRequests(drainRequest)
                .subscribeOn(DB_SCHEDULER) // This just says "where" to subscribe on (DB_SCHEDULERd in this case)
                .subscribe();
    }
}
