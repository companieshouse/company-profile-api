package uk.gov.companieshouse.company.profile.util;

public class ErrorResponseBody {
    private final String type;
    private final String error;

    public ErrorResponseBody(String type, String error) {
        this.type = type;
        this.error = error;
    }

    @Override
    public String toString() {
        String errorString =
                  "{\n"
                + "    \"errors\": [\n"
                + "        {\n"
                + "            \"type\": \"%s\",\n"
                + "            \"error\": \"%s\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        return String.format(errorString, type, error);
    }
}
