package io.codef.api.constants;

public enum CodefReferenceUrl {
    KEY("https://codef.io/account/keys"),
    DEV_GUIDE_REST_API("https://developer.codef.io/common-guide/rest-api"),
    GITHUB("https://github.com/codef-io/easycodef-java-v2"),
    PRODUCT("https://developer.codef.io/product/api"),
    TECH_INQUIRY("https://codef.io/cs/inquiry");

    private final String url;

    CodefReferenceUrl(String url) {
        this.url = url;
    }

    private static final String MESSAGE_FORMAT = "→ For detailed information, please visit '%s'";

    public String getUrl() {
        return String.format(MESSAGE_FORMAT, url);
    }
}
