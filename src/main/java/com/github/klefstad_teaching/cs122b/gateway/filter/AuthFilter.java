package com.github.klefstad_teaching.cs122b.gateway.filter;

import com.github.klefstad_teaching.cs122b.core.result.IDMResults;
import com.github.klefstad_teaching.cs122b.core.result.Result;
import com.github.klefstad_teaching.cs122b.core.result.ResultMap;
import com.github.klefstad_teaching.cs122b.core.security.JWTAuthenticationFilter;
import com.github.klefstad_teaching.cs122b.gateway.config.GatewayServiceConfig;
import com.github.klefstad_teaching.cs122b.gateway.repo.entity.AuthResponse;
import com.github.klefstad_teaching.cs122b.gateway.repo.entity.AuthenticateRequest;
import com.github.klefstad_teaching.cs122b.gateway.repo.entity.MyCustomResultPOJO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuthFilter implements GatewayFilter
{
    private static final Logger LOG = LoggerFactory.getLogger(AuthFilter.class);

    private final GatewayServiceConfig config;
    private final WebClient            webClient;

    @Autowired
    public AuthFilter(GatewayServiceConfig config)
    {
        this.config = config;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        Optional<String> accessTokenFromHeader = getAccessTokenFromHeader(exchange);
        if (!accessTokenFromHeader.isPresent()) {
            return setToFail(exchange);
        }
        String accessToken = accessTokenFromHeader.get();

        // make a request to our IDM's /authenticate endpoint in order to validate our user.

        // Define the body of the request to use with BodyInserters below
        AuthenticateRequest authenticateRequestBody = new AuthenticateRequest()
                .setAccessToken(accessToken);

        // Using flat map you can take the response of the IDM
        // and then depending on the response you can choose to return either
        // a Mono that says to continue or a Mono that says to end

        // below here in this filter function, nothing will be executed until we call filter.block() or filter.subscribe()
        // somewhere else
        // everything shown below is just me telling
        // that this is what WILL be executed whenever you call filter.block() or filter.subscribe() (2 ways to start the chain)
        // I want you to take the client
        // make a post request
        // transform the response into a POJO
        // get the status code using that POJO and compare it
        // depending on the comparison, continue with a chain or end the chain
        return this.webClient
                .post() //  specify an HTTP method of a request
                .uri(config.getIdm() + "/authenticate") // Define the URL
                .bodyValue(authenticateRequestBody) // This maps the POJO to JSON
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .map(authResponse -> ResultMap.fromCode(authResponse.getResult().getCode()))
                .map(result -> result.code() == IDMResults.ACCESS_TOKEN_IS_VALID.code())
                .flatMap(accessTokenIsValid -> accessTokenIsValid ?
                        chain.filter(exchange) :    // This returns a Mono that says to continue
                        setToFail(exchange)         // This returns a Mono that says to end
                )
                .log()
                ;
    }

    private Mono<Void> setToFail(ServerWebExchange exchange)
    {
        // This returns a Mono that says to end
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//        return exchange.getResponse().setComplete();
        return Mono.empty();
    }

    /**
     * Takes in a accessToken token and creates Mono chain that calls the idm and maps the value to
     * a Result
     *
     * @param accessToken a encodedJWT
     * @return a Mono that returns a Result
     */
    private Mono<Result> authenticate(String accessToken)
    {
        // Did this inside the "filter" function above for simplicity
        return Mono.empty();
    }

    private Optional<String> getAccessTokenFromHeader(ServerWebExchange exchange)
    {
        List<String> values = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (values  == null || values.size() != 1) {
            return Optional.empty();
        }

        String accessTokenWithBearer = values.get(0);

        if (accessTokenWithBearer.startsWith(JWTAuthenticationFilter.BEARER_PREFIX)) {
            return Optional.of(
                    accessTokenWithBearer.substring(JWTAuthenticationFilter.BEARER_PREFIX.length())
            );
        }

        return Optional.empty();
    }
}
