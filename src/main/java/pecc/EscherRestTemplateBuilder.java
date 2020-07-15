package pecc;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.emarsys.escher.Escher;
import com.emarsys.escher.EscherException;
import com.emarsys.escher.EscherRequest;

public class EscherRestTemplateBuilder {

    public static RestTemplate build(String scope, String key, String secret, boolean enableDebug) {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            @Nullable
            protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
                    @Nullable ResponseExtractor<T> responseExtractor) {
                try {
                    return super.doExecute(url, method, requestCallback, responseExtractor);
                } catch (RestClientResponseException e) {
                    logger.error(MessageFormat.format("Escher request failed, response: {0} {1}\n{2}\n",
                            e.getRawStatusCode(),
                            e.getStatusText(),
                            e.getResponseBodyAsString()));
                    throw e;
                }
            }
        };
        restTemplate.getInterceptors().add(new EscherClientHttpRequestInterceptor(scope, key, secret));
        if (enableDebug) {
            restTemplate.getInterceptors().add(new DebuggingInterceptor());
        }
        return restTemplate;
    }

    public static class EscherClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

        private static final Logger LOGGER = Logger.getLogger(EscherClientHttpRequestInterceptor.class);

        private String scope;
        private String key;
        private String secret;

        public EscherClientHttpRequestInterceptor(String scope, String key, String secret) {
            this.scope = scope;
            this.key = key;
            this.secret = secret;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            Escher escher = new Escher(scope)
                    .setAuthHeaderName("X-Ems-Auth")
                    .setDateHeaderName("X-Ems-Date")
                    .setAlgoPrefix("EMS")
                    .setVendorKey("EMS")
                    .setHashAlgo("SHA256");

            EscherRequest escherRequest = new EscherRequest() {
                @Override
                public String getHttpMethod() {
                    return Optional.ofNullable(request.getMethod()).orElse(HttpMethod.GET).toString();
                }

                @Override
                public URI getURI() {
                    return request.getURI();
                }

                @Override
                public List<Header> getRequestHeaders() {
                    return request.getHeaders().entrySet().stream()
                            .map(e -> new Header(e.getKey(), e.getValue().get(0))).collect(Collectors.toList()); // FIXME only first value ?!
                }

                @Override
                public void addHeader(String fieldName, String fieldValue) {
                    LOGGER.debug(MessageFormat.format("Adding header {0}: {1}", fieldName, fieldValue));

                    request.getHeaders().add(fieldName, fieldValue);
                }

                @Override
                public String getBody() {
                    return new String(body);
                }
            };

            try {
                List<String> signedHeaders = new ArrayList<>();
                signedHeaders.add("X-Ems-Date");
                signedHeaders.add("host");
                if (!escherRequest.getHttpMethod().equals(HttpMethod.GET.toString())) {
                    signedHeaders.add("Content-Type");
                }

                escher.signRequest(escherRequest, key, secret, signedHeaders);
            } catch (EscherException e) {
                throw new IOException("Escher failed to sign request", e);
            }

            return execution.execute(request, body);
        }


    }

    private static class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {

        private static final Logger LOGGER = Logger.getLogger(LoggingRequestInterceptor.class);
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            ClientHttpResponse response = null;

            try {
                response = execution.execute(request, body);
            } finally {
                if (response == null || response.getStatusCode().isError()) {
                    LOGGER.error(MessageFormat.format("Failed request: {0} {1}\n{2}\n{3}\n",
                            request.getMethodValue(),
                            request.getURI(),
                            request.getHeaders().entrySet().stream().collect(StringBuilder::new,
                                    (sb, e) -> sb.append(e.getKey()).append(": ").append(e.getValue().toString()).append("\n"),
                                    StringBuilder::append),
                            new String(body)));
                }
            }

            return response;

        }
    }

    public static class SlimRequestLoggingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger LOGGER = Logger.getLogger(SlimRequestLoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            ClientHttpResponse response = null;

            try {
                response = execution.execute(request, body);
            } finally {
                LOGGER.info(MessageFormat.format("REQUEST: {0} {1}", request.getMethodValue(), request.getURI()));
            }

            return response;
        }
    }

    public static class DebuggingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger LOGGER = Logger.getLogger(DebuggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {

            ClientHttpResponse response = null;

            try {
                response = execution.execute(request, body);
            } finally {
                LOGGER.info(MessageFormat.format("REQUEST: {0} {1}\n{2}\n{3}\n\nRESPONSE: HTTP {4}\n{5}",
                        request.getMethodValue(),
                        request.getURI(),
                        request.getHeaders().entrySet().stream().collect(StringBuilder::new,
                                (sb, e) -> sb.append(e.getKey()).append(": ").append(e.getValue().toString()).append("\n"),
                                StringBuilder::append),
                        new String(body),
                        response == null ? -1 : response.getStatusCode().value(),
                        response == null || response.getHeaders() == null ? ""
                                : response.getHeaders().entrySet().stream().collect(StringBuilder::new, (sb, e)
                                        -> sb.append(e.getKey()).append(": ").append(e.getValue().toString()).append("\n"),
                                StringBuilder::append)
                ));
            }

            return response;
        }
    }
}
