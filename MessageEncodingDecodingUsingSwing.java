import java.io.*;
import java.sql.*;
import javax.crypto.*;
import java.security.*;
import java.util.Base64;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MessageEncodingDecodingUsingSwing {
    public static void main(String[] args) throws Exception, SQLException 
    {
        SwingUtilities.invokeLater(() -> {
            InputOutput obj = new InputOutput();
            obj.setVisible(true);
        });
    }

    public static SecretKey getKey() {
        try {
            return generateOrRetrieveAESKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SecretKey generateOrRetrieveAESKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        char[] keystorePassword = "keystore_password".toCharArray();
        FileInputStream fis = null;

        try {
            File keystoreFile = new File("keystore.jceks");

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

class InputOutput extends JFrame {
    JLabel l1, l2;
    JTextField t1;
    JButton b1, b2 ,b3;
    Cipher cip;
    SecretKey key;
    //Open Connection
    Connection conn;
    byte[] encryptedData;
    String encryptedDataStr;
    String decryptedDataStr;

    public InputOutput() 
    {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);

        l1 = new JLabel("Enter your message");
        l1.setBounds(40, 10, 300, 30);
        add(l1);

        l2 = new JLabel("");
        l2.setBounds(60, 260, 300, 40);
        add(l2);

        t1 = new JTextField(200);
        b1 = new JButton("Encrypted Data");
        b2 = new JButton("Decrypted Data");
	b3 = new JButton("Insert data into Database");
        t1.setBounds(40, 60, 500, 40);
        b1.setBounds(60, 140, 150, 30);
        b2.setBounds(60, 180, 150, 30);
	b3.setBounds(60, 220, 200, 30);
        add(t1);
        add(b1);
        add(b2);
	add(b3);
        try
        {
	    // Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
	    // Giving a connection
	    try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sakila?allowPublicKeyRetrieval=true&characterEncoding=utf8&useSSL=false&useUnicode=true", "root", "StJo2912#_");
		Statement stat = conn.createStatement();
	    	String createTable="CREATE TABLE IF NOT EXISTS message(No INT AUTO_INCREMENT PRIMARY KEY,encrypted_data TEXT,decrypted_data TEXT)";
	    	stat.executeUpdate(createTable);
	    	stat.close();
            } catch (SQLException ae) {
                l2.setText("Error in database operations: " + ae.getMessage());
	    }
	    
	}catch(ClassNotFoundException ae){
	    l2.setText("Error in database operations: " + ae.getMessage());
	}
        b1.addActionListener(e -> {
            try 
	    {
                String originalData = t1.getText();
                key = MessageEncodingDecodingUsingSwing.getKey();

                cip = Cipher.getInstance("AES");
                cip.init(Cipher.ENCRYPT_MODE, key);
                encryptedData = cip.doFinal(originalData.getBytes());
                encryptedDataStr = Base64.getEncoder().encodeToString(encryptedData);
		//l2.setText("Data is Encrypted data");
            } catch (Exception ae) {
                l2.setText("Error in encrypting data");
            }
        });

        b2.addActionListener(e -> {
            try {
                cip.init(Cipher.DECRYPT_MODE, key);
                byte[] decryptedData = cip.doFinal(encryptedData);
                decryptedDataStr = new String(decryptedData);
		//l2.setText("Data is Decrypted data");
            } catch (Exception ae) {
                l2.setText("Error in decrypting data");
            }
        });
       
	b3.addActionListener(e -> {
	    //Load data into Database
	    String insertSql = "INSERT INTO message (encrypted_data,decrypted_data) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = conn.prepareStatement(insertSql)) 
	    {
            	preparedStatement.setString(1, encryptedDataStr);
	    	preparedStatement.setString(2, decryptedDataStr);
            	try 
	    	{
		    preparedStatement.executeUpdate();
                    //l2.setText("Data inserted into the database.");
            	} catch (SQLException ae) {
                    ae.printStackTrace();  // or use a logging framework to log the exception
                    l2.setText("Error executing SQL query: " + ae.getMessage());
            	}
             }catch(SQLException ae){
                 l2.setText("Error in database operations: " + ae.getMessage());
             }
	});
    }
}