package io.codef.api;

import io.codef.api.constants.CodefClientType;
import io.codef.api.constants.CodefResponseCode;
import io.codef.api.dto.CodefSimpleAuth;
import io.codef.api.dto.EasyCodefRequest;
import io.codef.api.dto.EasyCodefResponse;
import io.codef.api.error.CodefError;
import io.codef.api.error.CodefException;
import io.codef.api.util.RsaUtil;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Optional;

import static io.codef.api.dto.EasyCodefRequest.TRUE;

public class EasyCodef {
    private final HashMap<String, CodefSimpleAuth> simpleAuthRequestStorage = new HashMap<>();

    private final PublicKey publicKey;
    private final CodefClientType clientType;
    private final EasyCodefToken easyCodefToken;

    protected EasyCodef(EasyCodefBuilder builder, EasyCodefToken easyCodefToken) {
        this.publicKey = RsaUtil.generatePublicKey(builder.getPublicKey());
        this.clientType = builder.getClientType();
        this.easyCodefToken = easyCodefToken;
    }

    public EasyCodefResponse requestProduct(EasyCodefRequest request) throws CodefException {
        final String requestUrl = clientType.getHost() + request.path();
        final EasyCodefToken validToken = easyCodefToken.validateAndRefreshToken();

        final EasyCodefResponse easyCodefResponse = EasyCodefConnector.requestProduct(request, validToken, requestUrl);

        storeIfSimpleAuthResponseRequired(request, easyCodefResponse, requestUrl);
        return easyCodefResponse;
    }

    public EasyCodefResponse requestSimpleAuthCertification(String transactionId) throws CodefException {
        final CodefSimpleAuth codefSimpleAuth = simpleAuthRequestStorage.get(transactionId);
        CodefValidator.requireNonNullElseThrow(codefSimpleAuth, CodefError.SIMPLE_AUTH_FAILED);

        final String requestUrl = codefSimpleAuth.requestUrl();
        final EasyCodefRequest request = codefSimpleAuth.request();

        addTwoWayInfo(request, codefSimpleAuth);

        final EasyCodefToken validToken = easyCodefToken.validateAndRefreshToken();
        final EasyCodefResponse easyCodefResponse = EasyCodefConnector.requestProduct(request, validToken, requestUrl);

        updateSimpleAuthResponseRequired(requestUrl, request, easyCodefResponse, transactionId);
        return easyCodefResponse;
    }

    private void addTwoWayInfo(
            EasyCodefRequest request,
            CodefSimpleAuth codefSimpleAuth
    ) {
        request.requestBody().put(EasyCodefRequest.IS_TWO_WAY, true);
        request.requestBody().put(EasyCodefRequest.SIMPLE_AUTH, TRUE);
        request.requestBody().put(EasyCodefRequest.TWO_WAY_INFO, codefSimpleAuth.response().data());
    }

    private void storeIfSimpleAuthResponseRequired(EasyCodefRequest request, EasyCodefResponse easyCodefResponse, String requestUrl) {
        Optional.ofNullable(easyCodefResponse.code()).filter(code -> code.equals(CodefResponseCode.CF_03002)).ifPresent(code -> {
            CodefSimpleAuth codefSimpleAuth = new CodefSimpleAuth(requestUrl, request, easyCodefResponse);
            simpleAuthRequestStorage.put(easyCodefResponse.transactionId(), codefSimpleAuth);
        });
    }

    private void updateSimpleAuthResponseRequired(String path, EasyCodefRequest request, EasyCodefResponse easyCodefResponse, String transactionId) {
        Optional.ofNullable(easyCodefResponse.code())
                .filter(code -> code.equals(CodefResponseCode.CF_03002))
                .ifPresentOrElse(code -> {
                    CodefSimpleAuth newCodefSimpleAuth = new CodefSimpleAuth(path, request, easyCodefResponse);
                    simpleAuthRequestStorage.put(transactionId, newCodefSimpleAuth);
                }, () -> simpleAuthRequestStorage.remove(transactionId));
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}