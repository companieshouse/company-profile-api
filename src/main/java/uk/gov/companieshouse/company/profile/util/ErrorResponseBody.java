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
        // checked by @see https://jsonformatter.org/json-pretty-print
        String errorString = """
                {
                    "errors": [
                        {
                            "type": "%s",
                            "error": "%s"
                        }
                    ]
                }""";
        return String.format(errorString, type, error);
    }
}
