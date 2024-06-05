import java.io.*;
import java.sql.*;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.*;

public class MessageEncodingDecoding {
    public static void main(String[] args) throws Exception {
        Scanner s = new Scanner(System.in);
        System.out.println("Enter a message:");
        String originalData = s.nextLine();
        SecretKey key = generateOrRetrieveAESKey(); // Generate or retrieve the key securely
        byte[] encryptedData = null;
        byte[] decryptedData = null;

        try {
            // Encrypt the data
            Cipher cip = Cipher.getInstance("AES");
            cip.init(Cipher.ENCRYPT_MODE, key);
            encryptedData = cip.doFinal(originalData.getBytes());

            // Decrypt the data
            cip.init(Cipher.DECRYPT_MODE, key);
            decryptedData = cip.doFinal(encryptedData);
        } catch (Exception e) {
            System.out.println("Error in encrypting or decrypting the data: " + e.getMessage());
        }

        // Convert the encrypted and decrypted data to Base64 for printing
        String encryptedDataStr = Base64.getEncoder().encodeToString(encryptedData);
        String decryptedDataStr = new String(decryptedData);

        // JDBC Connection Information

        try {
            // Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

	    // Open a connection
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mini?allowPublicKeyRetrieval=true&characterEncoding=utf8&useSSL=false&useUnicode=true", "root", "StJo2912#_");
	    Statement stat = conn.createStatement();
	    String createTable="CREATE TABLE IF NOT EXISTS message(No INT AUTO_INCREMENT PRIMARY KEY,encrypted_data TEXT,decrypted_data TEXT)";
	    stat.executeUpdate(createTable);
	    stat.close();
            // Insert the encrypted data into the database
            String insertSql = "INSERT INTO message (encrypted_data,decrypted_data) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = conn.prepareStatement(insertSql)) {
                preparedStatement.setString(1, encryptedDataStr);
                preparedStatement.setString(2, decryptedDataStr);
                try {
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();  // or use a logging framework to log the exception
                    System.out.println("Error executing SQL query: " + e.getMessage());
                }
	    conn.close();
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error in database operations: " + e.getMessage());
        }
    }

    private static SecretKey generateOrRetrieveAESKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        char[] keystorePassword = "keystore_password".toCharArray();
        FileInputStream fis = null;

        try {
            File keystoreFile = new File("keystore.jceks"); // Create a File object

            if (keystoreFile.exists()) {
                fis = new FileInputStream(keystoreFile);
                keyStore.load(fis, keystorePassword);
            } else {
                keyStore.load(null, keystorePassword);
                try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                    keyStore.store(fos, keystorePassword);
                }
            }

            String keyAlias = "aes_key";
            SecretKey key;
            if (keyStore.containsAlias(keyAlias)) {
                Key keyFromKeystore = keyStore.getKey(keyAlias, keystorePassword);
                if (keyFromKeystore instanceof SecretKey)
                    key = (SecretKey) keyFromKeystore;
                else
                    throw new RuntimeException("Key in the keystore is not a SecretKey");
            } else {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256, new SecureRandom());
                key = kg.generateKey();
                keyStore.setKeyEntry(keyAlias, key, keystorePassword, null);
                try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                    keyStore.store(fos, keystorePassword);
                }
            }
            return key;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
