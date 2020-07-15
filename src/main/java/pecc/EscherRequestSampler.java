package pecc;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class EscherRequestSampler extends AbstractJavaSamplerClient {

    static final Logger log = LoggerFactory.getLogger(EscherRequestSampler.class);
    static final String SCOPE = "scope";
    static final String KEY = "key";
    static final String SECRET = "secret";
    static final String URL = "url";
    static final String HTTP_METHOD = "method";
    static final String REQUEST_BODY = "body";
    static final String CONTENT_TYPE = "Content-Type";
    static final String DEBUGLOGGING = "debuglogging";

    private RestTemplate restTemplate;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        restTemplate = EscherRestTemplateBuilder.build(
                context.getParameter(SCOPE),
                context.getParameter(KEY),
                context.getParameter(SECRET),
                Boolean.parseBoolean(context.getParameter(DEBUGLOGGING)));
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(SCOPE,"");
        defaultParameters.addArgument(KEY,"");
        defaultParameters.addArgument(SECRET,"");
        defaultParameters.addArgument(URL,"");
        defaultParameters.addArgument(HTTP_METHOD,"POST");
        defaultParameters.addArgument(REQUEST_BODY,"");
        defaultParameters.addArgument(CONTENT_TYPE,"application/json");
        defaultParameters.addArgument(DEBUGLOGGING,"false");
        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();
        try {

            HttpEntity<String> body = createBody(context);

            ResponseEntity<String> response = restTemplate
                    .exchange(
                            context.getParameter(URL),
                            HttpMethod.resolve(context.getParameter(HTTP_METHOD)),
                            body,
                            String.class);

            sampleResult.setResponseCode(Integer.toString(response.getStatusCodeValue()));
            sampleResult.setResponseMessage(response.getBody());
            sampleResult.setResponseData(response.getBody(), "UTF-8");
            sampleResult.setSuccessful(true);

        } catch (HttpStatusCodeException ex) {
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode(Integer.toString(ex.getRawStatusCode()));
            sampleResult.setResponseMessage(ex.getResponseBodyAsString());
            sampleResult.setResponseData(ex.getResponseBodyAsString(), "UTF-8");
        } catch (Exception e) {
            log.error("Error running EscherRequestSampler", e);
            sampleResult.setSuccessful(false);
            sampleResult.setResponseCode("-1");
            sampleResult.setResponseMessage(e.getMessage());
            sampleResult.setResponseData(e.getMessage(), "UTF-8");
        } finally {
            sampleResult.sampleEnd();
        }
        return sampleResult;
    }

    private HttpEntity<String> createBody(JavaSamplerContext context) {
        String requestBody = context.getParameter(REQUEST_BODY);

        if (isBlank(requestBody)) {
            return (HttpEntity<String>) HttpEntity.EMPTY;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_TYPE, context.getParameter(CONTENT_TYPE));
        return new HttpEntity<>(requestBody, headers);
    }

    private static boolean isBlank(String str) {
        return (str == null || str.length() == 0);
    }
}
