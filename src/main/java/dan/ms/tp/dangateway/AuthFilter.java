package dan.ms.tp.dangateway;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config>{
    
    public static class Config {
    }

    private static Predicate<ServerHttpRequest> isPublic = r -> List.of("/api/auth").stream().anyMatch(uri -> r.getURI().getPath().contains(uri));
    @Autowired 
    Environment env;

    public AuthFilter() {super(Config.class);}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            
            // continues
            if(isPublic.test(exchange.getRequest()))
                return chain.filter(exchange);

            if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
                
            
            String header = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if(header != null && header.startsWith("Bearer")) {
                header = header.substring(7);
            }
            try {
                new RestTemplate().getForObject(env.getProperty("env.auth.url")+"validate?token="+header, String.class);
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };  
    }

}
