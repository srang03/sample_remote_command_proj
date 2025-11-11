package kr.urock.sample_remote_command_proj.infrastructure.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 패스워드 암호화/복호화 유틸리티
 *
 * Jasypt를 사용한 AES-256 암호화
 * - 외부에서 암호화 키를 주입받아 사용 (환경변수)
 * - 암호화된 패스워드를 DB에 저장
 */
@Component
public class PasswordEncryptor {

    private final StandardPBEStringEncryptor encryptor;

    public PasswordEncryptor(@Value("${app.encryption.secret-key}") String secretKey) {
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword(secretKey);
        this.encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        this.encryptor.setIvGenerator(new RandomIvGenerator());
    }

    /**
     * 평문 패스워드 암호화
     *
     * @param plainPassword 평문 패스워드
     * @return 암호화된 패스워드
     */
    public String encrypt(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return encryptor.encrypt(plainPassword);
    }

    /**
     * 암호화된 패스워드 복호화
     *
     * @param encryptedPassword 암호화된 패스워드
     * @return 평문 패스워드
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("Encrypted password cannot be null or empty");
        }
        return encryptor.decrypt(encryptedPassword);
    }
}
