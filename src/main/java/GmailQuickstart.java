// [START gmail_quickstart]
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GmailQuickstart {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String user = "me";

        // Print the labels in the user's account.
        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();
        if (labels.isEmpty()) {
            System.out.println("No labels found.");
        } else {
            System.out.println("Labels:");
            for (Label label : labels) {
                System.out.printf("- %s\n", label.getName());
                String PATH = "C:\\Users\\miti4\\projects\\qs\\files\\";
                String directoryName = PATH.concat(label.getName());
                Files.createDirectories(Paths.get(directoryName));
            }
        }

        ListMessagesResponse messagesResponse = service.users().messages().list(user).execute();
        List<Message> messages = new ArrayList<Message>();
        messages.addAll(messagesResponse.getMessages());
        if (messages.isEmpty()) {
            System.out.println("No messages found.");
        } else {
            System.out.println("messages:");
            for (Message message : messages) {
                // Print the messages id in the user's account.
                String messageId = message.getId();
                System.out.printf("ID: %s\n", messageId);
                Message m = service.users().messages().get(user, messageId).execute();
                MessagePart msgpart = m.getPayload();
                // Print the message header
                List<MessagePartHeader> headers = msgpart.getHeaders();
                for (MessagePartHeader header : headers) {
                    if (header.getName().toString().equals("Subject")) {
                        System.out.println("Header: " + header.getValue().toString());
                    }
                }
                // Print the message attachment id
                List<MessagePart> parts = msgpart.getParts();
                String attId = null;
                for (MessagePart part : parts) {
                    if (part.getFilename() != null && part.getFilename().length() > 0) {
                        attId = part.getBody().getAttachmentId();
                        String filename = part.getFilename();
                        System.out.println("Attachment: " + filename);
                        MessagePartBody attachPart;
                        FileOutputStream fileOutFile = null;
                        try {
                            //Go get the attachment part and get the bytes
                            attachPart = service.users().messages().attachments().get(user, part.getPartId(), attId).execute();
                            byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
                            //Write the attachment to new file
                            File toFile = new File("C:\\Users\\miti4\\projects\\qs\\files");
                            fileOutFile = new FileOutputStream(toFile + "\\" + filename);
                            fileOutFile.write(fileByteArray);
                            fileOutFile.close();
                            // size of a file (in bytes)
                            System.out.println(String.format("%,d bytes", fileByteArray.length));
                            System.out.println(String.format("%,d kilobytes", fileByteArray.length / 1024));
                            System.out.println("download finished");
                            service.users().messages().trash(user, messageId).execute();
                            System.out.println("message deleted");
                        } catch (IOException e) {
                            System.out.println("Exception downloading attachment: " + filename);
                        } finally {
                            if (fileOutFile != null) {
                                try {
                                    fileOutFile.close();
                                } catch (IOException e) {
                                    // probably doesn't matter
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// [END gmail_quickstart]
