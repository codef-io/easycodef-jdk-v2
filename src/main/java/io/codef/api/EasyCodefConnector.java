package io.codef.api;

import com.alibaba.fastjson2.JSON;
import io.codef.api.constants.CodefHost;
import io.codef.api.constants.CodefPath;
import io.codef.api.dto.EasyCodefRequest;
import io.codef.api.dto.EasyCodefResponse;
import io.codef.api.error.CodefError;
import io.codef.api.error.CodefException;
import io.codef.api.util.HttpClientUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;

import static io.codef.api.dto.EasyCodefRequest.BASIC_TOKEN_FORMAT;
import static io.codef.api.dto.EasyCodefRequest.BEARER_TOKEN_FORMAT;
import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;

public final class EasyCodefConnector {
    private static final ResponseHandler responseHandler = new ResponseHandler();

    private EasyCodefConnector() {
    }

    public static EasyCodefResponse requestProduct(
            EasyCodefRequest request,
            EasyCodefToken token,
            String requestUrl
    ) throws CodefException {
        HttpPost httpRequest = createProductRequest(request, token, requestUrl);
        return executeRequest(httpRequest, responseHandler::handleProductResponse);
    }

    private static HttpPost createTokenRequest(String codefOAuthToken) {
        HttpPost httpPost = new HttpPost(CodefHost.CODEF_OAUTH_SERVER + CodefPath.ISSUE_TOKEN);
        httpPost.addHeader(AUTHORIZATION, String.format(BASIC_TOKEN_FORMAT, codefOAuthToken));
        return httpPost;
    }
    
    public static String requestToken(
            String codefOAuthToken
    ) throws CodefException {
        HttpPost request = createTokenRequest(codefOAuthToken);
        return executeRequest(request, responseHandler::handleTokenResponse);
    }

    private static HttpPost createProductRequest(
            EasyCodefRequest request,
            EasyCodefToken token,
            String requestUrl
    ) {
        HttpPost httpPost = new HttpPost(requestUrl);
        httpPost.addHeader(AUTHORIZATION, String.format(BEARER_TOKEN_FORMAT, token.getAccessToken()));

        String rawRequest = JSON.toJSONString(request.requestBody());
        httpPost.setEntity(new StringEntity(rawRequest, StandardCharsets.UTF_8));

        return httpPost;
    }

    private static <T> T executeRequest(
            HttpPost request,
            ResponseProcessor<T> processor
    ) {
        try (var httpClient = HttpClientUtil.createClient()) {
            return httpClient.execute(request, processor::process);
        } catch (CodefException e) {
            throw e;
        } catch (Exception e) {
            throw CodefException.of(CodefError.INTERNAL_SERVER_ERROR, e);
        }
    }
}