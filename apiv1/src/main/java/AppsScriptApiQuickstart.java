import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import com.google.api.services.script.model.*;
import com.google.api.services.script.Script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class AppsScriptApiQuickstart {
    /** Application name. */
    private static final String APPLICATION_NAME =
            "Apps Script API Java Quickstart";

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/script-api-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart. */
    private static final List<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/drive");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                AppsScriptApiQuickstart.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Script client service.
     * @param {Credential} credential an authorized Credential object.
     * @return an authorized Script client service
     */
    public static Script getScriptService(Credential credential) {
        return new Script.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) {
                        // This allows the API to call (and avoid
                        // timing out on) functions that take up to
                        // 6 minutes to complete (the maximum allowed
                        // script run time), plus a little overhead.
                        httpRequest.setReadTimeout(380000);
                    }
                })
                .build();
    }

    public static void main(String[] args) throws IOException {
        // ID of the script to call. Acquire this from the Apps Script editor,
        // under Publish > Publish as execution endpoint.
        String scriptId = "MsawqlhMuXdiVoigEUkh9guNZK2JnVIY8";

        // Apps Script function to call. This one takes no parameters and
        // returns a set of folder names (keyed on folder ID) that are
        // present in the user's root Drive folder.
        String functionName = "getFoldersUnderRoot";

        // Generate OAuth token and service object.
        Credential credential = authorize();
        String token = credential.getAccessToken();
        Script service = getScriptService(credential);

        // Create execution request.
        ExecutionRequest request = new ExecutionRequest()
                .setFunction(functionName)
                .setOauthToken(token);

        try {
            // Make the request, and wait for result.
            Operation op =
                    service.scripts().exec(scriptId, request).execute();

            // Print results of request.
            if (op.getDone()) {
                if (op.getError() != null) {
                    System.out.printf("Encountered Error %d: %s\n",
                            op.getError().getCode(), op.getError().getMessage());
                } else {
                    java.util.Map<String, Object> response = op.getResponse();
                    if (response.get("result") != null) {
                        System.out.println("List of child folders under your root folder:");
                        java.util.Map<String, String> responseMap =
                                (java.util.Map<String, String>)response.get("result");
                        for (String id: responseMap.keySet()) {
                            System.out.printf("\t%s (%s)\n", responseMap.get(id), id);
                        }
                    } else {
                        System.out.println("\tFunction had no errors, but returned no response!");
                    }
                }
            } else {
                System.out.println("Operation was not completed.");
            }
        } catch (GoogleJsonResponseException e) {
            System.out.println("Error returned: " + e.getDetails().getCode());
            System.out.println("Error Stacktrace:");
            e.printStackTrace(System.out);
        }
    }

}