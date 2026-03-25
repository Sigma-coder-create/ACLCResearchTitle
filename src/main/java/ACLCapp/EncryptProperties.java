package ACLCapp;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EncryptProperties {
    public static void main(String[] args) throws Exception {
        // Path to the original properties file
        String plainPath = "src/main/resources/db.properties";
        // Path for the encrypted output
        String encryptedPath = "src/main/resources/db.properties.enc";

        // Read the original file
        byte[] data = Files.readAllBytes(Paths.get(plainPath));

        // AES key (16 bytes)
        byte[] keyBytes = "16ByteSecretKey!".getBytes(); 
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        // Encrypt the file
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data);

        // Write the encrypted file
        Files.write(Paths.get(encryptedPath), encrypted);
        System.out.println("Encrypted file created: " + encryptedPath);

        // Delete the original plain file
        Files.delete(Paths.get(plainPath));
        System.out.println("Original file deleted: " + plainPath);
    }
}